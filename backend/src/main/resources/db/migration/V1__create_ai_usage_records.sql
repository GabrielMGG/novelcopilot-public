CREATE TABLE ai_usage_records (
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

CREATE INDEX idx_ai_usage_user_created ON ai_usage_records (user_id, created_at);
CREATE INDEX idx_ai_usage_model_created ON ai_usage_records (provider, model, created_at);
CREATE INDEX idx_ai_usage_action_created ON ai_usage_records (action_type, created_at);
CREATE INDEX idx_ai_usage_status ON ai_usage_records (status);
CREATE INDEX idx_ai_usage_created_at ON ai_usage_records (created_at);
