package io.d4tzz.ecommerce.payment.infrastructure.query;

import io.d4tzz.ecommerce.payment.query.PaymentQueryService;
import io.d4tzz.ecommerce.payment.query.PaymentSearchCriteria;
import io.d4tzz.ecommerce.payment.query.PaymentView;
import io.d4tzz.ecommerce.query.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
public class JdbcPaymentQueryService implements PaymentQueryService {

    private static final String SELECT_PAYMENT = """
        SELECT p.id, p.order_reference, o.id AS order_id, p.type, p.method, p.shop_account, p.amount, p.transaction_no, p.refund_of,
               p.note, p.created_at,
               CASE WHEN p.type = 'PAYMENT'
                    THEN (SELECT COALESCE(SUM(r.amount), 0) FROM payments r WHERE r.refund_of = p.id)
               END AS refunded_amount
        """;

    private static final String FROM_PAYMENT = """
        FROM payments p
        LEFT JOIN orders o ON o.order_code = p.order_reference
        """;

    private final JdbcClient jdbcClient;

    public JdbcPaymentQueryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PageResponse<PaymentView> searchPayments(PaymentSearchCriteria criteria) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (criteria.orderReference() != null && !criteria.orderReference().isBlank()) {
            where.add("p.order_reference = :orderReference");
            params.put("orderReference", criteria.orderReference().trim().toUpperCase());
        }
        if (criteria.type() != null && !criteria.type().isBlank()) {
            where.add("p.type = :type");
            params.put("type", criteria.type().trim().toUpperCase());
        }
        if (criteria.method() != null && !criteria.method().isBlank()) {
            where.add("p.method = :method");
            params.put("method", criteria.method().trim().toUpperCase());
        }
        if (criteria.shopAccount() != null && !criteria.shopAccount().isBlank()) {
            where.add("p.shop_account = :shopAccount");
            params.put("shopAccount", criteria.shopAccount().trim().toUpperCase());
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            where.add("(p.transaction_no ILIKE :kwLike OR p.order_reference ILIKE :kwLike)");
            params.put("kwLike", "%" + criteria.keyword().trim() + "%");
        }
        if (Boolean.TRUE.equals(criteria.unmatched())) {
            where.add("p.type = 'PAYMENT' AND p.order_reference IS NULL");
        }
        // OffsetDateTime because the Postgres driver does not bind Instant
        if (criteria.from() != null) {
            where.add("p.created_at >= :from");
            params.put("from", OffsetDateTime.ofInstant(criteria.from(), ZoneOffset.UTC));
        }
        if (criteria.to() != null) {
            where.add("p.created_at < :to");
            params.put("to", OffsetDateTime.ofInstant(criteria.to(), ZoneOffset.UTC));
        }

        String from = FROM_PAYMENT + (where.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", where) + "\n");

        var countSpec = jdbcClient.sql("SELECT COUNT(*)\n" + from);
        params.forEach(countSpec::param);
        Long total = countSpec.query(Long.class).single();
        if (total == null || total == 0) {
            return PageResponse.empty(criteria.getPage(), criteria.getSize());
        }

        var spec = jdbcClient.sql(SELECT_PAYMENT + from + """
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :limit OFFSET :offset
            """);
        params.forEach(spec::param);
        List<PaymentView> content = spec
            .param("limit", criteria.getSize())
            .param("offset", criteria.getOffset())
            .query((rs, rowNum) -> toView(rs))
            .list();

        return PageResponse.of(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public Optional<PaymentView> getPayment(Long id) {
        return jdbcClient
            .sql(SELECT_PAYMENT + FROM_PAYMENT + "WHERE p.id = :id")
            .param("id", id)
            .query((rs, rowNum) -> toView(rs))
            .optional();
    }

    private PaymentView toView(ResultSet rs) throws SQLException {
        return new PaymentView(
            rs.getLong("id"),
            rs.getString("order_reference"),
            rs.getObject("order_id", Long.class),
            rs.getString("type"),
            rs.getString("method"),
            rs.getString("shop_account"),
            rs.getBigDecimal("amount"),
            rs.getBigDecimal("refunded_amount"),
            rs.getString("transaction_no"),
            rs.getObject("refund_of", Long.class),
            rs.getString("note"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
