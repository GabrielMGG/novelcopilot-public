UPDATE model_pricing
SET active = true,
    effective_until = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE LOWER(provider) = 'cloudflare'
  AND model = '@cf/google/gemma-4-26b-a4b-it';
