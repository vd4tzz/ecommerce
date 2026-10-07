package io.d4tzz.ecommerce.checkout.infrastructure.catalog;

import io.d4tzz.ecommerce.checkout.application.port.VariantCatalog;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component("checkoutVariantCatalog")
public class JdbcVariantCatalog implements VariantCatalog {

    private final JdbcClient jdbcClient;
    private final MediaUrlResolver mediaUrlResolver;

    public JdbcVariantCatalog(JdbcClient jdbcClient, MediaUrlResolver mediaUrlResolver) {
        this.jdbcClient = jdbcClient;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Override
    public Map<VariantId, VariantSnapshot> findForSale(Collection<VariantId> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = variantIds.stream().map(VariantId::value).distinct().toList();
        Map<Long, String> attributes = attributesOf(ids);

        Map<VariantId, VariantSnapshot> result = new HashMap<>();
        jdbcClient
            .sql(
                """
                SELECT
                    v.id AS variant_id,
                    v.product_id,
                    v.sku,
                    v.price,
                    p.name AS product_name,
                    COALESCE(
                        v.media_id,
                        (
                            SELECT pm.media_id
                            FROM product_media pm
                            WHERE pm.product_id = p.id
                            ORDER BY pm.position ASC
                            LIMIT 1
                        )
                    ) AS media_id
                FROM variants v
                JOIN products p ON p.id = v.product_id
                WHERE v.id IN (:ids) AND p.status = 'PUBLISHED'
                """
            )
            .param("ids", ids)
            .query((rs, rowNum) ->
                result.put(
                    VariantId.of(rs.getLong("variant_id")),
                    new VariantSnapshot(
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getString("sku"),
                        attributes.getOrDefault(rs.getLong("variant_id"), ""),
                        mediaUrlResolver.resolveUrl(rs.getString("media_id")),
                        rs.getBigDecimal("price")
                    )
                )
            )
            .list();
        return result;
    }

    /** "Color: Red, Size: XL" per variant id. Variants without options are left out. */
    private Map<Long, String> attributesOf(List<Long> ids) {
        Map<Long, List<String>> parts = new LinkedHashMap<>();
        jdbcClient
            .sql(
                """
                SELECT vc.variant_id, o.name AS option_name, ov.name AS value_name
                FROM variant_combinations vc
                JOIN options o ON o.id = vc.option_id
                JOIN option_values ov ON ov.id = vc.value_id
                WHERE vc.variant_id IN (:ids)
                ORDER BY o.id ASC
                """
            )
            .param("ids", ids)
            .query((rs, rowNum) ->
                parts
                    .computeIfAbsent(rs.getLong("variant_id"), id -> new ArrayList<>())
                    .add(rs.getString("option_name") + ": " + rs.getString("value_name"))
            )
            .list();
        return parts
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.join(", ", entry.getValue())));
    }
}
