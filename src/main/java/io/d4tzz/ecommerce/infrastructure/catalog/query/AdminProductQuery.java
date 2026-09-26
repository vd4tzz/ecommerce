package io.d4tzz.ecommerce.infrastructure.catalog.query;

import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.query.catalog.AdminProductSearchCriteria;
import io.d4tzz.ecommerce.query.catalog.AdminProductTableRowView;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class AdminProductQuery {

    private final JdbcClient jdbcClient;
    private final CategoryPathResolver pathResolver;
    // private final MediaUrlResolver urlResolver;
    private final RowMapper<AdminProductTableRowView> adminProductRowMapper;

    public AdminProductQuery(JdbcClient jdbcClient, CategoryPathResolver pathResolver, MediaUrlResolver urlResolver) {
        this.jdbcClient = jdbcClient;
        this.pathResolver = pathResolver;
        // this.urlResolver = urlResolver;
        this.adminProductRowMapper = (rs, rowNum) ->
            new AdminProductTableRowView(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                urlResolver.resolveUrl(rs.getString("main_image_url")),
                rs.getObject("category_id", Long.class),
                rs.getString("category_name"),
                rs.getString("status"),
                rs.getBigDecimal("min_price"),
                rs.getBigDecimal("max_price"),
                rs.getInt("variant_count"),
                rs.getInt("media_count")
            );
    }

    public PageResponse<AdminProductTableRowView> getAdminProducts(AdminProductSearchCriteria criteria) {
        AdminProductSearchCriteria safeCriteria = criteria != null ? criteria : AdminProductSearchCriteria.empty();

        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        applyAdminFilters(where, params, safeCriteria);

        String whereClause = where.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", where);

        String countSql =
            "SELECT COUNT(*) FROM products p\n" + "LEFT JOIN categories c ON p.category_id = c.id\n" + whereClause;

        var countSpec = jdbcClient.sql(countSql);
        params.forEach(countSpec::param);
        Long total = countSpec.query(Long.class).single();
        long totalElements = total != null ? total : 0L;

        if (totalElements == 0) {
            return PageResponse.empty(safeCriteria.getPage(), safeCriteria.getSize());
        }

        String orderBy = resolveAdminSortOrder(safeCriteria.sortBy());

        String dataSql =
            """
            SELECT
                p.id,
                p.name,
                p.slug,
                (SELECT pm.media_id FROM product_media pm WHERE pm.product_id = p.id ORDER BY pm.position ASC LIMIT 1) AS main_image_url,
                p.category_id,
                c.name AS category_name,
                p.status,
                p.min_price,
                p.max_price,
                (SELECT COUNT(*) FROM variants v WHERE v.product_id = p.id) AS variant_count,
                (SELECT COUNT(*) FROM product_media pm WHERE pm.product_id = p.id) AS media_count
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            """ +
            whereClause +
            "\nORDER BY " +
            orderBy +
            "\nLIMIT :limit OFFSET :offset";

        var dataSpec = jdbcClient.sql(dataSql);
        params.forEach(dataSpec::param);
        dataSpec.param("limit", safeCriteria.getSize());
        dataSpec.param("offset", safeCriteria.getOffset());

        List<AdminProductTableRowView> content = dataSpec.query(adminProductRowMapper).list();

        return PageResponse.of(content, safeCriteria.getPage(), safeCriteria.getSize(), totalElements);
    }

    public Map<String, Long> getProductStatusCounts() {
        String sql = """
            SELECT p.status, COUNT(*) AS count
            FROM products p
            GROUP BY p.status
        """;

        Map<String, Long> result = new LinkedHashMap<>();
        jdbcClient
            .sql(sql)
            .query((rs, rowNum) -> Map.entry(rs.getString("status"), rs.getLong("count")))
            .list()
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));

        return result;
    }

    private void applyAdminFilters(
        List<String> where,
        Map<String, Object> params,
        AdminProductSearchCriteria criteria
    ) {
        if (criteria.status() != null && !criteria.status().isBlank()) {
            where.add("p.status = :status");
            params.put("status", criteria.status().trim().toUpperCase());
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

        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            String keyword = criteria.keyword().trim();
            boolean isNumeric = keyword.chars().allMatch(Character::isDigit);
            if (isNumeric) {
                where.add(
                    """
                        (
                            p.id = :exactId
                            OR immutable_unaccent(p.name) ILIKE immutable_unaccent(:kwLike)
                            OR EXISTS (
                                SELECT 1 FROM variants v
                                WHERE v.product_id = p.id
                                  AND v.sku ILIKE :kwLike
                            )
                        )
                    """
                );
                params.put("exactId", Long.parseLong(keyword));
                params.put("kwLike", "%" + keyword + "%");
            } else {
                where.add(
                    """
                        (
                            immutable_unaccent(p.name) ILIKE immutable_unaccent(:kwLike)
                            OR EXISTS (
                                SELECT 1 FROM variants v
                                WHERE v.product_id = p.id
                                  AND v.sku ILIKE :kwLike
                            )
                        )
                    """
                );
                params.put("kwLike", "%" + keyword + "%");
            }
        }
    }

    private String resolveAdminSortOrder(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "p.id DESC";
        }
        return switch (sortBy.toLowerCase()) {
            case "id_asc" -> "p.id ASC";
            case "price_asc" -> "p.min_price ASC, p.id DESC";
            case "price_desc" -> "p.max_price DESC, p.id DESC";
            case "name_asc" -> "p.name ASC, p.id DESC";
            case "name_desc" -> "p.name DESC, p.id DESC";
            default -> "p.id DESC";
        };
    }
}
