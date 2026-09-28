package io.d4tzz.ecommerce.infrastructure.catalog.query.product;

import io.d4tzz.ecommerce.query.catalog.product.AttributeFilterView;
import io.d4tzz.ecommerce.query.catalog.product.CategoryFilterView;
import io.d4tzz.ecommerce.query.catalog.product.FilterSidebarView;
import io.d4tzz.ecommerce.query.catalog.product.OptionFilterView;
import io.d4tzz.ecommerce.query.catalog.product.PriceRange;
import io.d4tzz.ecommerce.query.catalog.product.ProductSearchCriteria;
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

    public ProductFacetQuery(JdbcClient jdbcClient, CategoryPathResolver pathResolver, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.pathResolver = pathResolver;
    }

    public FilterSidebarView execute(ProductSearchCriteria criteria) {
        ProductSearchCriteria safeCriteria = criteria != null ? criteria : ProductSearchCriteria.empty();

        boolean isGlobal = safeCriteria.categoryId() == null;
        boolean hasKeyword = safeCriteria.keyword() != null && !safeCriteria.keyword().isBlank();
        boolean shouldLoadFacets = !isGlobal || hasKeyword || safeCriteria.isFullFacets();

        String pathPrefix = !isGlobal ? pathResolver.getCategoryPathPrefix(safeCriteria.categoryId()) : null;

        PriceRange priceRange = getPriceRange(safeCriteria, pathPrefix);
        List<CategoryFilterView> categories = getCategoryFacets(safeCriteria);

        Map<String, List<OptionFilterView>> options = shouldLoadFacets
            ? getOptionFacets(safeCriteria, pathPrefix)
            : Collections.emptyMap();

        Map<String, List<AttributeFilterView>> attributes = shouldLoadFacets
            ? getAttributeFacets(safeCriteria, pathPrefix)
            : Collections.emptyMap();

        return new FilterSidebarView(priceRange, categories, options, attributes);
    }

    private List<CategoryFilterView> getCategoryFacets(ProductSearchCriteria criteria) {
        if (criteria.categoryId() == null) {
            String sql = """
            SELECT
                root.id,
                root.name,
                root.slug
            FROM categories root
            WHERE root.parent_id IS NULL
            ORDER BY root.name ASC
            """;

            return jdbcClient
                .sql(sql)
                .query((rs, rowNum) ->
                    new CategoryFilterView(rs.getLong("id"), rs.getString("name"), rs.getString("slug"))
                )
                .list();
        } else {
            String sql = """
            SELECT
                sub.id,
                sub.name,
                sub.slug
            FROM categories sub
            WHERE sub.parent_id = :parentCategoryId
            ORDER BY sub.name ASC
            """;

            return jdbcClient
                .sql(sql)
                .param("parentCategoryId", criteria.categoryId())
                .query((rs, rowNum) ->
                    new CategoryFilterView(rs.getLong("id"), rs.getString("name"), rs.getString("slug"))
                )
                .list();
        }
    }

    private PriceRange getPriceRange(ProductSearchCriteria criteria, String pathPrefix) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        applyFilter(where, params, criteria, pathPrefix);

        boolean hasCategoryFilter = criteria.categoryId() != null;

        String sql =
            """
            SELECT
                COALESCE(MIN(p.min_price), 0) AS min_price,
                COALESCE(MAX(p.max_price), 0) AS max_price
            FROM products p
            """ +
            (hasCategoryFilter ? "JOIN categories c ON p.category_id = c.id\n" : "") +
            """
            WHERE
            """ +
            String.join("\n  AND ", where);

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);

        return spec
            .query((rs, rowNum) -> new PriceRange(rs.getBigDecimal("min_price"), rs.getBigDecimal("max_price")))
            .optional()
            .orElse(new PriceRange(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private Map<String, List<OptionFilterView>> getOptionFacets(ProductSearchCriteria criteria, String pathPrefix) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        applyFilter(where, params, criteria, pathPrefix);

        boolean hasCategoryFilter = criteria.categoryId() != null;

        String sql =
            """
            WITH matched_options AS (
                SELECT DISTINCT
                    pov.option_id,
                    pov.value_id
                FROM product_option_values pov
                JOIN products p ON pov.product_id = p.id
            """ +
            (hasCategoryFilter ? "    JOIN categories c ON p.category_id = c.id\n" : "") +
            """
                WHERE
            """ +
            String.join("\n      AND ", where) +
            """
            )
            SELECT
                mo.option_id,
                mo.value_id,
                o.name AS option_name,
                ov.name AS value_name
            FROM matched_options mo
            JOIN options o ON mo.option_id = o.id
            JOIN option_values ov ON mo.value_id = ov.id
            ORDER BY o.name ASC, ov.name ASC
            """;

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);

        List<OptionFilterView> flatFacets = spec
            .query((rs, rowNum) ->
                new OptionFilterView(
                    rs.getLong("option_id"),
                    rs.getLong("value_id"),
                    rs.getString("option_name"),
                    rs.getString("value_name")
                )
            )
            .list();

        return flatFacets
            .stream()
            .collect(Collectors.groupingBy(OptionFilterView::optionName, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<AttributeFilterView>> getAttributeFacets(
        ProductSearchCriteria criteria,
        String pathPrefix
    ) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        applyFilter(where, params, criteria, pathPrefix);

        where.add("jsonb_typeof(p.attributes) = 'object' AND p.attributes != '{}'::jsonb");

        boolean hasCategoryFilter = criteria.categoryId() != null;

        String sql =
            """
            SELECT DISTINCT
                kv.key AS attr_code,
                val AS val_code
            FROM products p
            """ +
            (hasCategoryFilter ? "JOIN categories c ON p.category_id = c.id\n" : "") +
            """
            CROSS JOIN LATERAL jsonb_each(p.attributes) AS kv
            CROSS JOIN LATERAL jsonb_array_elements_text(kv.value) AS val
            WHERE
            """ +
            String.join("\n  AND ", where) +
            """

              AND val IS NOT NULL AND val != ''
            ORDER BY kv.key ASC, val ASC
            """;

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);

        List<AttributeFilterView> flatFacets = spec
            .query((rs, rowNum) -> new AttributeFilterView(rs.getString("attr_code"), rs.getString("val_code")))
            .list();

        return flatFacets
            .stream()
            .collect(Collectors.groupingBy(AttributeFilterView::attrCode, LinkedHashMap::new, Collectors.toList()));
    }

    private void applyFilter(
        List<String> where,
        Map<String, Object> params,
        ProductSearchCriteria criteria,
        String pathPrefix
    ) {
        where.add("p.status = 'PUBLISHED'");

        if (criteria.categoryId() != null) {
            if (pathPrefix != null) {
                where.add("(c.id = :categoryId OR c.path LIKE :pathPrefix)");
                params.put("pathPrefix", pathPrefix);
            } else {
                where.add("c.id = :categoryId");
            }
            params.put("categoryId", criteria.categoryId());
        }

        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            where.add(
                """
                (to_tsvector('simple', immutable_unaccent(p.name)) @@ websearch_to_tsquery('simple', immutable_unaccent(:keyword))
                 OR immutable_unaccent(p.name) % immutable_unaccent(:keyword)
                 OR immutable_unaccent(:keyword) <% immutable_unaccent(p.name))
                """
            );
            params.put("keyword", criteria.keyword().trim());
        }
    }
}
