UPDATE credit_transaction
SET created_at = COALESCE(
    (
        SELECT ai_generation_usage.created_at
        FROM ai_generation_usage
        WHERE ai_generation_usage.id = credit_transaction.generation_usage_id
    ),
    updated_at,
    CURRENT_TIMESTAMP
)
WHERE created_at IS NULL;

UPDATE credit_transaction
SET updated_at = created_at
WHERE updated_at IS NULL;
