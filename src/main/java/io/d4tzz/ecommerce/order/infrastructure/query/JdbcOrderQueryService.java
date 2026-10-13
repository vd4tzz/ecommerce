package io.d4tzz.ecommerce.order.infrastructure.query;

import io.d4tzz.ecommerce.order.query.OrderQueryService;
import io.d4tzz.ecommerce.order.query.OrderSearchCriteria;
import io.d4tzz.ecommerce.order.query.OrderSummaryView;
import io.d4tzz.ecommerce.order.query.OrderView;
import io.d4tzz.ecommerce.common.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JdbcOrderQueryService implements OrderQueryService {

    private static final String SELECT_ORDER = """
        SELECT o.id, o.order_code, o.user_id, o.recipient_name, o.recipient_phone,
               o.province, o.district, o.ward, o.address_detail,
               o.subtotal, o.shipping_fee, o.total_amount, o.status, o.payment_status, o.note,
               o.payment_expires_at, o.paid_at, o.cancel_reason, o.cancelled_at, o.completed_at,
               o.created_at, o.updated_at,
               (o.status = 'PENDING' AND o.payment_status = 'UNPAID' AND o.payment_expires_at < now()) AS payment_expired
        FROM orders o
        """;

    private final JdbcClient jdbcClient;

    public JdbcOrderQueryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PageResponse<OrderSummaryView> searchOrders(OrderSearchCriteria criteria) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (criteria.userId() != null) {
            where.add("o.user_id = :userId");
            params.put("userId", criteria.userId());
        }
        if (criteria.status() != null && !criteria.status().isBlank()) {
            where.add("o.status = :status");
            params.put("status", criteria.status().trim().toUpperCase());
        }
        if (criteria.paymentStatus() != null && !criteria.paymentStatus().isBlank()) {
            where.add("o.payment_status = :paymentStatus");
            params.put("paymentStatus", criteria.paymentStatus().trim().toUpperCase());
        }
        if (Boolean.TRUE.equals(criteria.readyToConfirm())) {
            where.add("o.status = 'PENDING' AND (o.payment_expires_at IS NULL OR o.payment_status = 'PAID')");
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            where.add(
                "(o.order_code ILIKE :kwLike OR o.recipient_phone ILIKE :kwLike" +
                    " OR immutable_unaccent(o.recipient_name) ILIKE immutable_unaccent(:kwLike))"
            );
            params.put("kwLike", "%" + criteria.keyword().trim() + "%");
        }

        String from = "FROM orders o\n" + (where.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", where) + "\n");

        var countSpec = jdbcClient.sql("SELECT COUNT(*)\n" + from);
        params.forEach(countSpec::param);
        Long total = countSpec.query(Long.class).single();
        if (total == null || total == 0) {
            return PageResponse.empty(criteria.getPage(), criteria.getSize());
        }

        String sql = """
            SELECT o.id, o.order_code, o.user_id, o.recipient_name, o.recipient_phone, o.total_amount,
                   o.status, o.payment_status, o.payment_expires_at, o.created_at,
                   (o.status = 'PENDING' AND o.payment_status = 'UNPAID' AND o.payment_expires_at < now()) AS payment_expired,
                   (SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.order_id = o.id)
                       AS total_quantity
            """ + from + """
            ORDER BY o.created_at DESC, o.id DESC
            LIMIT :limit OFFSET :offset
            """;

        var spec = jdbcClient.sql(sql);
        params.forEach(spec::param);
        List<OrderSummaryView> content = spec
            .param("limit", criteria.getSize())
            .param("offset", criteria.getOffset())
            .query((rs, rowNum) ->
                new OrderSummaryView(
                    rs.getLong("id"),
                    rs.getString("order_code"),
                    rs.getLong("user_id"),
                    rs.getString("recipient_name"),
                    rs.getString("recipient_phone"),
                    rs.getInt("total_quantity"),
                    rs.getBigDecimal("total_amount"),
                    rs.getString("status"),
                    rs.getString("payment_status"),
                    toInstant(rs.getTimestamp("payment_expires_at")),
                    rs.getBoolean("payment_expired"),
                    toInstant(rs.getTimestamp("created_at"))
                )
            )
            .list();

        return PageResponse.of(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public Optional<OrderView> getOrder(Long id) {
        return jdbcClient
            .sql(SELECT_ORDER + "WHERE o.id = :id")
            .param("id", id)
            .query((rs, rowNum) -> toView(rs))
            .optional();
    }

    @Override
    public Optional<OrderView> getUserOrder(Long userId, String orderCode) {
        return jdbcClient
            .sql(SELECT_ORDER + "WHERE o.order_code = :orderCode AND o.user_id = :userId")
            .param("orderCode", orderCode.trim().toUpperCase())
            .param("userId", userId)
            .query((rs, rowNum) -> toView(rs))
            .optional();
    }

    private List<OrderView.Item> getItems(Long orderId) {
        return jdbcClient
            .sql(
                """
                SELECT id, variant_id, product_id, product_name, sku, options_description, thumbnail_url,
                       unit_price, quantity, total_price
                FROM order_items
                WHERE order_id = :orderId
                ORDER BY id ASC
                """
            )
            .param("orderId", orderId)
            .query((rs, rowNum) ->
                new OrderView.Item(
                    rs.getLong("id"),
                    rs.getLong("variant_id"),
                    rs.getLong("product_id"),
                    rs.getString("product_name"),
                    rs.getString("sku"),
                    rs.getString("options_description"),
                    rs.getString("thumbnail_url"),
                    rs.getBigDecimal("unit_price"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("total_price")
                )
            )
            .list();
    }

    private OrderView toView(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        return new OrderView(
            id,
            rs.getString("order_code"),
            rs.getLong("user_id"),
            rs.getString("recipient_name"),
            rs.getString("recipient_phone"),
            rs.getString("province"),
            rs.getString("district"),
            rs.getString("ward"),
            rs.getString("address_detail"),
            getItems(id),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("shipping_fee"),
            rs.getBigDecimal("total_amount"),
            rs.getString("status"),
            rs.getString("payment_status"),
            rs.getString("note"),
            toInstant(rs.getTimestamp("payment_expires_at")),
            rs.getBoolean("payment_expired"),
            toInstant(rs.getTimestamp("paid_at")),
            rs.getString("cancel_reason"),
            toInstant(rs.getTimestamp("cancelled_at")),
            toInstant(rs.getTimestamp("completed_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
