UPDATE model_pricing
SET active = false,
    effective_until = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'cloudflare'
  AND model = '@cf/google/gemma-4-26b-a4b-it'
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
    '@cf/meta/llama-3.3-70b-instruct-fp8-fast',
    0.29,
    0.29,
    2.25,
    CURRENT_TIMESTAMP,
    true
);
