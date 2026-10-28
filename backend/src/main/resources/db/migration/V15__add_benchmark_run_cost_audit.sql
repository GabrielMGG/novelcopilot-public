ALTER TABLE ai_benchmark_runs
    ADD COLUMN estimated_cost_usd DECIMAL(18, 10) NULL;

ALTER TABLE ai_benchmark_runs
    ADD COLUMN final_cost_usd DECIMAL(18, 10) NULL;
