CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255),
    display_name VARCHAR(255),
    avatar_url VARCHAR(255),
    auth_provider VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS stories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255),
    title VARCHAR(255),
    genre VARCHAR(255),
    tone VARCHAR(255),
    summary TEXT,
    main_idea TEXT,
    status VARCHAR(255),
    cover_image_path VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    deleted_at DATETIME,
    CONSTRAINT fk_stories_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_stories_user_deleted
    ON stories(user_id, deleted_at);

CREATE TABLE IF NOT EXISTS chapters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    story_id INT,
    title VARCHAR(255),
    chapter_order INT,
    type VARCHAR(255),
    goal VARCHAR(255),
    summary TEXT,
    content TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_chapters_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

CREATE INDEX idx_chapters_story_order
    ON chapters(story_id, chapter_order);

CREATE TABLE IF NOT EXISTS characters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    story_id INT,
    name VARCHAR(255),
    role VARCHAR(255),
    description TEXT,
    personality TEXT,
    goal VARCHAR(255),
    conflict TEXT,
    relationships TEXT,
    notes TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    importance VARCHAR(255),
    narrative_role VARCHAR(255),
    appearance VARCHAR(255),
    backstory VARCHAR(255),
    image_url VARCHAR(255),
    image_prompt VARCHAR(255),
    CONSTRAINT fk_characters_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

CREATE INDEX idx_characters_story
    ON characters(story_id);

CREATE TABLE IF NOT EXISTS world_notes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    story_id INT,
    title VARCHAR(255),
    category VARCHAR(255),
    content TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    importance VARCHAR(255),
    status VARCHAR(255),
    tags VARCHAR(255),
    CONSTRAINT fk_world_notes_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

CREATE INDEX idx_world_notes_story
    ON world_notes(story_id);

CREATE TABLE IF NOT EXISTS story_references (
    id INT AUTO_INCREMENT PRIMARY KEY,
    story_id INT,
    title VARCHAR(255),
    reference_type VARCHAR(255),
    what_i_like TEXT,
    inspiration_points TEXT,
    avoid_copying TEXT,
    notes TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_story_references_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

CREATE INDEX idx_story_references_story
    ON story_references(story_id);

CREATE TABLE IF NOT EXISTS ai_generations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    story_id INT,
    chapter_id INT,
    action_type VARCHAR(255),
    user_input TEXT,
    context_snapshot TEXT,
    ai_output TEXT,
    status VARCHAR(255),
    created_at DATETIME,
    CONSTRAINT fk_ai_generations_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_generations_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE SET NULL
);

CREATE INDEX idx_ai_generations_story
    ON ai_generations(story_id);

CREATE INDEX idx_ai_generations_chapter
    ON ai_generations(chapter_id);
