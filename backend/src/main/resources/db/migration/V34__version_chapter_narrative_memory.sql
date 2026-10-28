ALTER TABLE chapters
    ADD COLUMN summary_content_hash VARCHAR(64) NULL;

ALTER TABLE chapters
    ADD COLUMN summary_generated_at DATETIME NULL;

ALTER TABLE chapters
    ADD COLUMN summary_provider VARCHAR(50) NULL;

ALTER TABLE chapters
    ADD COLUMN summary_model VARCHAR(255) NULL;
