package io.d4tzz.ecommerce.support;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base for persistence tests: runs them against a throwaway PostgreSQL container, never the developer's database.
 *
 * <p>The container is started once and shared by all subclasses until the JVM exits. It is deliberately not a
 * {@code @Container}: that would stop it after every test class, while Spring caches the test context (and its
 * connection pool) for the whole JVM, so a later class would reuse a context pointing at a stopped container.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class PostgresContainerTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }
}
