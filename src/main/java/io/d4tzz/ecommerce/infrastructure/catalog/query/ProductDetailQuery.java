package io.d4tzz.ecommerce.infrastructure.catalog.query;

import io.d4tzz.ecommerce.query.catalog.ProductDetailView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProductDetailQuery {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;
    private final MediaUrlResolver urlResolver;

    public ProductDetailQuery(
        JdbcClient jdbcClient,
        ObjectMapper objectMapper,
        MediaUrlResolver urlResolver
    ) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        this.urlResolver = urlResolver;
    }

    public Optional<ProductDetailView> execute(String slug) {
        return executeWhere("p.slug = :slug", Map.of("slug", slug));
    }

    public Optional<ProductDetailView> execute(Long id) {
        return executeWhere("p.id = :id", Map.of("id", id));
    }

    private Optional<ProductDetailView> executeWhere(
        String whereClause,
        Map<String, Object> params
    ) {
        String sql =
            """
            SELECT
                p.id,
                p.name,
                p.slug,
                p.description,
                p.status,
                p.category_id,
                c.name AS category_name,
                c.slug AS category_slug,
                c.path AS category_path,
                p.min_price,
                p.max_price,
                p.attributes
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE\s""" + whereClause;

        var spec = jdbcClient.sql(sql);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            spec = spec.param(entry.getKey(), entry.getValue());
        }

        return spec
            .query((rs, rowNum) -> {
                Long productId = rs.getLong("id");
                String name = rs.getString("name");
                String productSlug = rs.getString("slug");
                String description = rs.getString("description");
                String status = rs.getString("status");
                Long categoryId = rs.getObject("category_id", Long.class);
                String categoryName = rs.getString("category_name");
                String categorySlug = rs.getString("category_slug");
                String categoryPath = rs.getString("category_path");
                BigDecimal minPrice = rs.getBigDecimal("min_price");
                BigDecimal maxPrice = rs.getBigDecimal("max_price");
                String attributesJson = rs.getString("attributes");

                List<ProductDetailView.BreadcrumbItem> breadcrumbs =
                    loadBreadcrumbs(categoryId, categoryPath);

                List<String> mediaUrls = loadProductMedia(productId);

                List<ProductDetailView.ProductOptionView> options =
                    loadProductOptions(productId);

                List<ProductDetailView.ProductVariantView> variants =
                    loadProductVariants(productId);

                Map<String, Object> attributes = parseAttributes(
                    attributesJson
                );

                return new ProductDetailView(
                    productId,
                    name,
                    productSlug,
                    description,
                    status,
                    categoryId,
                    categoryName,
                    categorySlug,
                    breadcrumbs,
                    mediaUrls,
                    minPrice,
                    maxPrice,
                    options,
                    variants,
                    attributes
                );
            })
            .optional();
    }

    private List<ProductDetailView.BreadcrumbItem> loadBreadcrumbs(
        Long categoryId,
        String categoryPath
    ) {
        if (categoryId == null) return Collections.emptyList();

        List<Long> categoryIds = new ArrayList<>();
        if (categoryPath != null && !categoryPath.isBlank()) {
            categoryIds.addAll(
                Arrays.stream(categoryPath.split("\\."))
                    .filter(s -> !s.isBlank())
                    .map(Long::parseLong)
                    .toList()
            );
        }
        categoryIds.add(categoryId);

        String sql = """
            SELECT id, name, slug
            FROM categories
            WHERE id IN (:ids)

        """;

        Map<Long, ProductDetailView.BreadcrumbItem> itemMap = jdbcClient
            .sql(sql)
            .param("ids", categoryIds)
            .query((rs, rowNum) ->
                new ProductDetailView.BreadcrumbItem(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("slug")
                )
            )
            .list()
            .stream()
            .collect(
                Collectors.toMap(ProductDetailView.BreadcrumbItem::id, b -> b)
            );

        return categoryIds
            .stream()
            .map(itemMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<String> loadProductMedia(Long productId) {
        String sql = """
            SELECT media_id
            FROM product_media
            WHERE product_id = :productId
            ORDER BY position ASC
        """;

        return jdbcClient
            .sql(sql)
            .param("productId", productId)
            .query((rs, rowNum) -> urlResolver.resolveUrl(rs.getString("media_id")))
            .list();
    }

    private List<ProductDetailView.ProductOptionView> loadProductOptions(
        Long productId
    ) {
        String sql = """
            SELECT
                o.id AS option_id,
                o.name AS option_name,
                ov.id AS value_id,
                ov.name AS value_name
            FROM product_option_values pov
            JOIN product_options po ON po.product_id = pov.product_id AND po.option_id = pov.option_id
            JOIN options o ON pov.option_id = o.id
            JOIN option_values ov ON pov.value_id = ov.id
            WHERE pov.product_id = :productId
            ORDER BY po.position ASC, pov.position ASC
        """;

        record RawOptionValue(
            Long optionId,
            String optionName,
            Long valueId,
            String valueName
        ) {}

        List<RawOptionValue> rawList = jdbcClient
            .sql(sql)
            .param("productId", productId)
            .query((rs, rowNum) ->
                new RawOptionValue(
                    rs.getLong("option_id"),
                    rs.getString("option_name"),
                    rs.getLong("value_id"),
                    rs.getString("value_name")
                )
            )
            .list();

        Map<Long, String> optionNameMap = new LinkedHashMap<>();
        Map<
            Long,
            List<ProductDetailView.ProductOptionView.OptionValueView>
        > valuesMap = new LinkedHashMap<>();

        for (RawOptionValue row : rawList) {
            optionNameMap.putIfAbsent(row.optionId(), row.optionName());
            valuesMap
                .computeIfAbsent(row.optionId(), k -> new ArrayList<>())
                .add(
                    new ProductDetailView.ProductOptionView.OptionValueView(
                        row.valueId(),
                        row.valueName()
                    )
                );
        }

        return optionNameMap
            .entrySet()
            .stream()
            .map(entry ->
                new ProductDetailView.ProductOptionView(
                    entry.getKey(),
                    entry.getValue(),
                    valuesMap.getOrDefault(entry.getKey(), List.of())
                )
            )
            .toList();
    }

    private List<ProductDetailView.ProductVariantView> loadProductVariants(
        Long productId
    ) {
        String sql = """
            SELECT
                v.id AS variant_id,
                v.sku,
                v.price,
                v.compare_at,
                v.media_id,
                v.length,
                v.width,
                v.height,
                v.weight,
                vc.option_id,
                vc.value_id
            FROM variants v
            LEFT JOIN variant_combinations vc ON v.id = vc.variant_id
            WHERE v.product_id = :productId
            ORDER BY v.id ASC
        """;

        record RawVariantRow(
            Long variantId,
            String sku,
            BigDecimal price,
            BigDecimal compareAt,
            String mediaId,
            Integer length,
            Integer width,
            Integer height,
            Integer weight,
            Long optionId,
            Long valueId
        ) {}
        List<RawVariantRow> rows = jdbcClient
            .sql(sql)
            .param("productId", productId)
            .query((rs, rowNum) ->
                new RawVariantRow(
                    rs.getLong("variant_id"),
                    rs.getString("sku"),
                    rs.getBigDecimal("price"),
                    rs.getBigDecimal("compare_at"),
                    rs.getString("media_id"),
                    rs.getObject("length", Integer.class),
                    rs.getObject("width", Integer.class),
                    rs.getObject("height", Integer.class),
                    rs.getObject("weight", Integer.class),
                    rs.getObject("option_id", Long.class),
                    rs.getObject("value_id", Long.class)
                )
            )
            .list();

        Map<Long, ProductDetailView.ProductVariantView> variantMap =
            new LinkedHashMap<>();
        Map<Long, Map<Long, Long>> combinationMap = new LinkedHashMap<>();

        for (RawVariantRow r : rows) {
            Map<Long, Long> combination = combinationMap.computeIfAbsent(
                r.variantId(),
                k -> new LinkedHashMap<>()
            );
            if (r.optionId() != null && r.valueId() != null) {
                combination.put(r.optionId(), r.valueId());
            }

            variantMap.putIfAbsent(
                r.variantId(),
                new ProductDetailView.ProductVariantView(
                    r.variantId(),
                    r.sku(),
                    r.price(),
                    r.compareAt(),
                    urlResolver.resolveUrl(r.mediaId()),
                    r.length(),
                    r.width(),
                    r.height(),
                    r.weight(),
                    combination
                )
            );
        }

        return new ArrayList<>(variantMap.values());
    }

    private Map<String, Object> parseAttributes(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(
                json,
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
