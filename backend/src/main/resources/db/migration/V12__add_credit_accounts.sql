CREATE TABLE user_credit_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    available_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    reserved_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    lifetime_granted_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    lifetime_used_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    current_quota_profile_code VARCHAR(50),
    current_cycle_start TIMESTAMP,
    current_cycle_end TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credit_cycle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    quota_profile_code VARCHAR(50) NOT NULL,
    cycle_start TIMESTAMP NOT NULL,
    cycle_end TIMESTAMP,
    granted_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    used_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    reserved_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    expired_credits DECIMAL(12, 4) NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_credit_cycle_idempotency UNIQUE (user_id, cycle_start, quota_profile_code)
);

CREATE TABLE credit_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    cycle_id BIGINT REFERENCES credit_cycle(id),
    generation_usage_id BIGINT,
    idempotency_key VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    credits DECIMAL(12, 4) NOT NULL,
    balance_before DECIMAL(12, 4) NOT NULL,
    balance_after DECIMAL(12, 4) NOT NULL,
    reserved_before DECIMAL(12, 4) NOT NULL,
    reserved_after DECIMAL(12, 4) NOT NULL,
    reason VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_transaction_idempotency UNIQUE (user_id, idempotency_key)
);

-- Track reservation and consumption of the technical AI quota.
ALTER TABLE ai_generation_usage ADD COLUMN estimated_credits DECIMAL(12, 4);
ALTER TABLE ai_generation_usage ADD COLUMN reserved_credits DECIMAL(12, 4);
ALTER TABLE ai_generation_usage ADD COLUMN charged_credits DECIMAL(12, 4);
ALTER TABLE ai_generation_usage ADD COLUMN released_credits DECIMAL(12, 4);
ALTER TABLE ai_generation_usage ADD COLUMN credit_usage_status VARCHAR(50) DEFAULT 'NOT_REQUIRED';
ALTER TABLE ai_generation_usage ADD COLUMN credit_transaction_id BIGINT REFERENCES credit_transaction(id);
