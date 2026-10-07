package io.d4tzz.ecommerce.catalog.infrastructure.query.product;

import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.catalog.query.product.ProductCardView;
import io.d4tzz.ecommerce.catalog.query.product.ProductSearchCriteria;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProductSearchQuery {

    private final JdbcClient jdbcClient;
    private final CategoryPathResolver pathResolver;
    private final ObjectMapper objectMapper;
    private final RowMapper<ProductCardView> productCardMapper;

    public ProductSearchQuery(
        JdbcClient jdbcClient,
        CategoryPathResolver pathResolver,
        ObjectMapper objectMapper,
        MediaUrlResolver urlResolver
    ) {
        this.jdbcClient = jdbcClient;
        this.pathResolver = pathResolver;
        this.objectMapper = objectMapper;
        this.productCardMapper = (rs, rowNum) ->
            new ProductCardView(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                urlResolver.resolveUrl(rs.getString("main_image_url")),
                rs.getBigDecimal("min_price"),
                rs.getBigDecimal("max_price"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("category_slug")
            );
    }

    public PageResponse<ProductCardView> execute(ProductSearchCriteria criteria) {
        ProductSearchCriteria safeCriteria = criteria != null ? criteria : ProductSearchCriteria.empty();

        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        applyFilters(where, params, safeCriteria);

        String whereClause = where.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", where);

        String countSql =
            "SELECT COUNT(*) FROM products p\n" + "JOIN categories c ON p.category_id = c.id\n" + whereClause;

        var countSpec = jdbcClient.sql(countSql);
        params.forEach(countSpec::param);
        Long total = countSpec.query(Long.class).single();
        long totalElements = total != null ? total : 0L;

        if (totalElements == 0) {
            return PageResponse.empty(safeCriteria.getPage(), safeCriteria.getSize());
        }

        boolean hasKeyword = safeCriteria.keyword() != null && !safeCriteria.keyword().isBlank();
        String orderBy = resolveSortOrder(safeCriteria.sortBy(), hasKeyword);

        String dataSql =
            """
            SELECT
                p.id,
                p.name,
                p.slug,
                (
                    SELECT pm.media_id
                    FROM product_media pm
                    WHERE pm.product_id = p.id
                    ORDER BY pm.position ASC LIMIT 1
                ) AS main_image_url,
                p.min_price,
                p.max_price,
                p.category_id,
                c.name AS category_name,
                c.slug AS category_slug
            FROM products p
            JOIN categories c ON p.category_id = c.id
            """ +
            whereClause +
            "\nORDER BY " +
            orderBy +
            "\nLIMIT :limit OFFSET :offset";

        var dataSpec = jdbcClient.sql(dataSql);
        params.forEach(dataSpec::param);
        dataSpec.param("limit", safeCriteria.getSize());
        dataSpec.param("offset", safeCriteria.getOffset());

        List<ProductCardView> content = dataSpec.query(productCardMapper).list();

        return PageResponse.of(content, safeCriteria.getPage(), safeCriteria.getSize(), totalElements);
    }

    private void applyFilters(List<String> where, Map<String, Object> params, ProductSearchCriteria criteria) {
        where.add("p.status = 'PUBLISHED'");

        boolean hasKeyword = criteria.keyword() != null && !criteria.keyword().isBlank();
        if (hasKeyword) {
            where.add(
                """
                (to_tsvector('simple', immutable_unaccent(p.name)) @@ websearch_to_tsquery('simple', immutable_unaccent(:keyword))
                 OR immutable_unaccent(p.name) % immutable_unaccent(:keyword)
                 OR immutable_unaccent(:keyword) <% immutable_unaccent(p.name))
                """
            );
            params.put("keyword", criteria.keyword().trim());
        }

        if (criteria.categoryId() != null) {
            String pathPrefix = pathResolver.getCategoryPathPrefix(criteria.categoryId());
            if (pathPrefix != null) {
                where.add("(c.id = :categoryId OR c.path LIKE :pathPrefix)");
                params.put("categoryId", criteria.categoryId());
                params.put("pathPrefix", pathPrefix);
            } else {
                where.add("c.id = :categoryId");
                params.put("categoryId", criteria.categoryId());
            }
        }

        if (criteria.minPrice() != null) {
            where.add("p.max_price >= :minPrice");
            params.put("minPrice", criteria.minPrice());
        }

        if (criteria.maxPrice() != null) {
            where.add("p.min_price <= :maxPrice");
            params.put("maxPrice", criteria.maxPrice());
        }

        if (criteria.optionValueIds() != null && !criteria.optionValueIds().isEmpty()) {
            where.add(
                """
                    EXISTS (
                        SELECT 1 FROM product_option_values pov
                        WHERE pov.product_id = p.id
                          AND pov.value_id IN (:optionValueIds)
                        GROUP BY pov.product_id
                        HAVING COUNT(DISTINCT pov.option_id) = (
                            SELECT COUNT(DISTINCT option_id) FROM option_values WHERE id IN (:optionValueIds)
                        )
                    )
                """
            );
            params.put("optionValueIds", criteria.optionValueIds());
        }

        if (criteria.attributes() != null && !criteria.attributes().isEmpty()) {
            int attrIdx = 0;
            for (Map.Entry<String, List<String>> entry : criteria.attributes().entrySet()) {
                List<String> values = entry.getValue();
                if (values == null || values.isEmpty()) {
                    continue;
                }
                List<String> orClauses = new ArrayList<>();
                int valIdx = 0;
                for (String val : values) {
                    String paramName = "attr_" + attrIdx + "_" + valIdx;
                    orClauses.add("p.attributes @> CAST(:%s AS jsonb)".formatted(paramName));
                    params.put(paramName, toJson(Map.of(entry.getKey(), List.of(val))));
                    valIdx++;
                }
                if (!orClauses.isEmpty()) {
                    where.add("(" + String.join(" OR ", orClauses) + ")");
                }
                attrIdx++;
            }
        }
    }

    private String resolveSortOrder(String sortBy, boolean hasKeyword) {
        String keywordSort = """
        (COALESCE(ts_rank(to_tsvector('simple', immutable_unaccent(p.name)), websearch_to_tsquery('simple', immutable_unaccent(:keyword))), 0.0) * 2.0
         + GREATEST(similarity(immutable_unaccent(p.name), immutable_unaccent(:keyword)), word_similarity(immutable_unaccent(:keyword), immutable_unaccent(p.name)))) DESC, p.id DESC
        """;
        if (sortBy == null || sortBy.isBlank()) {
            return hasKeyword ? keywordSort : "p.id DESC";
        }
        return switch (sortBy.toLowerCase()) {
            case "price_asc" -> "p.min_price ASC, p.id DESC";
            case "price_desc" -> "p.max_price DESC, p.id DESC";
            case "name_asc" -> "p.name ASC, p.id DESC";
            default -> hasKeyword ? keywordSort : "p.id DESC";
        };
    }

    private String toJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON", e);
        }
    }
}
