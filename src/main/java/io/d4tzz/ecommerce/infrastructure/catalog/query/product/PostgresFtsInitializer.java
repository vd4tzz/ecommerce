package io.d4tzz.ecommerce.infrastructure.catalog.query.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Tự động khởi tạo Extension unaccent, hàm immutable_unaccent và GIN Index
 * khi ứng dụng khởi động để hỗ trợ PostgreSQL Full-Text Search.
 */
@Component
public class PostgresFtsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresFtsInitializer.class);

    private final JdbcClient jdbcClient;

    public PostgresFtsInitializer(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcClient.sql("CREATE EXTENSION IF NOT EXISTS unaccent").update();
            jdbcClient.sql("CREATE EXTENSION IF NOT EXISTS pg_trgm").update();

            jdbcClient.sql("""
                CREATE OR REPLACE FUNCTION immutable_unaccent(text)
                RETURNS text AS $$
                SELECT public.unaccent($1);
                $$ LANGUAGE sql IMMUTABLE PARALLEL SAFE;
            """).update();

            jdbcClient.sql("""
                CREATE INDEX IF NOT EXISTS idx_products_fts ON products
                USING gin(to_tsvector('simple', immutable_unaccent(name)));
            """).update();

            jdbcClient.sql("""
                CREATE INDEX IF NOT EXISTS idx_products_name_trgm ON products
                USING gin(immutable_unaccent(name) gin_trgm_ops);
            """).update();

            jdbcClient.sql("""
                CREATE INDEX IF NOT EXISTS idx_products_attributes ON products
                USING gin (attributes);
            """).update();

            jdbcClient.sql("""
                DO $$
                BEGIN
                    EXECUTE format('ALTER DATABASE %I SET pg_trgm.word_similarity_threshold = 0.4', current_database());
                END $$;
            """).update();

            log.info("PostgreSQL FTS, pg_trgm & JSONB attributes indexes initialized successfully.");
        } catch (Exception e) {
            log.warn("Could not auto-initialize PostgreSQL FTS/trgm index: {}", e.getMessage());
        }
    }
}
