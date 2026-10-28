package com.novelcopilot.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "novelcopilot.ai.rate-limit")
public class AiRateLimitProperties {

    private boolean enabled = true;
    private int generalLimit = 10;
    private int generalWindowSeconds = 60;
    private int expensiveLimit = 3;
    private int expensiveWindowSeconds = 300;
    private int benchmarkRunLimit = 1;
    private int benchmarkRunWindowSeconds = 3600;
    private int benchmarkRetryLimit = 2;
    private int benchmarkRetryWindowSeconds = 3600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getGeneralLimit() {
        return generalLimit;
    }

    public void setGeneralLimit(int generalLimit) {
        this.generalLimit = generalLimit;
    }

    public int getGeneralWindowSeconds() {
        return generalWindowSeconds;
    }

    public void setGeneralWindowSeconds(int generalWindowSeconds) {
        this.generalWindowSeconds = generalWindowSeconds;
    }

    public int getExpensiveLimit() {
        return expensiveLimit;
    }

    public void setExpensiveLimit(int expensiveLimit) {
        this.expensiveLimit = expensiveLimit;
    }

    public int getExpensiveWindowSeconds() {
        return expensiveWindowSeconds;
    }

    public void setExpensiveWindowSeconds(int expensiveWindowSeconds) {
        this.expensiveWindowSeconds = expensiveWindowSeconds;
    }

    public int getBenchmarkRunLimit() {
        return benchmarkRunLimit;
    }

    public void setBenchmarkRunLimit(int benchmarkRunLimit) {
        this.benchmarkRunLimit = benchmarkRunLimit;
    }

    public int getBenchmarkRunWindowSeconds() {
        return benchmarkRunWindowSeconds;
    }

    public void setBenchmarkRunWindowSeconds(int benchmarkRunWindowSeconds) {
        this.benchmarkRunWindowSeconds = benchmarkRunWindowSeconds;
    }

    public int getBenchmarkRetryLimit() {
        return benchmarkRetryLimit;
    }

    public void setBenchmarkRetryLimit(int benchmarkRetryLimit) {
        this.benchmarkRetryLimit = benchmarkRetryLimit;
    }

    public int getBenchmarkRetryWindowSeconds() {
        return benchmarkRetryWindowSeconds;
    }

    public void setBenchmarkRetryWindowSeconds(int benchmarkRetryWindowSeconds) {
        this.benchmarkRetryWindowSeconds = benchmarkRetryWindowSeconds;
    }
}
