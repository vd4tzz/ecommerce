package io.d4tzz.ecommerce.inventory.infrastructure.query;

import io.d4tzz.ecommerce.inventory.query.WarehouseQueryService;
import io.d4tzz.ecommerce.inventory.query.WarehouseView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JdbcWarehouseQueryService implements WarehouseQueryService {

    private static final String SELECT_WAREHOUSE = """
        SELECT w.id, w.code, w.name, w.province, w.district, w.ward, w.address_detail,
               w.status, w.created_at, w.updated_at
        FROM warehouses w
        """;

    private final JdbcClient jdbcClient;

    public JdbcWarehouseQueryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<WarehouseView> getWarehouses() {
        return jdbcClient
            .sql(SELECT_WAREHOUSE + "ORDER BY w.status ASC, w.code ASC")
            .query((rs, rowNum) -> toView(rs))
            .list();
    }

    @Override
    public Optional<WarehouseView> getWarehouse(Long id) {
        return jdbcClient
            .sql(SELECT_WAREHOUSE + "WHERE w.id = :id")
            .param("id", id)
            .query((rs, rowNum) -> toView(rs))
            .optional();
    }

    private WarehouseView toView(ResultSet rs) throws SQLException {
        return new WarehouseView(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("province"),
            rs.getString("district"),
            rs.getString("ward"),
            rs.getString("address_detail"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
