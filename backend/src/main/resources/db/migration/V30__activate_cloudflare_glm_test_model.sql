UPDATE model_pricing
SET active = false,
    effective_until = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'cloudflare'
  AND model = '@cf/meta/llama-3.3-70b-instruct-fp8-fast'
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
    '@cf/zai-org/glm-4.7-flash',
    0.06,
    0.06,
    0.40,
    CURRENT_TIMESTAMP,
    true
);
