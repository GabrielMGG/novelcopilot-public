UPDATE model_pricing
SET active = false,
    effective_until = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'cloudflare'
  AND model = '@cf/ibm-granite/granite-4.0-h-micro'
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
    '@cf/meta/llama-4-scout-17b-16e-instruct',
    0.270,
    0.270,
    0.850,
    CURRENT_TIMESTAMP,
    true
);
