ALTER TABLE ai_benchmark_evaluations ADD COLUMN compatibility_status VARCHAR(100) NULL;
ALTER TABLE ai_benchmark_evaluations MODIFY COLUMN literary_quality INT NULL;
ALTER TABLE ai_benchmark_evaluations MODIFY COLUMN context_coherence INT NULL;
ALTER TABLE ai_benchmark_evaluations MODIFY COLUMN instruction_following INT NULL;
ALTER TABLE ai_benchmark_evaluations MODIFY COLUMN language_naturalness INT NULL;
ALTER TABLE ai_benchmark_evaluations MODIFY COLUMN repetition_control INT NULL;
ALTER TABLE ai_benchmark_evaluations MODIFY COLUMN style_preservation INT NULL;
ALTER TABLE ai_benchmark_evaluations MODIFY COLUMN overall_score DECIMAL(5,2) NULL;
