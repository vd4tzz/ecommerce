package io.d4tzz.ecommerce.inventory.infrastructure.query;

import io.d4tzz.ecommerce.inventory.query.StockItemView;
import io.d4tzz.ecommerce.inventory.query.StockLedgerEntryView;
import io.d4tzz.ecommerce.inventory.query.StockLedgerSearchCriteria;
import io.d4tzz.ecommerce.inventory.query.StockQueryService;
import io.d4tzz.ecommerce.inventory.query.StockReservationView;
import io.d4tzz.ecommerce.inventory.query.StockSearchCriteria;
import io.d4tzz.ecommerce.inventory.query.VariantAvailabilityView;
import io.d4tzz.ecommerce.query.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JdbcStockQueryService implements StockQueryService {

    private static final String LEDGER_SELECT = """
        SELECT e.id, e.warehouse_id, w.code AS warehouse_code, e.variant_id, v.sku, p.name AS product_name,
               e.type, e.quantity, e.on_hand_after, e.reserved_after, e.reference_type, e.reference_value,
               e.note, e.occurred_at
        """;

    private static final String LEDGER_FROM = """
        FROM stock_ledger_entries e
        JOIN warehouses w ON w.id = e.warehouse_id
        LEFT JOIN variants v ON v.id = e.variant_id
        LEFT JOIN products p ON p.id = v.product_id
        """;

    private final JdbcClient jdbcClient;

    public JdbcStockQueryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PageResponse<StockItemView> getStock(StockSearchCriteria criteria) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (criteria.warehouseId() != null) {
            where.add("i.warehouse_id = :warehouseId");
            params.put("warehouseId", criteria.warehouseId());
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            where.add("(v.sku ILIKE :kwLike OR immutable_unaccent(p.name) ILIKE immutable_unaccent(:kwLike))");
            params.put("kwLike", "%" + criteria.keyword().trim() + "%");
        }
        if (criteria.maxAvailable() != null) {
            where.add("(i.on_hand - i.reserved) <= :maxAvailable");
            params.put("maxAvailable", criteria.maxAvailable());
        }

        String from = """
            FROM inventory_items i
            JOIN warehouses w ON w.id = i.warehouse_id
            JOIN variants v ON v.id = i.variant_id
            JOIN products p ON p.id = v.product_id
            """ + whereClause(where);

        long total = count(from, params);
        if (total == 0) {
            return PageResponse.empty(criteria.getPage(), criteria.getSize());
        }

        // Looking for low stock: the fewest sellable units first
        String orderBy = criteria.maxAvailable() != null
            ? "ORDER BY (i.on_hand - i.reserved) ASC, p.name ASC, v.sku ASC, w.code ASC"
            : "ORDER BY p.name ASC, v.sku ASC, w.code ASC";

        String sql = """
            SELECT i.warehouse_id, w.code AS warehouse_code, i.variant_id, v.sku, p.id AS product_id,
                   p.name AS product_name, i.on_hand, i.reserved, i.updated_at
            """ + from + orderBy + """

            LIMIT :limit OFFSET :offset
            """;

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);
        List<StockItemView> content = spec
            .param("limit", criteria.getSize())
            .param("offset", criteria.getOffset())
            .query((rs, rowNum) -> {
                int onHand = rs.getInt("on_hand");
                int reserved = rs.getInt("reserved");
                return new StockItemView(
                    rs.getLong("warehouse_id"),
                    rs.getString("warehouse_code"),
                    rs.getLong("variant_id"),
                    rs.getString("sku"),
                    rs.getLong("product_id"),
                    rs.getString("product_name"),
                    onHand,
                    reserved,
                    onHand - reserved,
                    rs.getTimestamp("updated_at").toInstant()
                );
            })
            .list();

        return PageResponse.of(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public List<VariantAvailabilityView> getAvailability(Collection<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT i.variant_id, i.warehouse_id, w.code AS warehouse_code, (i.on_hand - i.reserved) AS available
            FROM inventory_items i
            JOIN warehouses w ON w.id = i.warehouse_id
            WHERE i.variant_id IN (:variantIds) AND w.status = 'ACTIVE'
            ORDER BY i.variant_id ASC, available DESC, i.warehouse_id ASC
            """;

        return jdbcClient
            .sql(sql)
            .param("variantIds", List.copyOf(variantIds))
            .query((rs, rowNum) ->
                new VariantAvailabilityView(
                    rs.getLong("variant_id"),
                    rs.getLong("warehouse_id"),
                    rs.getString("warehouse_code"),
                    rs.getInt("available")
                )
            )
            .list();
    }

    @Override
    public PageResponse<StockLedgerEntryView> getLedger(StockLedgerSearchCriteria criteria) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (criteria.warehouseId() != null) {
            where.add("e.warehouse_id = :warehouseId");
            params.put("warehouseId", criteria.warehouseId());
        }
        if (criteria.variantId() != null) {
            where.add("e.variant_id = :variantId");
            params.put("variantId", criteria.variantId());
        }
        if (criteria.type() != null && !criteria.type().isBlank()) {
            where.add("e.type = :type");
            params.put("type", criteria.type().trim().toUpperCase());
        }
        if (criteria.referenceType() != null && !criteria.referenceType().isBlank()) {
            where.add("e.reference_type = :referenceType");
            params.put("referenceType", criteria.referenceType().trim().toUpperCase());
        }
        if (criteria.referenceValue() != null && !criteria.referenceValue().isBlank()) {
            where.add("e.reference_value = :referenceValue");
            params.put("referenceValue", criteria.referenceValue().trim());
        }
        // OffsetDateTime because the Postgres driver does not bind Instant
        if (criteria.from() != null) {
            where.add("e.occurred_at >= :from");
            params.put("from", OffsetDateTime.ofInstant(criteria.from(), ZoneOffset.UTC));
        }
        if (criteria.to() != null) {
            where.add("e.occurred_at < :to");
            params.put("to", OffsetDateTime.ofInstant(criteria.to(), ZoneOffset.UTC));
        }

        String from = LEDGER_FROM + whereClause(where);

        long total = count(from, params);
        if (total == 0) {
            return PageResponse.empty(criteria.getPage(), criteria.getSize());
        }

        String sql = LEDGER_SELECT + from + """
            ORDER BY e.occurred_at DESC, e.id ASC
            LIMIT :limit OFFSET :offset
            """;

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);
        List<StockLedgerEntryView> content = spec
            .param("limit", criteria.getSize())
            .param("offset", criteria.getOffset())
            .query(JdbcStockQueryService::mapLedgerEntry)
            .list();

        return PageResponse.of(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public List<StockLedgerEntryView> getLedgerEntries(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        String sql = LEDGER_SELECT + LEDGER_FROM + """
            WHERE e.id IN (:ids)
            ORDER BY e.warehouse_id ASC, e.variant_id ASC
            """;

        return jdbcClient
            .sql(sql)
            .param("ids", List.copyOf(ids))
            .query(JdbcStockQueryService::mapLedgerEntry)
            .list();
    }

    @Override
    public List<StockReservationView> getReservationsByOrder(Long orderId) {
        String sql = """
            SELECT r.id, r.order_id, r.warehouse_id, w.code AS warehouse_code, r.status, r.created_at, r.updated_at,
                   l.variant_id, v.sku, l.quantity
            FROM stock_reservations r
            JOIN warehouses w ON w.id = r.warehouse_id
            JOIN stock_reservation_lines l ON l.reservation_id = r.id
            LEFT JOIN variants v ON v.id = l.variant_id
            WHERE r.order_id = :orderId
            ORDER BY r.warehouse_id ASC, l.variant_id ASC
            """;

        record Row(
            Long id,
            Long orderId,
            Long warehouseId,
            String warehouseCode,
            String status,
            Instant createdAt,
            Instant updatedAt,
            StockReservationView.Line line
        ) {}

        List<Row> rows = jdbcClient
            .sql(sql)
            .param("orderId", orderId)
            .query((rs, rowNum) ->
                new Row(
                    rs.getLong("id"),
                    rs.getLong("order_id"),
                    rs.getLong("warehouse_id"),
                    rs.getString("warehouse_code"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    new StockReservationView.Line(rs.getLong("variant_id"), rs.getString("sku"), rs.getInt("quantity"))
                )
            )
            .list();

        Map<Long, List<Row>> rowsByReservation = new LinkedHashMap<>();
        for (Row row : rows) {
            rowsByReservation.computeIfAbsent(row.id(), id -> new ArrayList<>()).add(row);
        }

        return rowsByReservation
            .values()
            .stream()
            .map(group -> {
                Row first = group.getFirst();
                return new StockReservationView(
                    first.id(),
                    first.orderId(),
                    first.warehouseId(),
                    first.warehouseCode(),
                    first.status(),
                    group.stream().map(Row::line).toList(),
                    first.createdAt(),
                    first.updatedAt()
                );
            })
            .toList();
    }

    private static StockLedgerEntryView mapLedgerEntry(ResultSet rs, int rowNum) throws SQLException {
        return new StockLedgerEntryView(
            rs.getObject("id", UUID.class),
            rs.getLong("warehouse_id"),
            rs.getString("warehouse_code"),
            rs.getLong("variant_id"),
            rs.getString("sku"),
            rs.getString("product_name"),
            rs.getString("type"),
            rs.getInt("quantity"),
            rs.getInt("on_hand_after"),
            rs.getInt("reserved_after"),
            rs.getString("reference_type"),
            rs.getString("reference_value"),
            rs.getString("note"),
            rs.getTimestamp("occurred_at").toInstant()
        );
    }

    private long count(String from, Map<String, Object> params) {
        var spec = jdbcClient.sql("SELECT COUNT(*)\n" + from);
        params.forEach(spec::param);
        Long total = spec.query(Long.class).single();
        return total != null ? total : 0L;
    }

    private static String whereClause(List<String> where) {
        return where.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", where) + "\n";
    }
}
