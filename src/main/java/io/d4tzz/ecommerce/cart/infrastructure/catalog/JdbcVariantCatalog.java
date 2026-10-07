package io.d4tzz.ecommerce.cart.infrastructure.catalog;

import io.d4tzz.ecommerce.cart.application.port.VariantCatalog;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component("cartVariantCatalog")
public class JdbcVariantCatalog implements VariantCatalog {

    private final JdbcClient jdbcClient;

    public JdbcVariantCatalog(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Set<VariantId> findNotForSale(Collection<VariantId> variantIds) {
        if (variantIds.isEmpty()) {
            return Set.of();
        }

        List<Long> ids = variantIds.stream().map(VariantId::value).distinct().toList();
        Set<Long> forSale = new HashSet<>(
            jdbcClient
                .sql(
                    """
                    SELECT v.id FROM variants v
                    JOIN products p ON p.id = v.product_id
                    WHERE v.id IN (:ids) AND p.status = 'PUBLISHED'
                    """
                )
                .param("ids", ids)
                .query(Long.class)
                .list()
        );

        Set<VariantId> notForSale = new HashSet<>();
        for (Long id : ids) {
            if (!forSale.contains(id)) {
                notForSale.add(VariantId.of(id));
            }
        }
        return notForSale;
    }
}
