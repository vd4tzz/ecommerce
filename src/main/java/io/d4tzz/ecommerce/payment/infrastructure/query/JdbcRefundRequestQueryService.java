package io.d4tzz.ecommerce.payment.infrastructure.query;

import io.d4tzz.ecommerce.payment.query.RefundRequestQueryService;
import io.d4tzz.ecommerce.payment.query.RefundRequestSearchCriteria;
import io.d4tzz.ecommerce.payment.query.RefundRequestView;
import io.d4tzz.ecommerce.common.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
public class JdbcRefundRequestQueryService implements RefundRequestQueryService {

    private static final String SELECT_REQUEST = """
        SELECT r.id, r.payment_id, p.order_reference, p.method AS payment_method,
               p.shop_account AS payment_shop_account, p.transaction_no AS payment_transaction_no,
               r.amount, r.reason, r.status, r.refund_id, r.created_at, r.completed_at
        """;

    private static final String FROM_REQUEST = """
        FROM refund_requests r
        JOIN payments p ON p.id = r.payment_id
        """;

    private final JdbcClient jdbcClient;

    public JdbcRefundRequestQueryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PageResponse<RefundRequestView> searchRefundRequests(RefundRequestSearchCriteria criteria) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (criteria.status() != null && !criteria.status().isBlank()) {
            where.add("r.status = :status");
            params.put("status", criteria.status().trim().toUpperCase());
        }
        if (criteria.orderReference() != null && !criteria.orderReference().isBlank()) {
            where.add("p.order_reference = :orderReference");
            params.put("orderReference", criteria.orderReference().trim().toUpperCase());
        }
        String from = FROM_REQUEST + (where.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", where) + "\n");

        var countSpec = jdbcClient.sql("SELECT COUNT(*)\n" + from);
        params.forEach(countSpec::param);
        Long total = countSpec.query(Long.class).single();
        if (total == null || total == 0) {
            return PageResponse.empty(criteria.getPage(), criteria.getSize());
        }

        var spec = jdbcClient.sql(SELECT_REQUEST + from + """
            ORDER BY r.created_at ASC, r.id ASC
            LIMIT :limit OFFSET :offset
            """);
        params.forEach(spec::param);
        List<RefundRequestView> content = spec
            .param("limit", criteria.getSize())
            .param("offset", criteria.getOffset())
            .query((rs, rowNum) -> toView(rs))
            .list();

        return PageResponse.of(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public Optional<RefundRequestView> getRefundRequest(Long id) {
        return jdbcClient
            .sql(SELECT_REQUEST + FROM_REQUEST + "WHERE r.id = :id")
            .param("id", id)
            .query((rs, rowNum) -> toView(rs))
            .optional();
    }

    private RefundRequestView toView(ResultSet rs) throws SQLException {
        Timestamp completedAt = rs.getTimestamp("completed_at");
        return new RefundRequestView(
            rs.getLong("id"),
            rs.getLong("payment_id"),
            rs.getString("order_reference"),
            rs.getString("payment_method"),
            rs.getString("payment_shop_account"),
            rs.getString("payment_transaction_no"),
            rs.getBigDecimal("amount"),
            rs.getString("reason"),
            rs.getString("status"),
            rs.getObject("refund_id", Long.class),
            rs.getTimestamp("created_at").toInstant(),
            completedAt != null ? completedAt.toInstant() : null
        );
    }
}
