package io.d4tzz.ecommerce.infrastructure.catalog.query;

import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.query.catalog.ProductCardView;
import io.d4tzz.ecommerce.query.catalog.ProductSearchCriteria;
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
    private final MediaUrlResolver urlResolver;
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
        this.urlResolver = urlResolver;
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

        // 1. Chạy câu lệnh COUNT (không có ORDER BY, không chạy subquery thừa)
        String countSql =
            "SELECT COUNT(*) FROM products p\n" + "JOIN categories c ON p.category_id = c.id\n" + whereClause;

        var countSpec = jdbcClient.sql(countSql);
        params.forEach(countSpec::param);
        Long total = countSpec.query(Long.class).single();
        long totalElements = total != null ? total : 0L;

        if (totalElements == 0) {
            return PageResponse.empty(safeCriteria.getPage(), safeCriteria.getSize());
        }

        // 2. Chạy câu lệnh SELECT DATA với LIMIT / OFFSET
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
        where.add("p.status = 'ACTIVE'");

        boolean hasKeyword = criteria.keyword() != null && !criteria.keyword().isBlank();
        if (hasKeyword) {
            where.add(
                "to_tsvector('simple', immutable_unaccent(p.name)) @@ websearch_to_tsquery('simple', immutable_unaccent(:keyword))"
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
                    )
                """
            );
            params.put("optionValueIds", criteria.optionValueIds());
        }

        if (criteria.attributes() != null && !criteria.attributes().isEmpty()) {
            where.add("p.attributes @> CAST(:attrFilter AS jsonb)");
            params.put("attrFilter", toJson(criteria.attributes()));
        }
    }

    private String resolveSortOrder(String sortBy, boolean hasKeyword) {
        if (sortBy == null || sortBy.isBlank()) {
            return hasKeyword
                ? "ts_rank(to_tsvector('simple', immutable_unaccent(p.name)), websearch_to_tsquery('simple', immutable_unaccent(:keyword))) DESC, p.id DESC"
                : "p.id DESC";
        }
        return switch (sortBy.toLowerCase()) {
            case "price_asc" -> "p.min_price ASC, p.id DESC";
            case "price_desc" -> "p.max_price DESC, p.id DESC";
            case "name_asc" -> "p.name ASC, p.id DESC";
            default -> hasKeyword
                ? "ts_rank(to_tsvector('simple', immutable_unaccent(p.name)), websearch_to_tsquery('simple', immutable_unaccent(:keyword))) DESC, p.id DESC"
                : "p.id DESC";
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
