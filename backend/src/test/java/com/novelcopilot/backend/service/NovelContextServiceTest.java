package com.novelcopilot.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novelcopilot.backend.config.NovelAiConfigProperties;
import com.novelcopilot.backend.config.NovelAiConfigProperties.ContextProfilesConfig;
import com.novelcopilot.backend.config.NovelAiConfigProperties.ProfileBudget;
import com.novelcopilot.backend.dto.ContextProfile;
import com.novelcopilot.backend.dto.NovelAiAction;
import com.novelcopilot.backend.dto.NovelAiRequest;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.entity.Story;
import com.novelcopilot.backend.entity.StoryCharacter;
import com.novelcopilot.backend.entity.WorldNote;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.repository.StoryCharacterRepository;
import com.novelcopilot.backend.repository.StoryRepository;
import com.novelcopilot.backend.repository.WorldNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NovelContextServiceTest {

    @Mock private StoryRepository storyRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private StoryCharacterRepository characterRepository;
    @Mock private WorldNoteRepository worldNoteRepository;
    @Mock private NovelAiConfigProperties config;
    
    private TokenEstimator tokenEstimator;
    private NovelContextService contextService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        tokenEstimator = new TokenEstimator();
        contextService = new NovelContextService(
                storyRepository, chapterRepository, characterRepository, worldNoteRepository, config, tokenEstimator
        );
        mapper = new ObjectMapper();
        
        ContextProfilesConfig profiles = new ContextProfilesConfig();
        lenient().when(config.getContext()).thenReturn(profiles);
    }

    @Test
    void shouldResolveCorrectProfile() {
        assertEquals(ContextProfile.SELECTION_EDIT, contextService.resolveProfile(NovelAiAction.REWRITE_SELECTION));
        assertEquals(ContextProfile.INLINE_LOW_COST, contextService.resolveProfile(NovelAiAction.INLINE_CONTINUATION));
        assertEquals(ContextProfile.SCENE_CONTINUITY, contextService.resolveProfile(NovelAiAction.CONTINUE_SCENE));
        assertEquals(ContextProfile.CONTINUATION_SUGGESTIONS, contextService.resolveProfile(NovelAiAction.SUGGEST_CONTINUATION));
        assertEquals(ContextProfile.CHAPTER_FROM_BEATS, contextService.resolveProfile(NovelAiAction.WRITE_CHAPTER_FROM_BEATS));
        assertEquals(ContextProfile.CHAPTER_TITLE, contextService.resolveProfile(NovelAiAction.GENERATE_CHAPTER_TITLE));
        assertEquals(ContextProfile.CHAPTER_REVIEW, contextService.resolveProfile(NovelAiAction.REVIEW_CHAPTER));
    }

    @Test
    void shouldRejectInvalidStoryId() {
        NovelAiRequest req = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 999, null, null, null, null, null, null, null, null, null, null, false, null);
        when(storyRepository.findById(999)).thenReturn(Optional.empty());
        ObjectNode diag = mapper.createObjectNode();
        assertThrows(ResponseStatusException.class, () -> contextService.buildContext("user1", req, diag));
    }

    @Test
    void shouldRejectCrossTenantStory() {
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        story.setUserId("user1"); // Tenant 1
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));

        NovelAiRequest req = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, null, null, null, null, null, null, null, null, null, null, false, null);
        ObjectNode diag = mapper.createObjectNode();
        
        // Tenant 2 is requesting Tenant 1's story
        assertThrows(ResponseStatusException.class, () -> contextService.buildContext("user2", req, diag));
    }

    @Test
    void shouldRejectCrossTenantChapterId() {
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        story.setUserId("user1");
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        
        when(chapterRepository.findByIdAndStoryId(2, 1)).thenReturn(Optional.empty());

        NovelAiRequest req = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 2, null, null, null, null, null, null, null, null, null, false, null);
        ObjectNode diag = mapper.createObjectNode();
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> contextService.buildContext("user1", req, diag));
        assertTrue(ex.getReason().contains("Capítulo inválido"));
    }

    @Test
    void shouldNotSendEntireChapterForContinueScene() {
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        story.setUserId("user1");
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));

        ContextProfilesConfig profiles = new ContextProfilesConfig();
        ProfileBudget budget = new ProfileBudget(100, 100, 100, 100, 0, 0, 0); // Very small budget to force truncation
        profiles.setSceneContinuity(budget);
        lenient().when(config.getContext()).thenReturn(profiles);

        Chapter chapter = new Chapter();
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", 2);
        chapter.setChapterOrder(1);
        // Create a huge chapter content (10,000 chars)
        chapter.setContent("a".repeat(10000));
        when(chapterRepository.findByIdAndStoryId(2, 1)).thenReturn(Optional.of(chapter));
        
        NovelAiRequest req = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 2, null, null, null, null, null, null, null, null, null, false, null);
        ObjectNode diag = mapper.createObjectNode();
        
        NovelContextService.ContextResult result = contextService.buildContext("user1", req, diag);
        
        // Ensure it truncates the immediate continuity
        assertTrue(diag.get("currentChapterIncludedMode").asText().equals("TRUNCATED_END"));
        assertTrue(diag.get("immediateContinuityChars").asInt() < 10000);
        
        // 1/3 of max exceptional (12000) = 4000 tokens = ~16000 chars default. 
        // Let's set a smaller budget to force truncation.
    }
    
    @Test
    void shouldTruncateGloballyIfOversized() {
        ContextProfilesConfig profiles = new ContextProfilesConfig();
        ProfileBudget budget = new ProfileBudget(10, 10, 10, 10, 1, 1, 1); // Tiny budget
        profiles.setSceneContinuity(budget);
        lenient().when(config.getContext()).thenReturn(profiles);
        
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        story.setUserId("user1");
        story.setTitle("Super longo titulo gigante massivo extenso");
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));

        NovelAiRequest req = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, null, null, null, null, null, null, null, null, null, null, false, null);
        ObjectNode diag = mapper.createObjectNode();
        
        NovelContextService.ContextResult result = contextService.buildContext("user1", req, diag);
        
        assertTrue(diag.get("contextTruncated").asBoolean());
        assertTrue(result.contextText.contains("TRUNCADO]"));
        assertFalse(diag.get("previousChapterMemoryMayBeTruncated").asBoolean());
    }

    @Test
    void shouldUseCurrentEditorTextInsteadOfStaleDatabaseContent() {
        Story story = story(1);
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        Chapter chapter = chapter(2, 2, "Texto antigo salvo no banco.");
        when(chapterRepository.findByIdAndStoryId(2, 1)).thenReturn(Optional.of(chapter));

        NovelAiRequest request = new NovelAiRequest(
                NovelAiAction.CONTINUE_SCENE, 1, 2, null, null, null, null,
                null, null, null, null, null, false, null,
                "Texto atual ainda no editor com Kastor evitando o perigo."
        );
        ObjectNode diagnostics = mapper.createObjectNode();

        NovelContextService.ContextResult result =
                contextService.buildContext("user1", request, diagnostics);

        assertEquals("REQUEST", diagnostics.get("currentChapterTextSource").asText());
        assertTrue(result.contextText.contains("Kastor evitando o perigo"));
        assertFalse(result.contextText.contains("Texto antigo salvo no banco"));
    }

    @Test
    void shouldIgnoreStaleSummaryOfCurrentChapter() {
        Story story = story(1);
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        Chapter chapter = chapter(2, 2, "Arkendu decidiu permanecer fora do caso.");
        chapter.setGoal("Concluir a cena no DSI.");
        chapter.setSummary("Arkendu decidiu investigar o perigo.");
        chapter.setSummaryContentHash(ChapterMemorySupport.contentHash("texto anterior"));
        when(chapterRepository.findByIdAndStoryId(2, 1)).thenReturn(Optional.of(chapter));

        ObjectNode diagnostics = mapper.createObjectNode();
        NovelContextService.ContextResult result = contextService.buildContext(
                "user1",
                new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 2, null, null,
                        null, null, null, null, null, null, null, false, null),
                diagnostics
        );

        assertFalse(diagnostics.get("currentChapterSummaryCurrent").asBoolean());
        assertTrue(result.contextText.contains("Objetivo: Concluir a cena no DSI."));
        assertFalse(result.contextText.contains("decidiu investigar o perigo"));
    }

    @Test
    void shouldNotCountUnversionedSummaryAsValidMemory() {
        Story story = story(1);
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        Chapter current = chapter(3, 3, "Cena atual.");
        Chapter previous = chapter(2, 2, "Kastor aconselhou Arkendu a não procurar perigo.");
        previous.setSummary("Resumo antigo afirma incorretamente que Arkendu decidiu investigar.");
        when(chapterRepository.findByIdAndStoryId(3, 1)).thenReturn(Optional.of(current));
        when(chapterRepository.findByStoryIdOrderByChapterOrderAsc(1))
                .thenReturn(List.of(previous, current));

        ObjectNode diagnostics = mapper.createObjectNode();
        NovelContextService.ContextResult result = contextService.buildContext(
                "user1",
                new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 3, null, null,
                        null, null, null, null, null, null, null, false, null),
                diagnostics
        );

        assertEquals(0, diagnostics.get("previousChapterSummaryCount").asInt());
        assertEquals(1, diagnostics.get("previousChapterFallbackCount").asInt());
        assertEquals("FACTUAL_EXCERPT_FALLBACK",
                diagnostics.get("previousChapterMemorySources").get(0).get("source").asText());
        assertTrue(result.contextText.contains("Kastor aconselhou Arkendu"));
        assertFalse(result.contextText.contains("decidiu investigar"));
    }

    @Test
    void shouldIncludeOnlySummaryWhoseHashMatchesChapterContent() {
        Story story = story(1);
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        Chapter current = chapter(3, 3, "Cena atual.");
        Chapter previous = chapter(2, 2, "Kastor evita riscos desnecessários.");
        previous.setSummary("Kastor evita riscos e desaconselha investigações perigosas.");
        previous.setSummaryContentHash(ChapterMemorySupport.contentHash(previous.getContent()));
        when(chapterRepository.findByIdAndStoryId(3, 1)).thenReturn(Optional.of(current));
        when(chapterRepository.findByStoryIdOrderByChapterOrderAsc(1))
                .thenReturn(List.of(previous, current));

        ObjectNode diagnostics = mapper.createObjectNode();
        NovelContextService.ContextResult result = contextService.buildContext(
                "user1",
                new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 3, null, null,
                        null, null, null, null, null, null, null, false, null),
                diagnostics
        );

        assertEquals(1, diagnostics.get("previousChapterSummaryCount").asInt());
        assertEquals(0, diagnostics.get("previousChapterFallbackCount").asInt());
        assertEquals("CURRENT_SUMMARY",
                diagnostics.get("previousChapterMemorySources").get(0).get("source").asText());
        assertTrue(result.contextText.contains(previous.getSummary()));
    }

    @Test
    void shouldSelectRelevantCharactersDeterministicly() {
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        story.setUserId("user1");
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));

        Chapter chapter = new Chapter();
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", 2);
        chapter.setContent("Kastor e Arkendu estavam investigando.");
        when(chapterRepository.findByIdAndStoryId(2, 1)).thenReturn(Optional.of(chapter));

        StoryCharacter c1 = new StoryCharacter();
        org.springframework.test.util.ReflectionTestUtils.setField(c1, "id", 10); c1.setName("Kastor");
        StoryCharacter c2 = new StoryCharacter();
        org.springframework.test.util.ReflectionTestUtils.setField(c2, "id", 11); c2.setName("Arkendu");
        StoryCharacter c3 = new StoryCharacter();
        org.springframework.test.util.ReflectionTestUtils.setField(c3, "id", 12); c3.setName("Irrelevante");

        when(characterRepository.findByStoryId(1)).thenReturn(List.of(c1, c2, c3));

        // Há espaço para todos: o irrelevante deve ficar de fora por relevância,
        // e não por falta de vaga.
        ContextProfilesConfig profiles = new ContextProfilesConfig();
        ProfileBudget budget = new ProfileBudget(5000, 8000, 12000, 1000, 5, 5, 3);
        profiles.setSceneContinuity(budget);
        lenient().when(config.getContext()).thenReturn(profiles);

        NovelAiRequest req = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 2, null, null, null, null, null, null, null, null, null, false, null);
        ObjectNode diag = mapper.createObjectNode();
        
        NovelContextService.ContextResult result = contextService.buildContext("user1", req, diag);
        
        assertEquals(2, diag.get("characterCount").asInt());
        assertEquals(3, diag.get("availableCharacterCount").asInt());
        assertEquals(1, diag.get("excludedCharacterCount").asInt());
        assertEquals("Kastor", diag.get("selectedCharacterNames").get(0).asText());
        assertEquals("Arkendu", diag.get("selectedCharacterNames").get(1).asText());
        assertEquals(12, diag.get("excludedCharacterIds").get(0).asInt());
        assertEquals(
                "Irrelevante",
                diag.get("excludedCharacterNames").get(0).asText()
        );
        assertTrue(result.contextText.contains("Kastor"));
        assertTrue(result.contextText.contains("Arkendu"));
        assertFalse(result.contextText.contains("Irrelevante"));
    }

    @Test
    void shouldIncludeMentionedWorldNoteWithoutFillingRemainingSlots() {
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        story.setUserId("user1");
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));

        Chapter chapter = new Chapter();
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", 2);
        chapter.setChapterOrder(1);
        chapter.setContent(
                "Arkendu entrou no Departamento de Investigação e Saneamento (DSI)."
        );
        when(chapterRepository.findByIdAndStoryId(2, 1)).thenReturn(Optional.of(chapter));

        WorldNote dsi = worldNote(
                20,
                "Divisão de Saneamento e Inquérito (DSI)",
                "DSI, investigação",
                "FUNCAO_DSI"
        );
        WorldNote currency = worldNote(
                21,
                "Moeda",
                "Moeda, economia",
                "REGRA_MOEDA"
        );
        when(worldNoteRepository.findByStoryId(1)).thenReturn(List.of(dsi, currency));

        ContextProfilesConfig profiles = new ContextProfilesConfig();
        ProfileBudget budget = new ProfileBudget(5000, 8000, 12000, 1000, 5, 5, 3);
        profiles.setSceneContinuity(budget);
        lenient().when(config.getContext()).thenReturn(profiles);

        NovelAiRequest req = new NovelAiRequest(
                NovelAiAction.CONTINUE_SCENE,
                1,
                2,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );
        ObjectNode diag = mapper.createObjectNode();

        NovelContextService.ContextResult result =
                contextService.buildContext("user1", req, diag);

        assertEquals(2, diag.get("availableWorldNoteCount").asInt());
        assertEquals(1, diag.get("worldNoteCount").asInt());
        assertEquals(1, diag.get("excludedWorldNoteCount").asInt());
        assertEquals(20, diag.get("selectedWorldNoteIds").get(0).asInt());
        assertEquals(
                "Divisão de Saneamento e Inquérito (DSI)",
                diag.get("selectedWorldNoteTitles").get(0).asText()
        );
        assertEquals(21, diag.get("excludedWorldNoteIds").get(0).asInt());
        assertEquals(
                "Moeda",
                diag.get("excludedWorldNoteTitles").get(0).asText()
        );
        assertTrue(result.contextText.contains("FUNCAO_DSI"));
        assertFalse(result.contextText.contains("REGRA_MOEDA"));
    }

    @Test
    void shouldIncludeExplicitlySelectedWorldNoteEvenWhenNotMentioned() {
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        story.setUserId("user1");
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));

        WorldNote dsi = worldNote(
                20,
                "Divisão de Saneamento e Inquérito (DSI)",
                "DSI, investigação",
                "FUNCAO_DSI"
        );
        WorldNote currency = worldNote(
                21,
                "Moeda",
                "Moeda, economia",
                "REGRA_MOEDA"
        );
        when(worldNoteRepository.findByStoryId(1)).thenReturn(List.of(dsi, currency));

        ContextProfilesConfig profiles = new ContextProfilesConfig();
        ProfileBudget budget = new ProfileBudget(5000, 8000, 12000, 1000, 5, 5, 3);
        profiles.setSceneContinuity(budget);
        lenient().when(config.getContext()).thenReturn(profiles);

        NovelAiRequest req = new NovelAiRequest(
                NovelAiAction.CONTINUE_SCENE,
                1,
                null,
                null,
                "Continue sem alterar os fatos.",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(21),
                false,
                null
        );
        ObjectNode diag = mapper.createObjectNode();

        NovelContextService.ContextResult result =
                contextService.buildContext("user1", req, diag);

        assertEquals(1, diag.get("worldNoteCount").asInt());
        assertEquals(21, diag.get("selectedWorldNoteIds").get(0).asInt());
        assertTrue(result.contextText.contains("REGRA_MOEDA"));
        assertFalse(result.contextText.contains("FUNCAO_DSI"));
    }

    private WorldNote worldNote(
            int id,
            String title,
            String tags,
            String content
    ) {
        WorldNote note = new WorldNote();
        org.springframework.test.util.ReflectionTestUtils.setField(note, "id", id);
        note.setTitle(title);
        note.setTags(tags);
        note.setContent(content);
        return note;
    }

    private Story story(int id) {
        Story story = new Story();
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", id);
        story.setUserId("user1");
        story.setTitle("História de teste");
        return story;
    }

    private Chapter chapter(int id, int order, String content) {
        Chapter chapter = new Chapter();
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", id);
        chapter.setStoryId(1);
        chapter.setChapterOrder(order);
        chapter.setTitle("Capítulo " + order);
        chapter.setContent(content);
        return chapter;
    }
}
