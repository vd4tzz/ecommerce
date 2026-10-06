package io.d4tzz.ecommerce.shipping.infrastructure.inventory;

import io.d4tzz.ecommerce.domain.user.Address;
import io.d4tzz.ecommerce.shipping.application.port.WarehouseLookup;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcWarehouseLookup implements WarehouseLookup {

    private final JdbcClient jdbcClient;

    public JdbcWarehouseLookup(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Map<Long, Address> addressesOf(Collection<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Address> result = new HashMap<>();
        jdbcClient
            .sql("SELECT id, province, district, ward, address_detail FROM warehouses WHERE id IN (:ids)")
            .param("ids", List.copyOf(warehouseIds))
            .query((rs, rowNum) ->
                result.put(
                    rs.getLong("id"),
                    Address.of(
                        rs.getString("province"),
                        rs.getString("district"),
                        rs.getString("ward"),
                        rs.getString("address_detail")
                    )
                )
            )
            .list();
        return result;
    }
}
