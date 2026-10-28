CREATE TABLE ai_rate_limit_counters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    window_started_at TIMESTAMP NOT NULL,
    request_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_rate_limit_window UNIQUE (user_id, scope, window_started_at)
);

CREATE INDEX idx_ai_rate_limit_window_started_at
    ON ai_rate_limit_counters (window_started_at);
