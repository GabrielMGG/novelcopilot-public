UPDATE model_pricing
SET active = false,
    effective_until = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'openrouter'
  AND model = 'deepseek/deepseek-v4-flash:free'
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
    'openai/gpt-oss-120b',
    0.15,
    0.075,
    0.60,
    CURRENT_TIMESTAMP,
    true
);
