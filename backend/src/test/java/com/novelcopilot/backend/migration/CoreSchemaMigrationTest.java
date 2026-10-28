package com.novelcopilot.backend.migration;

import com.novelcopilot.backend.entity.AiGeneration;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.entity.Story;
import com.novelcopilot.backend.entity.StoryCharacter;
import com.novelcopilot.backend.entity.StoryReference;
import com.novelcopilot.backend.entity.UserProfile;
import com.novelcopilot.backend.entity.WorldNote;
import com.novelcopilot.backend.repository.AiGenerationRepository;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.repository.StoryCharacterRepository;
import com.novelcopilot.backend.repository.StoryReferenceRepository;
import com.novelcopilot.backend.repository.StoryRepository;
import com.novelcopilot.backend.repository.UserProfileRepository;
import com.novelcopilot.backend.repository.WorldNoteRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class CoreSchemaMigrationTest {

    private static final List<String> CORE_TABLES = List.of(
            "USERS",
            "STORIES",
            "CHAPTERS",
            "CHARACTERS",
            "WORLD_NOTES",
            "STORY_REFERENCES",
            "AI_GENERATIONS");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private StoryCharacterRepository characterRepository;

    @Autowired
    private WorldNoteRepository worldNoteRepository;

    @Autowired
    private StoryReferenceRepository referenceRepository;

    @Autowired
    private AiGenerationRepository generationRepository;

    @Test
    void emptyDatabaseContainsCoreTablesForeignKeysAndIndexes() throws Exception {
        assertEquals(1, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM "flyway_schema_history"
                WHERE "version" = '17' AND "success" = TRUE
                """,
                Integer.class));

        try (Connection connection = dataSource.getConnection()) {
            for (String table : CORE_TABLES) {
                assertTrue(tableExists(connection, table), "Missing table " + table);
            }

            assertForeignKey(connection, "STORIES", "USER_ID");
            assertForeignKey(connection, "CHAPTERS", "STORY_ID");
            assertForeignKey(connection, "CHARACTERS", "STORY_ID");
            assertForeignKey(connection, "WORLD_NOTES", "STORY_ID");
            assertForeignKey(connection, "STORY_REFERENCES", "STORY_ID");
            assertForeignKey(connection, "AI_GENERATIONS", "STORY_ID");
            assertForeignKey(connection, "AI_GENERATIONS", "CHAPTER_ID");

            assertIndex(connection, "STORIES", "IDX_STORIES_USER_DELETED");
            assertIndex(connection, "CHAPTERS", "IDX_CHAPTERS_STORY_ORDER");
            assertIndex(connection, "CHARACTERS", "IDX_CHARACTERS_STORY");
            assertIndex(connection, "WORLD_NOTES", "IDX_WORLD_NOTES_STORY");
            assertIndex(connection, "STORY_REFERENCES", "IDX_STORY_REFERENCES_STORY");
            assertIndex(connection, "AI_GENERATIONS", "IDX_AI_GENERATIONS_STORY");
            assertIndex(connection, "AI_GENERATIONS", "IDX_AI_GENERATIONS_CHAPTER");
            assertColumn(connection, "CHAPTERS", "VERSION");
        }
    }

    @Test
    @Transactional
    void repositoriesPersistCompleteCoreNovelGraph() {
        LocalDateTime now = LocalDateTime.now();
        String userId = "550e8400-e29b-41d4-a716-446655440010";

        UserProfile user = new UserProfile();
        user.setId(userId);
        user.setEmail("schema@example.com");
        user.setCreatedAt(now);
        userProfileRepository.save(user);

        Story story = new Story();
        story.setUserId(userId);
        story.setTitle("Schema story");
        story.setCreatedAt(now);
        story = storyRepository.save(story);
        assertNotNull(story.getId());

        Chapter chapter = new Chapter();
        chapter.setStoryId(story.getId());
        chapter.setTitle("Chapter 1");
        chapter.setChapterOrder(1);
        chapter.setCreatedAt(now);
        chapter = chapterRepository.save(chapter);
        assertNotNull(chapter.getId());
        assertNotNull(chapter.getVersion());

        StoryCharacter character = new StoryCharacter();
        character.setStoryId(story.getId());
        character.setName("Character");
        character.setCreatedAt(now);
        characterRepository.save(character);

        WorldNote worldNote = new WorldNote();
        worldNote.setStoryId(story.getId());
        worldNote.setTitle("World");
        worldNote.setCreatedAt(now);
        worldNoteRepository.save(worldNote);

        StoryReference reference = new StoryReference();
        reference.setStoryId(story.getId());
        reference.setTitle("Reference");
        reference.setCreatedAt(now);
        referenceRepository.save(reference);

        AiGeneration generation = new AiGeneration();
        generation.setStoryId(story.getId());
        generation.setChapterId(chapter.getId());
        generation.setActionType("CONTINUE_SCENE");
        generation.setCreatedAt(now);
        generationRepository.save(generation);

        assertEquals(1, chapterRepository.findByStoryId(story.getId()).size());
        assertEquals(1, characterRepository.findByStoryId(story.getId()).size());
        assertEquals(1, worldNoteRepository.findByStoryId(story.getId()).size());
        assertEquals(1, referenceRepository.findByStoryId(story.getId()).size());
        assertEquals(1, generationRepository.findByStoryId(story.getId()).size());
    }

    @Test
    void upgradesExistingCoreTablesWithoutLosingRows() throws Exception {
        String databaseUrl = "jdbc:h2:mem:core-upgrade-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("15"))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE users (
                        id VARCHAR(255) PRIMARY KEY,
                        email VARCHAR(255),
                        display_name VARCHAR(255),
                        avatar_url VARCHAR(255),
                        auth_provider VARCHAR(255),
                        created_at DATETIME,
                        updated_at DATETIME
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE stories (
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
                        deleted_at DATETIME
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO users (id, email)
                    VALUES ('legacy-user', 'legacy@example.com')
                    """);
            statement.executeUpdate("""
                    INSERT INTO stories (user_id, title)
                    VALUES ('legacy-user', 'Legacy story')
                    """);
        }

        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            try (ResultSet users = statement.executeQuery(
                    "SELECT email FROM users WHERE id = 'legacy-user'")) {
                assertTrue(users.next());
                assertEquals("legacy@example.com", users.getString("email"));
            }
            try (ResultSet stories = statement.executeQuery(
                    "SELECT title FROM stories WHERE user_id = 'legacy-user'")) {
                assertTrue(stories.next());
                assertEquals("Legacy story", stories.getString("title"));
            }
            assertTrue(tableExists(connection, "CHAPTERS"));
            assertTrue(tableExists(connection, "AI_GENERATIONS"));
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private void assertForeignKey(
            Connection connection,
            String tableName,
            String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet keys = metadata.getImportedKeys(null, null, tableName)) {
            while (keys.next()) {
                if (columnName.equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                    return;
                }
            }
        }
        throw new AssertionError("Missing foreign key " + tableName + "." + columnName);
    }

    private void assertIndex(
            Connection connection,
            String tableName,
            String indexName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(
                null, null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }
        throw new AssertionError("Missing index " + indexName);
    }

    private void assertColumn(
            Connection connection,
            String tableName,
            String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                null, null, tableName, columnName)) {
            assertTrue(
                    columns.next(),
                    "Missing column " + tableName + "." + columnName
            );
        }
    }
}
