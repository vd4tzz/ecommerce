package io.d4tzz.ecommerce.infrastructure.catalog.query;

import io.d4tzz.ecommerce.query.catalog.AttributeFacet;
import io.d4tzz.ecommerce.query.catalog.CategoryFilterFacetView;
import io.d4tzz.ecommerce.query.catalog.FilterSidebarView;
import io.d4tzz.ecommerce.query.catalog.OptionFacetDto;
import io.d4tzz.ecommerce.query.catalog.PriceRange;
import io.d4tzz.ecommerce.query.catalog.ProductSearchCriteria;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProductFacetQuery {

    private final JdbcClient jdbcClient;
    private final CategoryPathResolver pathResolver;
    private final ObjectMapper objectMapper;

    public ProductFacetQuery(JdbcClient jdbcClient, CategoryPathResolver pathResolver, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.pathResolver = pathResolver;
        this.objectMapper = objectMapper;
    }

    public FilterSidebarView execute(ProductSearchCriteria criteria) {
        ProductSearchCriteria safeCriteria = criteria != null ? criteria : ProductSearchCriteria.empty();

        boolean isGlobal = safeCriteria.categoryId() == null;
        boolean shouldLoadFacets = !isGlobal || safeCriteria.isFullFacets();

        String pathPrefix = !isGlobal ? pathResolver.getCategoryPathPrefix(safeCriteria.categoryId()) : null;

        PriceRange priceRange = getPriceRange(safeCriteria, pathPrefix);

        List<CategoryFilterFacetView> categories = getCategoryFacets(safeCriteria);

        Map<String, List<OptionFacetDto>> options = shouldLoadFacets
            ? getOptionFacets(safeCriteria, pathPrefix)
            : Collections.emptyMap();

        Map<String, List<AttributeFacet>> attributes = shouldLoadFacets
            ? getAttributeFacets(safeCriteria, pathPrefix)
            : Collections.emptyMap();

        return new FilterSidebarView(priceRange, categories, options, attributes);
    }

    private List<CategoryFilterFacetView> getCategoryFacets(ProductSearchCriteria criteria) {
        List<String> productConditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        applyFilters(productConditions, params, criteria, null, false, true, true, true);

        String sql;
        if (criteria.categoryId() == null) {
            sql =
                """
                SELECT
                    c.id,
                    c.name,
                    c.slug,
                    COUNT(DISTINCT p.id) AS product_count
                FROM products p
                JOIN categories c ON p.category_id = c.id
                WHERE """ +
                String.join("\n  AND ", productConditions) +
                """
                    GROUP BY c.id, c.name, c.slug
                    ORDER BY product_count DESC, c.name ASC
                """;
        } else {
            params.put("parentCategoryId", criteria.categoryId());
            sql =
                """
                SELECT
                    c.id,
                    c.name,
                    c.slug,
                    COUNT(DISTINCT p.id) AS product_count
                FROM categories c
                LEFT JOIN products p ON p.category_id = c.id
                    AND """ +
                String.join("\n  AND ", productConditions) +
                """
                    WHERE c.parent_id = :parentCategoryId
                    GROUP BY c.id, c.name, c.slug
                    ORDER BY product_count DESC, c.name ASC
                """;
        }

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);

        return spec
            .query((rs, rowNum) ->
                new CategoryFilterFacetView(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("slug"),
                    rs.getLong("product_count")
                )
            )
            .list();
    }

    private PriceRange getPriceRange(ProductSearchCriteria criteria, String pathPrefix) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        // includeCategory = true, includePrice = false (giữ slider giá độc lập), includeOptions = true, includeAttributes = true
        applyFilters(where, params, criteria, pathPrefix, true, false, true, true);

        String sql =
            """
            SELECT
                COALESCE(MIN(p.min_price), 0) AS min_price,
                COALESCE(MAX(p.max_price), 0) AS max_price
            FROM products p
            JOIN categories c ON p.category_id = c.id
            WHERE """ + String.join("\n  AND ", where);

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);

        return spec
            .query((rs, rowNum) -> new PriceRange(rs.getBigDecimal("min_price"), rs.getBigDecimal("max_price")))
            .optional()
            .orElse(new PriceRange(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private Map<String, List<OptionFacetDto>> getOptionFacets(ProductSearchCriteria criteria, String pathPrefix) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        // includeCategory = true, includePrice = true, includeOptions = false (Disjunctive: cho phép chọn các màu/size khác), includeAttributes = true
        applyFilters(where, params, criteria, pathPrefix, true, true, false, true);

        String sql =
            """
            WITH facet_counts AS (
                SELECT
                    pov.option_id,
                    pov.value_id,
                    COUNT(DISTINCT p.id) AS product_count
                FROM product_option_values pov
                JOIN products p ON pov.product_id = p.id
                JOIN categories c ON p.category_id = c.id
                WHERE """ +
            String.join("\n  AND ", where) +
            """
                    GROUP BY pov.option_id, pov.value_id
                )
                SELECT
                    fc.option_id,
                    fc.value_id,
                    o.name AS option_name,
                    ov.name AS value_name,
                    fc.product_count
                FROM facet_counts fc
                JOIN options o ON fc.option_id = o.id
                JOIN option_values ov ON fc.value_id = ov.id
                ORDER BY o.name ASC, fc.product_count DESC, ov.name ASC
            """;

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);

        List<OptionFacetDto> flatFacets = spec
            .query((rs, rowNum) ->
                new OptionFacetDto(
                    rs.getLong("option_id"),
                    rs.getLong("value_id"),
                    rs.getString("option_name"),
                    rs.getString("value_name"),
                    rs.getLong("product_count")
                )
            )
            .list();

        return flatFacets
            .stream()
            .collect(Collectors.groupingBy(OptionFacetDto::optionName, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<AttributeFacet>> getAttributeFacets(ProductSearchCriteria criteria, String pathPrefix) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        // includeCategory = true, includePrice = true, includeOptions = true, includeAttributes = false (Disjunctive: cho phép chọn các brand khác)
        applyFilters(where, params, criteria, pathPrefix, true, true, true, false);

        where.add("p.attributes != '{}'::jsonb");

        String sql =
            """
            WITH active_products AS (
                SELECT p.id, p.attributes
                FROM products p
                JOIN categories c ON p.category_id = c.id
                WHERE """ +
            String.join("\n  AND ", where) +
            """
                )
                SELECT
                    kv.key AS attr_code,
                    kv.value AS val_code,
                    COUNT(DISTINCT ap.id) AS product_count
                FROM active_products ap
                CROSS JOIN LATERAL jsonb_each_text(ap.attributes) AS kv
                GROUP BY kv.key, kv.value
                ORDER BY kv.key ASC, product_count DESC, kv.value ASC
            """;

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);

        List<AttributeFacet> flatFacets = spec
            .query((rs, rowNum) ->
                new AttributeFacet(rs.getString("attr_code"), rs.getString("val_code"), rs.getLong("product_count"))
            )
            .list();

        return flatFacets
            .stream()
            .collect(Collectors.groupingBy(AttributeFacet::attrCode, LinkedHashMap::new, Collectors.toList()));
    }

    private void applyFilters(
        List<String> where,
        Map<String, Object> params,
        ProductSearchCriteria criteria,
        String pathPrefix,
        boolean includeCategory,
        boolean includePrice,
        boolean includeOptions,
        boolean includeAttributes
    ) {
        where.add("p.status = 'ACTIVE'");

        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            where.add(
                "to_tsvector('simple', immutable_unaccent(p.name)) @@ websearch_to_tsquery('simple', immutable_unaccent(:keyword))"
            );
            params.put("keyword", criteria.keyword().trim());
        }

        if (includeCategory && criteria.categoryId() != null) {
            if (pathPrefix != null) {
                where.add("(c.id = :categoryId OR c.path LIKE :pathPrefix)");
                params.put("pathPrefix", pathPrefix);
            } else {
                where.add("c.id = :categoryId");
            }
            params.put("categoryId", criteria.categoryId());
        }

        if (includePrice) {
            if (criteria.minPrice() != null) {
                where.add("p.max_price >= :minPrice");
                params.put("minPrice", criteria.minPrice());
            }
            if (criteria.maxPrice() != null) {
                where.add("p.min_price <= :maxPrice");
                params.put("maxPrice", criteria.maxPrice());
            }
        }

        if (includeOptions && criteria.optionValueIds() != null && !criteria.optionValueIds().isEmpty()) {
            where.add(
                """
                    EXISTS (
                        SELECT 1 FROM product_option_values pov_filter
                        WHERE pov_filter.product_id = p.id
                          AND pov_filter.value_id IN (:filterOptionValueIds)
                    )
                """
            );
            params.put("filterOptionValueIds", criteria.optionValueIds());
        }

        if (includeAttributes && criteria.attributes() != null && !criteria.attributes().isEmpty()) {
            where.add("p.attributes @> CAST(:filterAttr AS jsonb)");
            params.put("filterAttr", toJson(criteria.attributes()));
        }
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
