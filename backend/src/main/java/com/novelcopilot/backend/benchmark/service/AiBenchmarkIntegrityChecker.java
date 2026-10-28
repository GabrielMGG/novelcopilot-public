package com.novelcopilot.backend.benchmark.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("benchmark & !test")
@org.springframework.context.annotation.DependsOn("flyway")
public class AiBenchmarkIntegrityChecker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AiBenchmarkIntegrityChecker.class);
    private final JdbcTemplate jdbcTemplate;

    public AiBenchmarkIntegrityChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("Verifying MySQL integrity for benchmark profile...");

        String[] requiredTables = {
                "flyway_schema_history",
                "ai_usage_records",
                "ai_benchmark_runs",
                "ai_benchmark_responses",
                "ai_benchmark_evaluations"
        };

        for (String table : requiredTables) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                    Integer.class,
                    table
            );
            if (count == null || count == 0) {
                log.error("Diagnostico: Tabela obrigatoria ausente: {}", table);
                log.error("O benchmark esta bloqueado devido a integridade do banco de dados.");
                System.exit(1);
            }
        }

        List<String> requiredMigrations = List.of("1", "2", "3");
        for (String version : requiredMigrations) {
            Integer successCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = 1",
                    Integer.class,
                    version
            );
            if (successCount == null || successCount == 0) {
                log.error("Diagnostico: Migration V{} nao encontrada ou sem estado de sucesso.", version);
                log.error("O benchmark esta bloqueado devido a integridade das migrations.");
                System.exit(1);
            }
        }

        log.info("Verificacao de integridade concluida com sucesso. Todas as tabelas e migrations estao presentes.");
    }
}
