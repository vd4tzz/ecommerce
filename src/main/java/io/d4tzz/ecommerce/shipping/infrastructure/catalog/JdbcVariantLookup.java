package io.d4tzz.ecommerce.shipping.infrastructure.catalog;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.shipping.application.port.VariantLookup;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcVariantLookup implements VariantLookup {

    private final JdbcClient jdbcClient;

    public JdbcVariantLookup(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Map<VariantId, Measurements> measurementsOf(Collection<VariantId> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = variantIds.stream().map(VariantId::value).distinct().toList();
        Map<VariantId, Measurements> result = new HashMap<>();
        jdbcClient
            .sql(
                """
                SELECT id, weight, length, width, height FROM variants
                WHERE id IN (:ids)
                  AND weight IS NOT NULL AND length IS NOT NULL AND width IS NOT NULL AND height IS NOT NULL
                """
            )
            .param("ids", ids)
            .query((rs, rowNum) ->
                result.put(
                    VariantId.of(rs.getLong("id")),
                    new Measurements(rs.getInt("weight"), rs.getInt("length"), rs.getInt("width"), rs.getInt("height"))
                )
            )
            .list();
        return result;
    }
}
