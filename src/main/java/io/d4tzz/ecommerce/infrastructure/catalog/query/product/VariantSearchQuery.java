package io.d4tzz.ecommerce.infrastructure.catalog.query.product;

import io.d4tzz.ecommerce.query.catalog.product.ProductVariantSummaryView;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class VariantSearchQuery {

    private final JdbcClient jdbcClient;
    private final MediaUrlResolver mediaUrlResolver;

    public VariantSearchQuery(JdbcClient jdbcClient, MediaUrlResolver mediaUrlResolver) {
        this.jdbcClient = jdbcClient;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    public List<ProductVariantSummaryView> execute(String keyword, int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);

        StringBuilder sql = new StringBuilder("""
            SELECT
                v.id AS variant_id,
                v.product_id,
                v.sku,
                p.name AS product_name,
                p.slug AS product_slug,
                p.status AS product_status,
                v.price,
                v.media_id
            FROM variants v
            JOIN products p ON v.product_id = p.id
            """);

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql.append(" WHERE (LOWER(p.name) LIKE :keyword OR LOWER(v.sku) LIKE :keyword)");
        }
        sql.append(" ORDER BY p.name ASC, v.id ASC LIMIT :limit");

        var spec = jdbcClient.sql(sql.toString()).param("limit", safeLimit);
        if (hasKeyword) {
            spec = spec.param("keyword", keyword.trim().toLowerCase() + "%");
        }

        record RawVariant(
            Long variantId,
            Long productId,
            String sku,
            String productName,
            String productSlug,
            String productStatus,
            BigDecimal price,
            String mediaId
        ) {}

        List<RawVariant> rawVariants = spec.query((rs, rowNum) -> new RawVariant(
            rs.getLong("variant_id"),
            rs.getLong("product_id"),
            rs.getString("sku"),
            rs.getString("product_name"),
            rs.getString("product_slug"),
            rs.getString("product_status"),
            rs.getBigDecimal("price"),
            rs.getString("media_id")
        )).list();

        if (rawVariants.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> variantIds = rawVariants.stream().map(RawVariant::variantId).toList();

        String combinationSql = """
            SELECT
                vc.variant_id,
                o.name AS option_name,
                ov.name AS value_name
            FROM variant_combinations vc
            JOIN options o ON vc.option_id = o.id
            JOIN option_values ov ON vc.value_id = ov.id
            WHERE vc.variant_id IN (:variantIds)
            ORDER BY o.id ASC
            """;

        Map<Long, Map<String, String>> combinationMap = new LinkedHashMap<>();
        jdbcClient.sql(combinationSql)
            .param("variantIds", variantIds)
            .query((rs, rowNum) -> {
                Long variantId = rs.getLong("variant_id");
                String optName = rs.getString("option_name");
                String valName = rs.getString("value_name");
                combinationMap
                    .computeIfAbsent(variantId, k -> new LinkedHashMap<>())
                    .put(optName, valName);
                return null;
            }).list();

        List<ProductVariantSummaryView> result = new ArrayList<>();
        for (RawVariant v : rawVariants) {
            Map<String, String> combination = combinationMap.getOrDefault(v.variantId(), Collections.emptyMap());
            result.add(new ProductVariantSummaryView(
                v.variantId(),
                v.productId(),
                v.sku(),
                v.productName(),
                v.productSlug(),
                v.productStatus(),
                v.price(),
                mediaUrlResolver.resolveUrl(v.mediaId()),
                combination
            ));
        }

        return result;
    }
}
