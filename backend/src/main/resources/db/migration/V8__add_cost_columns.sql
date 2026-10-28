ALTER TABLE ai_benchmark_responses ADD COLUMN provider_reported_cost_usd DECIMAL(18, 10);
ALTER TABLE ai_benchmark_responses ADD COLUMN calculated_cost_usd DECIMAL(18, 10);
ALTER TABLE ai_benchmark_responses ADD COLUMN effective_cost_usd DECIMAL(18, 10);
ALTER TABLE ai_benchmark_responses ADD COLUMN cost_source VARCHAR(20);

