CREATE TABLE IF NOT EXISTS ai_usage_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NULL,
    story_id INT NULL,
    chapter_id INT NULL,
    action_type VARCHAR(100) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(150) NOT NULL,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    cached_input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost_usd DECIMAL(18, 10) NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);
