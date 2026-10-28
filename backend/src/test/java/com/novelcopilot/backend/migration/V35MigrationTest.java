package com.novelcopilot.backend.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V35MigrationTest {

    @Test
    void upgradesV35AndRecoversSummaryCosts() throws Exception {
        String databaseUrl = "jdbc:h2:mem:v35-upgrade-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";

        // Migrate to V34
        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("34"))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
             Statement statement = connection.createStatement()) {

            // Create a dummy user to satisfy FK
            statement.executeUpdate("""
                    INSERT INTO users (id, email)
                    VALUES ('user1', 'user1@example.com')
                    """);

            // Insert records that should be backfilled
            statement.executeUpdate("""
                    INSERT INTO ai_usage_records (
                        user_id, action_type, provider, model, status,
                        input_tokens, cached_input_tokens, output_tokens, total_tokens, estimated_cost_usd, duration_ms, created_at
                    ) VALUES (
                        'user1', 'SUMMARIZE_CHAPTER', 'Cloudflare', '@cf/meta/llama-3.2-3b-instruct', 'SUCCESS',
                        807, 0, 262, 1069, 0, 3526, NOW()
                    )
                    """);

            // Insert another record with null cost
            statement.executeUpdate("""
                    INSERT INTO ai_usage_records (
                        user_id, action_type, provider, model, status,
                        input_tokens, cached_input_tokens, output_tokens, total_tokens, estimated_cost_usd, duration_ms, created_at
                    ) VALUES (
                        'user1', 'SUMMARIZE_CHAPTER', 'Cloudflare', '@cf/meta/llama-3.2-3b-instruct', 'SUCCESS',
                        1000, 0, 1000, 2000, NULL, 4000, NOW()
                    )
                    """);

            // Insert a record that should NOT be backfilled (different provider)
            statement.executeUpdate("""
                    INSERT INTO ai_usage_records (
                        user_id, action_type, provider, model, status,
                        input_tokens, cached_input_tokens, output_tokens, total_tokens, estimated_cost_usd, duration_ms, created_at
                    ) VALUES (
                        'user1', 'SUMMARIZE_CHAPTER', 'Groq', 'llama-3.3-70b-versatile', 'SUCCESS',
                        1000, 0, 1000, 2000, 0, 4000, NOW()
                    )
                    """);
        }

        // Migrate to V35
        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("35"))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
             Statement statement = connection.createStatement()) {

            // Assert the two required models remain explicitly active.
            try (ResultSet rs = statement.executeQuery(
                    """
                    SELECT COUNT(*) as count
                    FROM model_pricing
                    WHERE provider = 'cloudflare'
                      AND model IN (
                          '@cf/meta/llama-4-scout-17b-16e-instruct',
                          '@cf/meta/llama-3.2-3b-instruct'
                      )
                      AND active = true
                    """)) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt("count"), "Both Llama 4 Scout and Llama 3.2 3B should be active");
            }

            // Assert first recovered cost
            try (ResultSet rs = statement.executeQuery(
                    "SELECT estimated_cost_usd FROM ai_usage_records WHERE provider = 'Cloudflare' AND input_tokens = 807")) {
                assertTrue(rs.next());
                BigDecimal cost = rs.getBigDecimal("estimated_cost_usd");
                assertEquals(new BigDecimal("0.0001289270"), cost);
            }

            // Assert second recovered cost (from NULL)
            try (ResultSet rs = statement.executeQuery(
                    "SELECT estimated_cost_usd FROM ai_usage_records WHERE provider = 'Cloudflare' AND input_tokens = 1000")) {
                assertTrue(rs.next());
                BigDecimal cost = rs.getBigDecimal("estimated_cost_usd");
                // 1000 * (0.051/1000000) + 1000 * (0.335/1000000) = 0.000051 + 0.000335 = 0.000386
                assertEquals(new BigDecimal("0.0003860000").stripTrailingZeros(), cost.stripTrailingZeros());
            }

            // Assert untouched record
            try (ResultSet rs = statement.executeQuery(
                    "SELECT estimated_cost_usd FROM ai_usage_records WHERE provider = 'Groq'")) {
                assertTrue(rs.next());
                BigDecimal cost = rs.getBigDecimal("estimated_cost_usd");
                assertEquals(0, cost.compareTo(BigDecimal.ZERO)); // Remains 0
            }
        }
    }
}
