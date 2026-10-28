package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.NovelAiAction;
import com.novelcopilot.backend.dto.NovelAiRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NovelPromptBuilderTest {

    private final NovelPromptBuilder builder = new NovelPromptBuilder();

    @Test
    void testJsonActionDoesNotReceiveProseRule() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.PLAN_SCENE, 1, 1, null, null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildSystemPrompt(request);
        assertTrue(prompt.contains("Retorne somente JSON válido"));
        assertFalse(prompt.contains("Retorne somente texto simples"));
    }

    @Test
    void testProseActionDoesNotReceiveJsonRule() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 1, null, null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildSystemPrompt(request);
        assertTrue(prompt.contains("Retorne somente texto simples"));
        assertFalse(prompt.contains("Retorne somente JSON válido"));
    }

    @Test
    void testContinueSceneRules() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 1, null, null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildActionPrompt(NovelAiAction.CONTINUE_SCENE, request, "context");

        assertTrue(prompt.contains("AÇÃO: CONTINUAR CENA"));
        assertTrue(prompt.contains("Não invente números, causas, pistas"));
        assertTrue(prompt.contains("Reutilize normalmente fatos e elementos já presentes no contexto"));
        assertTrue(prompt.contains("Não uniformize suas vozes"));
        assertTrue(prompt.contains("Evite clichês repetidos"));
        assertTrue(prompt.contains("não invente uma personalidade completa"));
    }

    @Test
    void testContinuityHierarchyProtectsExplicitSceneState() {
        NovelAiRequest request = new NovelAiRequest(
                NovelAiAction.CONTINUE_SCENE,
                1,
                1,
                null,
                "Desenvolva a investigação.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        String prompt = builder.buildActionPrompt(
                NovelAiAction.CONTINUE_SCENE,
                request,
                "Os dois investigadores receberam juntos a missão Alfa. "
                        + "A missão Beta já possui outra equipe."
        );

        assertTrue(prompt.contains("HIERARQUIA OBRIGATÓRIA DE CONTINUIDADE"));
        assertTrue(prompt.contains("ESTADO ATUAL DA CENA"));
        assertTrue(prompt.contains("CÂNONE RELEVANTE"));
        assertTrue(prompt.contains("INSTRUÇÃO DO USUÁRIO"));
        assertTrue(prompt.contains("LIBERDADE CRIATIVA"));
        assertTrue(prompt.contains("quem está presente ou junto"));
        assertTrue(prompt.contains("tarefas já foram atribuídas"));
        assertTrue(prompt.contains("reabrir uma alternativa já encerrada"));
        assertTrue(prompt.contains("Não invente familiaridade anterior"));
        assertFalse(prompt.contains("Arkendu"));
        assertFalse(prompt.contains("Kastor"));
        assertFalse(prompt.contains("DSI"));
    }

    @Test
    void testAllContinuationActionsReceiveHierarchy() {
        NovelAiAction[] actions = {
                NovelAiAction.CONTINUE_SCENE,
                NovelAiAction.INLINE_CONTINUATION,
                NovelAiAction.SUGGEST_CONTINUATION,
                NovelAiAction.PLAN_SCENE,
                NovelAiAction.WRITE_CHAPTER_FROM_BEATS,
                NovelAiAction.CREATE_NEXT_CHAPTER
        };

        for (NovelAiAction action : actions) {
            NovelAiRequest request = new NovelAiRequest(
                    action,
                    1,
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            String prompt = builder.buildActionPrompt(action, request, "context");

            assertTrue(
                    prompt.contains("HIERARQUIA OBRIGATÓRIA DE CONTINUIDADE"),
                    "Action " + action + " must protect narrative continuity"
            );
        }
    }

    @Test
    void testInlineContinuationRules() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.INLINE_CONTINUATION, 1, 1, null, null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildActionPrompt(NovelAiAction.INLINE_CONTINUATION, request, "context");

        assertTrue(prompt.contains("AÇÃO: CONTINUAÇÃO INLINE"));
        assertTrue(prompt.contains("80 e 180 palavras"));
        assertTrue(prompt.contains("Não resuma"));
        assertFalse(prompt.contains("formato JSON"));
    }

    @Test
    void testSuggestContinuationReturnsJson() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.SUGGEST_CONTINUATION, 1, 1, null, null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildActionPrompt(NovelAiAction.SUGGEST_CONTINUATION, request, "context");

        assertTrue(prompt.contains("AÇÃO: SUGERIR CONTINUAÇÃO"));
        assertTrue(prompt.contains("formato JSON"));
        assertTrue(prompt.contains("\"safe\""));
        assertTrue(prompt.contains("\"unexpected\""));
        assertTrue(prompt.contains("\"intense\""));
    }

    @Test
    void testRewriteSelectionDoesNotContinueScene() {
        NovelAiRequest request = new NovelAiRequest(
                NovelAiAction.REWRITE_SELECTION,
                1,
                1,
                "selected text",
                "Troque explicitamente o destino da missão neste trecho.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        String prompt = builder.buildActionPrompt(NovelAiAction.REWRITE_SELECTION, request, "context");
        String systemPrompt = builder.buildSystemPrompt(request);

        assertTrue(prompt.contains("AÇÃO: REESCREVER TRECHO"));
        assertTrue(prompt.contains("continuação, resumo ou nova cena"));
        assertTrue(prompt.contains("exceto quando a instrução do usuário pedir explicitamente"));
        assertTrue(prompt.contains("grau de formalidade"));
        assertTrue(prompt.contains("entre 1 e 8 palavras"));
        assertTrue(prompt.contains("um único parágrafo"));
        assertFalse(prompt.contains("HIERARQUIA OBRIGATÓRIA DE CONTINUIDADE"));
        assertTrue(systemPrompt.contains("EXCEÇÃO DE EDIÇÃO"));
        assertTrue(systemPrompt.contains("somente dentro do trecho selecionado"));
    }

    @Test
    void testRewriteLimitsKeepShortAndMediumSelectionsProportional() {
        var shortLimits = NovelPromptBuilder.rewriteLimits("muito assustador");
        var paragraphLimits = NovelPromptBuilder.rewriteLimits(words(100));

        assertEquals(1, shortLimits.minimumWords());
        assertEquals(8, shortLimits.maximumWords());
        assertEquals(64, shortLimits.maximumOutputTokens());
        assertEquals(60, paragraphLimits.minimumWords());
        assertEquals(150, paragraphLimits.maximumWords());
        assertDoesNotThrow(() -> NovelPromptBuilder.validateRewrite("muito assustador", "aterrorizante"));
        assertThrows(IllegalStateException.class, () -> NovelPromptBuilder.validateRewrite(words(100), words(40)));
        assertThrows(IllegalStateException.class, () -> NovelPromptBuilder.validateRewrite(words(100), words(151)));
    }

    @Test
    void testRewriteValidationSupportsLanguagesAndRejectsMetaText() {
        String portuguese = "Lena não entrou na estação porque aguardava autorização.";
        String english = "Lena did not enter the station because she was waiting for authorization.";
        String spanish = "Lena no entró en la estación porque esperaba autorización.";

        assertDoesNotThrow(() -> NovelPromptBuilder.validateRewrite(portuguese, portuguese));
        assertDoesNotThrow(() -> NovelPromptBuilder.validateRewrite(english, english));
        assertDoesNotThrow(() -> NovelPromptBuilder.validateRewrite(spanish, spanish));
        assertThrows(
                IllegalStateException.class,
                () -> NovelPromptBuilder.validateRewrite("A porta rangeu devagar.", "Aqui está a reescrita: a porta rangeu.")
        );
        assertThrows(
                IllegalStateException.class,
                () -> NovelPromptBuilder.validateRewrite("A porta rangeu devagar.", "A porta rangeu.\n\nO corredor escureceu.")
        );
    }

    private static String words(int count) {
        return String.join(" ", java.util.Collections.nCopies(count, "palavra"));
    }

    @Test
    void testAddTensionDoesNotStartClimax() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.ADD_TENSION, 1, 1, "selected text", null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildActionPrompt(NovelAiAction.ADD_TENSION, request, "context");

        assertTrue(prompt.contains("AÇÃO: ADICIONAR TENSÃO"));
        assertTrue(prompt.contains("Não transforme a cena em ação exagerada"));
    }

    @Test
    void testToneWorksWithNullLanguage() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 1, null, null, null, "Dark", null, null, null, null, null, null, null);
        String sysPrompt = builder.buildSystemPrompt(request);

        assertTrue(sysPrompt.contains("TOM OBRIGATÓRIO: Dark"));
    }

    @Test
    void testPovWorksWithNullLanguage() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.CONTINUE_SCENE, 1, 1, null, null, null, null, "Third person", null, null, null, null, null, null);
        String sysPrompt = builder.buildSystemPrompt(request);

        assertTrue(sysPrompt.contains("POV OBRIGATÓRIO: Third person"));
    }

    @Test
    void testCreateNextChapterRules() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.CREATE_NEXT_CHAPTER, 1, 1, null, null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildActionPrompt(NovelAiAction.CREATE_NEXT_CHAPTER, request, "context");
        assertTrue(prompt.contains("AÇÃO: GERAR CAPÍTULO"));
        assertTrue(prompt.contains("Mostre consequências das ações anteriores"));
    }

    @Test
    void testWriteChapterFromBeatsRules() {
        NovelAiRequest request = new NovelAiRequest(NovelAiAction.WRITE_CHAPTER_FROM_BEATS, 1, 1, null, null, null, null, null, null, null, null, null, null, null);
        String prompt = builder.buildActionPrompt(NovelAiAction.WRITE_CHAPTER_FROM_BEATS, request, "context");
        assertTrue(prompt.contains("FIEL"));
        assertTrue(prompt.contains("FLEXÍVEL"));
    }

    @Test
    void testAllActionsHaveTemplates() {
        for (NovelAiAction action : NovelAiAction.values()) {
            NovelAiRequest request = new NovelAiRequest(action, 1, 1, "test", null, null, null, null, null, null, null, null, null, null);
            String prompt = builder.buildActionPrompt(action, request, "context");
            assertNotNull(prompt, "Action " + action + " returned null template");
            assertFalse(prompt.isBlank(), "Action " + action + " returned blank template");
        }
    }

    @Test
    void shouldRequestAProportionalEvidenceBasedChapterReview() {
        NovelAiRequest request = new NovelAiRequest(
                NovelAiAction.REVIEW_CHAPTER, 1, 1, null, null, null,
                null, null, null, null, null, null, null, null
        );

        String systemPrompt = builder.buildSystemPrompt(request);
        String prompt = builder.buildActionPrompt(NovelAiAction.REVIEW_CHAPTER, request, "context");

        assertTrue(systemPrompt.contains("JSON"));
        assertTrue(prompt.contains("proporcionalmente"));
        assertTrue(prompt.toLowerCase().contains("evid"));
        assertTrue(prompt.contains("optional"));
        assertTrue(prompt.contains("personagens, regras, conflitos"));
        assertTrue(prompt.contains("critical|important|optional"));
    }

    @Test
    void shouldRequestThreeShortChapterTitlesInStoryLanguage() {
        NovelAiRequest request = new NovelAiRequest(
                NovelAiAction.GENERATE_CHAPTER_TITLE, 1, 1, null, null, null,
                null, null, null, null, null, null, null, null
        );

        String systemPrompt = builder.buildSystemPrompt(request);
        String prompt = builder.buildActionPrompt(NovelAiAction.GENERATE_CHAPTER_TITLE, request, "context");

        assertTrue(systemPrompt.contains("somente JSON válido"));
        assertTrue(prompt.contains("EXATAMENTE TRES"));
        assertTrue(prompt.contains("entre 2 e 8 palavras"));
        assertTrue(prompt.contains("mesmo idioma predominante do capitulo"));
        assertTrue(prompt.contains("somente no conteudo do capitulo atual"));
        assertTrue(prompt.contains("{\"options\":[\"...\",\"...\",\"...\"]}"));
    }
}
