UPDATE model_pricing
SET active = false,
    effective_until = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'cloudflare'
  AND model = '@cf/zai-org/glm-4.7-flash'
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
    '@cf/qwen/qwen3-30b-a3b-fp8',
    0.051,
    0.051,
    0.335,
    CURRENT_TIMESTAMP,
    true
);
