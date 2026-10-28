package com.novelcopilot.backend.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "SUPABASE_URL=http://localhost:8000",
    "SUPABASE_ANON_KEY=fake-key",
    "spring.datasource.url=jdbc:h2:mem:migrationtestdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "DB_USER=test",
    "DB_PASSWORD=test",
    "GEMINI_API_KEY=fake-gemini",
    "GROQ_API_KEY=fake-groq",
    "spring.flyway.enabled=false"
})
public class AiUsageMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testAiUsageMigration() {
        // Run flyway manually on the datasource to ensure the script works
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        assertDoesNotThrow(() -> flyway.migrate());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Verify table exists by counting
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM ai_usage_records", Integer.class);
        assertEquals(0, count);
        
        // Test insert works (simulating that the table structure is right and accepts small decimals)
        jdbcTemplate.update("INSERT INTO ai_usage_records (action_type, provider, model, status, created_at, estimated_cost_usd) " +
                "VALUES ('WRITE', 'groq', 'llama', 'SUCCESS', CURRENT_TIMESTAMP, 0.00000015)");
                
        Integer newCount = jdbcTemplate.queryForObject("SELECT count(*) FROM ai_usage_records", Integer.class);
        assertEquals(1, newCount);
    }

    @Test
    void backfillsMissingCreditTransactionDates() throws Exception {
        String databaseUrl = "jdbc:h2:mem:credit-timestamp-"
                + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("23"))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                databaseUrl,
                "sa",
                ""
        ); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO credit_transaction (
                        user_id,
                        idempotency_key,
                        type,
                        status,
                        credits,
                        balance_before,
                        balance_after,
                        reserved_before,
                        reserved_after,
                        created_at,
                        updated_at
                    ) VALUES (
                        'legacy-user',
                        'legacy-capture',
                        'CAPTURE',
                        'COMPLETED',
                        2,
                        97,
                        97,
                        3,
                        0,
                        NULL,
                        '2026-07-30 14:09:30'
                    )
                    """);
        }

        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                databaseUrl,
                "sa",
                ""
        ); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT created_at, updated_at
                     FROM credit_transaction
                     WHERE idempotency_key = 'legacy-capture'
                     """)) {
            result.next();
            LocalDateTime createdAt = result
                    .getTimestamp("created_at")
                    .toLocalDateTime();
            LocalDateTime updatedAt = result
                    .getTimestamp("updated_at")
                    .toLocalDateTime();

            assertNotNull(createdAt);
            assertEquals(
                    LocalDateTime.of(2026, 7, 30, 14, 9, 30),
                    createdAt
            );
            assertEquals(createdAt, updatedAt);
        }
    }
}
