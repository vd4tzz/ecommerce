package io.d4tzz.ecommerce.infrastructure.catalog.query.product;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class CategoryPathResolver {

    private final JdbcClient jdbcClient;

    public CategoryPathResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public String getCategoryPathPrefix(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        String sql = "SELECT CONCAT(COALESCE(path, ''), id, '.%') FROM categories WHERE id = :id";
        return jdbcClient
            .sql(sql)
            .param("id", categoryId)
            .query(String.class)
            .optional()
            .orElse(categoryId + ".%");
    }
}
