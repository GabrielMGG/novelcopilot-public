UPDATE model_pricing
SET active = false,
    effective_until = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'groq'
  AND model = 'llama-3.3-70b-versatile'
  AND active = true;

INSERT INTO model_pricing (
    provider,
    model,
    input_price_usd_per_million,
    cached_input_price_usd_per_million,
    output_price_usd_per_million,
    effective_from,
    active
)
VALUES (
    'groq',
    'openai/gpt-oss-20b',
    0.075,
    0.037,
    0.30,
    CURRENT_TIMESTAMP,
    true
);
