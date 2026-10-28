-- Inserir preço ativo para Llama 3.2 3B da Cloudflare
INSERT INTO model_pricing (provider, model, input_price_usd_per_million, cached_input_price_usd_per_million, output_price_usd_per_million, active, effective_from)
VALUES ('cloudflare', '@cf/meta/llama-3.2-3b-instruct', 0.051, 0.051, 0.335, true, CURRENT_TIMESTAMP);

-- Backfill dos registros anteriores de resumo que não possuem custo calculado
UPDATE ai_usage_records
SET estimated_cost_usd =
    (GREATEST(0, COALESCE(input_tokens, 0) - COALESCE(cached_input_tokens, 0)) * (0.051 / 1000000.0))
    + (COALESCE(cached_input_tokens, 0) * (0.051 / 1000000.0))
    + (COALESCE(output_tokens, 0) * (0.335 / 1000000.0))
WHERE action_type = 'SUMMARIZE_CHAPTER'
  AND LOWER(provider) = 'cloudflare'
  AND model = '@cf/meta/llama-3.2-3b-instruct'
  AND status = 'SUCCESS'
  AND (estimated_cost_usd IS NULL OR estimated_cost_usd = 0);
