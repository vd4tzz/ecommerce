package io.d4tzz.ecommerce.user.infrastructure.query;

import io.d4tzz.ecommerce.user.query.UserQueryService;
import io.d4tzz.ecommerce.user.query.UserSearchCriteria;
import io.d4tzz.ecommerce.user.query.UserView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class JdbcUserQueryService implements UserQueryService {

    private final JdbcClient jdbcClient;

    public JdbcUserQueryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<UserView> findById(Long id) {
        return jdbcClient
            .sql(BASE_SELECT + " WHERE u.id = :id")
            .param("id", id)
            .query(this::mapRow)
            .optional();
    }

    @Override
    public List<UserView> search(UserSearchCriteria criteria) {
        var where = new ArrayList<String>();
        var sql = new StringBuilder(BASE_SELECT);

        applyFilters(criteria, where);
        if (!where.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", where));
        sql.append(" ORDER BY u.created_at DESC");
        sql.append(" LIMIT :size OFFSET :offset");

        var spec = jdbcClient.sql(sql.toString())
            .param("size", criteria.size())
            .param("offset", (long) criteria.page() * criteria.size());

        bindParams(criteria, spec);
        return spec.query(this::mapRow).list();
    }

    @Override
    public long count(UserSearchCriteria criteria) {
        var where = new ArrayList<String>();
        var sql = new StringBuilder("SELECT COUNT(*) FROM users u");

        applyFilters(criteria, where);
        if (!where.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", where));

        var spec = jdbcClient.sql(sql.toString());
        bindParams(criteria, spec);
        return spec.query(Long.class).single();
    }

    private void applyFilters(UserSearchCriteria criteria, List<String> where) {
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            where.add("(u.email ILIKE :keyword OR u.full_name ILIKE :keyword OR u.phone_number ILIKE :keyword)");
        }
        if (criteria.role() != null) where.add("u.role = :role");
        if (criteria.status() != null) where.add("u.status = :status");
        if (criteria.emailVerified() != null) where.add("u.email_verified = :emailVerified");
    }

    private void bindParams(UserSearchCriteria criteria, JdbcClient.StatementSpec spec) {
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            spec.param("keyword", "%" + criteria.keyword() + "%");
        }
        if (criteria.role() != null) spec.param("role", criteria.role());
        if (criteria.status() != null) spec.param("status", criteria.status());
        if (criteria.emailVerified() != null) spec.param("emailVerified", criteria.emailVerified());
    }

    private UserView mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        UserView.Address address = null;
        if (rs.getString("addr_province") != null) {
            address = new UserView.Address(
                rs.getString("addr_recipient_name"),
                rs.getString("addr_phone"),
                rs.getString("addr_province"),
                rs.getString("addr_district"),
                rs.getString("addr_ward"),
                rs.getString("addr_detail")
            );
        }
        return new UserView(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("full_name"),
            rs.getString("phone_number"),
            rs.getString("avatar_url"),
            rs.getString("role"),
            rs.getString("status"),
            rs.getBoolean("email_verified"),
            address,
            rs.getTimestamp("last_login_at") != null ? rs.getTimestamp("last_login_at").toInstant() : null,
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private static final String BASE_SELECT = """
        SELECT
            u.id, u.email, u.full_name, u.phone_number, u.avatar_url,
            u.role, u.status, u.email_verified, u.last_login_at, u.created_at,
            a.recipient_name AS addr_recipient_name, a.phone AS addr_phone,
            a.province      AS addr_province,        a.district AS addr_district,
            a.ward          AS addr_ward,             a.detail   AS addr_detail
        FROM users u
        LEFT JOIN addresses a ON a.user_id = u.id
        """;
}
