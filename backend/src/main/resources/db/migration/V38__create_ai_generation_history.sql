CREATE TABLE ai_generation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usage_id BIGINT NOT NULL,
    user_id CHAR(36) NOT NULL,
    story_id INT NOT NULL,
    chapter_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    output_type VARCHAR(20) NOT NULL,
    content MEDIUMTEXT,
    structured_output JSON,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_ai_generation_history_usage UNIQUE (usage_id),
    CONSTRAINT fk_ai_generation_history_usage
        FOREIGN KEY (usage_id) REFERENCES ai_generation_usage(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_generation_history_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_generation_history_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_generation_history_chapter_created
    ON ai_generation_history(user_id, story_id, chapter_id, created_at DESC, id DESC);
