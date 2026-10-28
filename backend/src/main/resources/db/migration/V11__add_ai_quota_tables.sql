CREATE TABLE quota_profile (
    code VARCHAR(50) PRIMARY KEY,
    description TEXT,
    monthly_credits DECIMAL(12, 4) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE model_pricing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    input_price_usd_per_million DECIMAL(12, 4) NOT NULL,
    cached_input_price_usd_per_million DECIMAL(12, 4) NOT NULL,
    output_price_usd_per_million DECIMAL(12, 4) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_until TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO quota_profile (code, description, monthly_credits, active)
VALUES ('DEMO', 'Default development quota', 500.0, true);

INSERT INTO model_pricing (
    provider,
    model,
    input_price_usd_per_million,
    cached_input_price_usd_per_million,
    output_price_usd_per_million,
    effective_from,
    active
)
VALUES ('groq', 'llama-3.3-70b-versatile', 0.59, 0.59, 0.79, CURRENT_TIMESTAMP, true);
