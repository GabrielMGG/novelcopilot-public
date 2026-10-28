package com.novelcopilot.backend.benchmark.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource, Environment env) {
        String locations = env.getProperty("spring.flyway.locations", "classpath:db/migration");
        boolean baselineOnMigrate = env.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, true);
        
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(baselineOnMigrate)
                .locations(locations)
                .load();
                
        flyway.migrate();
        return flyway;
    }
}
