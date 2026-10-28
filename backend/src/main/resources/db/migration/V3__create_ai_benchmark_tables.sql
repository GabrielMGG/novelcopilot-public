CREATE TABLE ai_benchmark_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    suite_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE ai_benchmark_responses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    case_id VARCHAR(255) NOT NULL,
    anonymous_label VARCHAR(10) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(150) NOT NULL,
    content TEXT NULL,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    cached_input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost_usd DECIMAL(18, 10) NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    error_code VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE ai_benchmark_evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    response_id BIGINT NOT NULL,
    evaluator_id VARCHAR(255) NOT NULL,
    literary_quality INT NOT NULL,
    context_coherence INT NOT NULL,
    instruction_following INT NOT NULL,
    language_naturalness INT NOT NULL,
    repetition_control INT NOT NULL,
    style_preservation INT NOT NULL,
    overall_score DECIMAL(5, 2) NOT NULL,
    notes TEXT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_benchmark_run_created ON ai_benchmark_runs (created_at);
CREATE INDEX idx_benchmark_resp_run ON ai_benchmark_responses (run_id);
CREATE INDEX idx_benchmark_eval_resp ON ai_benchmark_evaluations (response_id);
