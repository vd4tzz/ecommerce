package io.d4tzz.ecommerce.inventory.infrastructure.catalog;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.inventory.application.port.VariantCatalog;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcVariantCatalog implements VariantCatalog {

    private final JdbcClient jdbcClient;

    public JdbcVariantCatalog(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Set<VariantId> findMissing(Collection<VariantId> variantIds) {
        if (variantIds.isEmpty()) {
            return Set.of();
        }

        List<Long> ids = variantIds.stream().map(VariantId::value).distinct().toList();
        Set<Long> existing = new HashSet<>(
            jdbcClient.sql("SELECT id FROM variants WHERE id IN (:ids)").param("ids", ids).query(Long.class).list()
        );

        Set<VariantId> missing = new HashSet<>();
        for (Long id : ids) {
            if (!existing.contains(id)) {
                missing.add(VariantId.of(id));
            }
        }
        return missing;
    }
}
