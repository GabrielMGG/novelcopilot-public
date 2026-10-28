UPDATE model_pricing
SET active = false,
    effective_until = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'groq'
  AND model = 'openai/gpt-oss-120b'
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
    'cloudflare',
    '@cf/google/gemma-4-26b-a4b-it',
    0.10,
    0.10,
    0.30,
    CURRENT_TIMESTAMP,
    true
);
