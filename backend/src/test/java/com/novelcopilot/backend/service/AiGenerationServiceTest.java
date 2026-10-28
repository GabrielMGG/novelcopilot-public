package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.dto.GenerateChapterSummaryResponse;
import com.novelcopilot.backend.dto.GenerateAiRequest;
import com.novelcopilot.backend.config.NovelAiConfigProperties;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.entity.Story;
import com.novelcopilot.backend.entity.AiGeneration;
import com.novelcopilot.backend.repository.*;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiGenerationServiceTest {

    @Mock
    private AiProvider aiProvider;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private StoryCharacterRepository storyCharacterRepository;
    @Mock
    private WorldNoteRepository worldNoteRepository;
    @Mock
    private StoryReferenceRepository storyReferenceRepository;
    @Mock
    private AiGenerationRepository aiGenerationRepository;
    @Mock
    private AiUsageService aiUsageService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AiRateLimitService rateLimitService;
    @Mock
    private AiProviderResolver aiProviderResolver;
    @Mock
    private NovelAiConfigProperties novelAiConfig;
    @Mock
    private NovelAiConfigProperties.SummaryConfig summaryConfig;
    @Mock
    private AiCostCalculator aiCostCalculator;
    @Mock
    private NarrativeLanguageDetector narrativeLanguageDetector;

    @InjectMocks
    private AiGenerationService aiGenerationService;

    private Story story;
    private Chapter chapter;

    @BeforeEach
    void setUp() {
        story = new Story();
        story.setTitle("Test Story");

        chapter = new Chapter();
        chapter.setStoryId(1);
        chapter.setTitle("Chapter 1");

        AuthenticatedUserPrincipal mockUser = new AuthenticatedUserPrincipal("test-user-id", "test@example.com", "Test User", null, "email");
        lenient().when(currentUserService.getCurrentUser()).thenReturn(mockUser);
        lenient().when(novelAiConfig.getSummary()).thenReturn(summaryConfig);
        lenient().when(narrativeLanguageDetector.detect(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            if (text.contains("sintió curiosidad")) {
                return Optional.of(new NarrativeLanguageDetector.DetectionResult("es", "SPANISH", 0.91));
            }
            if (text.contains("aconselhou Arkendu")) {
                return Optional.of(new NarrativeLanguageDetector.DetectionResult("pt", "PORTUGUESE", 0.92));
            }
            return Optional.of(new NarrativeLanguageDetector.DetectionResult("en", "ENGLISH", 0.93));
        });
    }

    private AiGenerationResult buildResult(String content) {
        return new AiGenerationResult(content, "Groq", "llama-3.3-70b-versatile", 100L, 0L, 50L, 150L, 500L);
    }

    @Test
    void testReviewWithEnglishText_HasEnglishInstructionAndNoPortugueseFix() {
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(aiProvider.generate(anyString(), anyString())).thenReturn(buildResult("Revised text."));
        when(aiGenerationRepository.save(any(AiGeneration.class))).thenReturn(new AiGeneration());

        GenerateAiRequest request = new GenerateAiRequest(1, "REVIEW", "Please review", "She walk into the room and look around.", "She walk into the room and look around.");

        aiGenerationService.generate(1, request);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        verify(rateLimitService).checkAiGeneration("test-user-id", false);
        verify(aiProvider).generate(instructionsCaptor.capture(), promptCaptor.capture());

        String instructions = instructionsCaptor.getValue();
        String prompt = promptCaptor.getValue();

        assertTrue(instructions.contains("Use o idioma predominante do conteúdo narrativo como idioma da resposta."));
        assertTrue(prompt.contains("She walk into the room and look around."));
        assertTrue(prompt.contains("Devolva o texto revisado no mesmo idioma do trecho original."));
        assertFalse(instructions.toLowerCase().contains("responda em português") || prompt.toLowerCase().contains("responda em português"));
    }

    @Test
    void testReviewWithSpanishText_MaintainsOriginalLanguage() {
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(aiProvider.generate(anyString(), anyString())).thenReturn(buildResult("Texto revisado."));
        when(aiGenerationRepository.save(any(AiGeneration.class))).thenReturn(new AiGeneration());

        GenerateAiRequest request = new GenerateAiRequest(1, "REVIEW", "Revisar", "Ella caminaron hacia la puerta.", "Ella caminaron hacia la puerta.");

        aiGenerationService.generate(1, request);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        verify(aiProvider).generate(instructionsCaptor.capture(), promptCaptor.capture());

        String instructions = instructionsCaptor.getValue();
        String prompt = promptCaptor.getValue();

        assertTrue(instructions.contains("Use o idioma predominante do conteúdo narrativo como idioma da resposta."));
        assertTrue(prompt.contains("Ella caminaron hacia la puerta."));
        assertTrue(prompt.contains("Devolva o texto revisado no mesmo idioma do trecho original."));
    }

    @Test
    void testReviewWithFrenchText_ProhibitsTranslation() {
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(aiProvider.generate(anyString(), anyString())).thenReturn(buildResult("Texte révisé."));
        when(aiGenerationRepository.save(any(AiGeneration.class))).thenReturn(new AiGeneration());

        GenerateAiRequest request = new GenerateAiRequest(1, "REVIEW", "Revisar", "Il sont entré dans la pièce.", "Il sont entré dans la pièce.");

        aiGenerationService.generate(1, request);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        verify(aiProvider).generate(instructionsCaptor.capture(), promptCaptor.capture());

        String instructions = instructionsCaptor.getValue();
        String prompt = promptCaptor.getValue();

        assertTrue(instructions.contains("Use o idioma predominante do conteúdo narrativo como idioma da resposta."));
        assertTrue(prompt.contains("Il sont entré dans la pièce."));
        assertTrue(prompt.contains("Não traduza."));
    }

    @Test
    void testContinue_ReusesCentralRule() {
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(aiProvider.generate(anyString(), anyString())).thenReturn(buildResult("Continuou."));
        when(aiGenerationRepository.save(any(AiGeneration.class))).thenReturn(new AiGeneration());

        GenerateAiRequest request = new GenerateAiRequest(1, "CONTINUE", "Continuar", "Texto", "Texto");

        aiGenerationService.generate(1, request);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiProvider).generate(instructionsCaptor.capture(), anyString());

        assertTrue(instructionsCaptor.getValue().contains("Use o idioma predominante do conteúdo narrativo como idioma da resposta."));
    }

    @Test
    void testRewrite_ReusesCentralRule() {
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(aiProvider.generate(anyString(), anyString())).thenReturn(buildResult("Reescrito."));
        when(aiGenerationRepository.save(any(AiGeneration.class))).thenReturn(new AiGeneration());

        GenerateAiRequest request = new GenerateAiRequest(1, "REWRITE", "Reescrever", "Texto", "Texto");

        aiGenerationService.generate(1, request);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiProvider).generate(instructionsCaptor.capture(), anyString());

        assertTrue(instructionsCaptor.getValue().contains("Use o idioma predominante do conteúdo narrativo como idioma da resposta."));
    }

    @Test
    void testExpand_ReusesCentralRule() {
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(aiProvider.generate(anyString(), anyString())).thenReturn(buildResult("Expandido."));
        when(aiGenerationRepository.save(any(AiGeneration.class))).thenReturn(new AiGeneration());

        GenerateAiRequest request = new GenerateAiRequest(1, "EXPAND_SELECTION", "Expandir", "Texto", "Texto");

        aiGenerationService.generate(1, request);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiProvider).generate(instructionsCaptor.capture(), anyString());

        assertTrue(instructionsCaptor.getValue().contains("Use o idioma predominante do conteúdo narrativo como idioma da resposta."));
    }

    @Test
    void chapterMemoryPromptProtectsDemonstratedCautionAndUsesPlainText() {
        CapturedSummaryPrompt captured = generateSummaryAndCapture(
                "<p>Kastor aconselhou Arkendu a não procurar perigo.</p>"
                        + "<p>Arkendu afirmou que não pretendia investigar.</p>",
                "Kastor evita riscos. Arkendu não pretende investigar."
        );

        assertTrue(captured.instructions().contains("A promise not to act must never become determination to act."));
        assertTrue(captured.instructions().contains("preserve both that interpretation and the lack of action"));
        assertTrue(captured.instructions().contains("Never retain such a clue while omitting"));
        assertTrue(captured.instructions().contains("Do not invent dismissal, disbelief, or inaction"));
        assertTrue(captured.instructions().contains("A clue must never appear alone as an unresolved question"));
        assertTrue(captured.instructions().contains("Reactions to clues and decisions not to act"));
        assertTrue(captured.instructions().contains("is mandatory whenever CHAPTER_TEXT shows any character"));
        assertTrue(captured.instructions().contains("couple each retained clue to the exact character"));
        assertTrue(captured.instructions().contains("Do not turn caution into heroism"));
        assertTrue(captured.instructions().contains("Unknown ownership must remain unknown"));
        assertTrue(captured.instructions().contains("Working together does not establish mutual trust"));
        assertTrue(captured.instructions().contains("An open door does not establish who opened it"));
        assertTrue(captured.instructions().contains("actor-action-condition-evidence ledger"));
        assertTrue(captured.instructions().contains("Preserve the exact actor of every action"));
        assertTrue(captured.instructions().contains("Never replace one named actor with a pair"));
        assertTrue(captured.instructions().contains("Never claim that characters agreed"));
        assertTrue(captured.instructions().contains("Do not attach a condition from one action to a different action"));
        assertTrue(captured.instructions().contains("including that a character has no evidence"));
        assertTrue(captured.instructions().contains("is mandatory continuity information"));
        assertTrue(captured.instructions().contains("current physical location"));
        assertTrue(captured.instructions().contains("Write the entire answer in exactly that language"));
        assertTrue(captured.instructions().contains("Do not use Markdown, bullets, numbered lists"));
        assertFalse(captured.instructions().contains("Você é"));
        assertTrue(captured.prompt().contains("--- BEGIN CHAPTER TEXT ---"));
        assertTrue(captured.prompt().contains("Name: PORTUGUESE"));
        assertTrue(captured.prompt().contains("ISO_639_1: pt"));
        assertTrue(captured.prompt().contains("Kastor aconselhou Arkendu"));
        assertTrue(captured.prompt().contains("--- END CHAPTER TEXT ---"));
        assertTrue(captured.prompt().contains("Genre: Not provided"));
        assertTrue(captured.prompt().contains("Tone: Not provided"));
        assertFalse(captured.prompt().contains("Nao informado"));
        assertFalse(captured.prompt().contains("<p>"));
        assertEquals("Kastor evita riscos. Arkendu não pretende investigar.", captured.response().summary());
        assertEquals(ChapterMemorySupport.contentHash(chapter.getContent()), captured.response().summaryContentHash());
        assertEquals("Cloudflare", chapter.getSummaryProvider());
        assertEquals("modelo-memoria", chapter.getSummaryModel());
    }

    @Test
    void chapterMemoryPromptKeepsDismissedClueCoupledToReactionWithoutForcingInaction() {
        CapturedSummaryPrompt captured = generateSummaryAndCapture(
                "Arkendu ouviu um rumor de desaparecimento, considerou-o um caso comum e não o investigou. "
                        + "Lena encontrou outra pista e começou a investigá-la.",
                "Arkendu descartou o rumor e não o investigou. Lena começou a investigar a outra pista."
        );

        assertTrue(captured.instructions().contains("potential clue, rumor, anomaly, or danger"));
        assertTrue(captured.instructions().contains("preserve both that interpretation and the lack of action"));
        assertTrue(captured.instructions().contains("Never retain such a clue while omitting"));
        assertTrue(captured.instructions().contains("when the chapter instead shows interest, investigation, or another response"));
        assertTrue(captured.instructions().contains("state in the same sentence each explicitly established character interpretation"));
        assertTrue(captured.prompt().contains("Arkendu ouviu um rumor de desaparecimento"));
        assertTrue(captured.prompt().contains("Lena encontrou outra pista e começou a investigá-la"));
        assertEquals(
                "Arkendu descartou o rumor e não o investigou. Lena começou a investigar a outra pista.",
                captured.response().summary()
        );
    }

    @Test
    void chapterMemoryPromptMakesArkendusDismissalMandatoryInsteadOfLeavingTheRumorUnresolved() {
        CapturedSummaryPrompt captured = generateSummaryAndCapture(
                "Duas mulheres disseram que mulheres e crianças desapareceram sem deixar rastro. "
                        + "Arkendu ouviu a conversa em silêncio. Não havia nenhum grande mistério naquilo. "
                        + "Para ele, eram ocorrências comuns causadas por dívidas, mercado negro e pela negligência da DSI. "
                        + "Arkendu não decidiu investigar os desaparecimentos.",
                "Reações a pistas e decisões de não agir\n"
                        + "Arkendu interpretou os desaparecimentos como ocorrências comuns ligadas a dívidas, mercado negro e negligência da DSI, e não decidiu investigá-los."
        );

        assertTrue(captured.instructions().contains("Reactions to clues and decisions not to act"));
        assertTrue(captured.instructions().contains("A clue must never appear alone as an unresolved question"));
        assertTrue(captured.instructions().contains("resulting action or inaction"));
        assertTrue(captured.prompt().contains("Não havia nenhum grande mistério naquilo"));
        assertTrue(captured.prompt().contains("Arkendu não decidiu investigar"));
        assertEquals(
                "Reações a pistas e decisões de não agir\n"
                        + "Arkendu interpretou os desaparecimentos como ocorrências comuns ligadas a dívidas, mercado negro e negligência da DSI, e não decidiu investigá-los.",
                captured.response().summary()
        );
    }

    @Test
    void chapterMemoryPromptUsesNeutralInstructionsForEnglishNarrative() {
        CapturedSummaryPrompt captured = generateSummaryAndCapture(
                "Lena was curious, but she would not enter without authorization. Tomas had no evidence.",
                "Final state\nLena did not enter. Tomas had no evidence."
        );

        assertTrue(captured.instructions().contains("supplied in DETECTED_OUTPUT_LANGUAGE"));
        assertTrue(captured.instructions().contains("including every section label"));
        assertTrue(captured.instructions().contains("Never claim that characters agreed"));
        assertTrue(captured.instructions().contains("Preserve the exact scope and condition"));
        assertTrue(captured.instructions().contains("Every explicit negation and evidential limitation"));
        assertFalse(captured.instructions().contains("Responda em português"));
        assertTrue(captured.prompt().contains("Lena was curious"));
        assertTrue(captured.prompt().contains("Name: ENGLISH"));
        assertTrue(captured.prompt().contains("ISO_639_1: en"));
        assertEquals("Final state\nLena did not enter. Tomas had no evidence.", captured.response().summary());
    }

    @Test
    void chapterMemoryPromptUsesNeutralInstructionsForSpanishNarrative() {
        CapturedSummaryPrompt captured = generateSummaryAndCapture(
                "Lena sintió curiosidad, pero no entraría sin autorización. Tomas no tenía pruebas.",
                "Estado final\nLena no entró. Tomas no tenía pruebas."
        );

        assertTrue(captured.instructions().contains("supplied in DETECTED_OUTPUT_LANGUAGE"));
        assertTrue(captured.instructions().contains("including every section label"));
        assertTrue(captured.instructions().contains("Preserve names verbatim"));
        assertFalse(captured.instructions().contains("Use o idioma predominante"));
        assertTrue(captured.prompt().contains("Lena sintió curiosidad"));
        assertTrue(captured.prompt().contains("Name: SPANISH"));
        assertTrue(captured.prompt().contains("ISO_639_1: es"));
        assertEquals("Estado final\nLena no entró. Tomas no tenía pruebas.", captured.response().summary());
    }

    @Test
    void chapterMemoryStopsBeforeExternalCallsWhenLanguageIsUncertain() {
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", 1);
        chapter.setContent("Prologue");
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(narrativeLanguageDetector.detect("Prologue")).thenReturn(Optional.empty());

        org.springframework.web.server.ResponseStatusException error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> aiGenerationService.generateChapterSummary(1, 1)
        );

        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(aiProviderResolver, never()).resolve(anyString());
        verify(aiProvider, never()).generateSummary(anyString(), anyString(), anyString(), any());
        verify(aiUsageService, never()).recordSuccess(anyString(), any(), any(), anyString(), any());
    }

    @Test
    void currentChapterMemoryIsReturnedWithoutAnotherProviderCall() {
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", 1);
        chapter.setContent("Kastor evita perigo.");
        chapter.setSummary("Kastor evita perigo.");
        chapter.setSummaryContentHash(ChapterMemorySupport.contentHash(chapter.getContent()));
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        GenerateChapterSummaryResponse response =
                aiGenerationService.generateChapterSummary(1, 1);

        assertEquals(chapter.getSummary(), response.summary());
        verify(aiProviderResolver, never()).resolve(anyString());
        verify(aiProvider, never()).generateSummary(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testAnalyticalAction_PrioritizesStoryLanguage() {
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(aiProvider.generate(anyString(), anyString())).thenReturn(buildResult("Análise."));
        when(aiGenerationRepository.save(any(AiGeneration.class))).thenReturn(new AiGeneration());

        GenerateAiRequest request = new GenerateAiRequest(1, "PLAN", "Planejar", "Texto", "Texto");

        aiGenerationService.generate(1, request);

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiProvider).generate(instructionsCaptor.capture(), anyString());

        assertTrue(instructionsCaptor.getValue().contains("Use o idioma predominante do conteúdo narrativo como idioma da resposta."));
    }

    @Test
    void generateChapterSummary_BlocksWhenCostIsNull() {
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", 1);
        chapter.setContent("Texto válido");

        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(summaryConfig.getProvider()).thenReturn("cloudflare");
        when(summaryConfig.getModel()).thenReturn("modelo-sem-preco");
        when(summaryConfig.getMaxOutputTokens()).thenReturn(1000);

        when(aiCostCalculator.calculateCost(null, "cloudflare", "modelo-sem-preco", 0, 0, 0))
                .thenReturn(new AiCostCalculator.CalculationResult(null, "CALCULATED", null));

        org.springframework.web.server.ResponseStatusException error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> aiGenerationService.generateChapterSummary(1, 1)
        );

        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertTrue(error.getReason().contains("Preço do modelo não configurado"));

        verify(rateLimitService, never()).checkAiGeneration(anyString(), any(Boolean.class));
        verify(aiProviderResolver, never()).resolve(anyString());
        verify(aiProvider, never()).generateSummary(anyString(), anyString(), anyString(), any());
        verify(chapterRepository, never()).save(any());
        verify(aiGenerationRepository, never()).save(any());
        verify(aiUsageService, never()).recordSuccess(any(), any(), any(), any(), any());
        verify(aiUsageService, never()).recordError(any(), any(), any(), any(), any(), any(), anyLong(), any());

        assertNull(chapter.getSummary());
        assertNull(chapter.getSummaryContentHash());
    }

    private CapturedSummaryPrompt generateSummaryAndCapture(
            String chapterContent,
            String generatedSummary
    ) {
        org.springframework.test.util.ReflectionTestUtils.setField(story, "id", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(chapter, "id", 1);
        chapter.setContent(chapterContent);
        when(storyRepository.findById(1)).thenReturn(Optional.of(story));
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(summaryConfig.getProvider()).thenReturn("cloudflare");
        when(summaryConfig.getModel()).thenReturn("modelo-memoria");
        when(summaryConfig.getMaxOutputTokens()).thenReturn(1400);
        when(aiProviderResolver.resolve("cloudflare")).thenReturn(aiProvider);
        when(aiProvider.generateSummary(anyString(), anyString(), anyString(), any()))
                .thenReturn(new AiGenerationResult(
                        generatedSummary,
                        "Cloudflare", "modelo-memoria", 100L, 0L, 50L, 150L, 500L
                ));
        when(chapterRepository.save(any(Chapter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGenerationRepository.save(any(AiGeneration.class)))
                .thenReturn(new AiGeneration());
        when(aiCostCalculator.calculateCost(null, "cloudflare", "modelo-memoria", 0, 0, 0))
                .thenReturn(new AiCostCalculator.CalculationResult(
                        java.math.BigDecimal.ONE,
                        "CALCULATED",
                        null
                ));

        GenerateChapterSummaryResponse response =
                aiGenerationService.generateChapterSummary(1, 1);

        ArgumentCaptor<String> instructions = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiProvider).generateSummary(
                instructions.capture(),
                prompt.capture(),
                org.mockito.ArgumentMatchers.eq("modelo-memoria"),
                org.mockito.ArgumentMatchers.eq(1400)
        );

        return new CapturedSummaryPrompt(
                instructions.getValue(),
                prompt.getValue(),
                response
        );
    }

    private record CapturedSummaryPrompt(
            String instructions,
            String prompt,
            GenerateChapterSummaryResponse response
    ) {
    }
}
