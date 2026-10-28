package com.novelcopilot.backend.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novelcopilot.backend.config.NovelAiConfigProperties;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NovelContextService {

    private static final Set<String> REFERENCE_STOP_WORDS = Set.of(
            "a", "as", "com", "da", "das", "de", "do", "dos", "e", "em",
            "mundo", "na", "nas", "no", "nos", "nota", "o", "os", "para",
            "por", "sem", "um", "uma"
    );

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryCharacterRepository characterRepository;
    private final WorldNoteRepository worldNoteRepository;
    private final NovelAiConfigProperties config;
    private final TokenEstimator tokenEstimator;

    public NovelContextService(StoryRepository storyRepository,
                               ChapterRepository chapterRepository,
                               StoryCharacterRepository characterRepository,
                               WorldNoteRepository worldNoteRepository,
                               NovelAiConfigProperties config,
                               TokenEstimator tokenEstimator) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.characterRepository = characterRepository;
        this.worldNoteRepository = worldNoteRepository;
        this.config = config;
        this.tokenEstimator = tokenEstimator;
    }

    public ContextProfile resolveProfile(NovelAiAction action) {
        return switch (action) {
            case REWRITE_SELECTION, EXPAND_SELECTION, IMPROVE_DIALOGUE, ADD_TENSION, SIMPLIFY_SELECTION -> ContextProfile.SELECTION_EDIT;
            case INLINE_CONTINUATION -> ContextProfile.INLINE_LOW_COST;
            case CONTINUE_SCENE -> ContextProfile.SCENE_CONTINUITY;
            case SUGGEST_CONTINUATION -> ContextProfile.CONTINUATION_SUGGESTIONS;
            case WRITE_CHAPTER_FROM_BEATS -> ContextProfile.CHAPTER_FROM_BEATS;
            case GENERATE_CHAPTER_TITLE -> ContextProfile.CHAPTER_TITLE;
            case REVIEW_CHAPTER -> ContextProfile.CHAPTER_REVIEW;
            default -> ContextProfile.SCENE_CONTINUITY; // Fallback
        };
    }

    public ProfileBudget getBudget(ContextProfile profile) {
        return switch (profile) {
            case SELECTION_EDIT -> config.getContext().getSelectionEdit();
            case INLINE_LOW_COST -> config.getContext().getInlineLowCost();
            case SCENE_CONTINUITY -> config.getContext().getSceneContinuity();
            case CONTINUATION_SUGGESTIONS -> config.getContext().getContinuationSuggestions();
            case CHAPTER_FROM_BEATS -> config.getContext().getChapterFromBeats();
            case CHAPTER_TITLE -> config.getContext().getChapterTitle();
            case CHAPTER_REVIEW -> config.getContext().getChapterReview();
        };
    }

    public static class ContextResult {
        public String contextText;
        public ObjectNode diagnostics;

        public ContextResult(String contextText, ObjectNode diagnostics) {
            this.contextText = contextText;
            this.diagnostics = diagnostics;
        }
    }

    public ContextResult buildContext(String userId, NovelAiRequest request, ObjectNode diagnostics) {
        ContextProfile profile = resolveProfile(request.action());
        ProfileBudget budget = getBudget(profile);

        diagnostics.put("contextStrategyVersion", "v2-deterministic");
        diagnostics.put("contextProfile", profile.name());

        // 1. ISOLAMENTO POR HISTÓRIA
        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "História não encontrada."));

        if (!story.getUserId().equals(userId)) { // Assumindo método getUserId em Story, que valida tenancy
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado à história.");
        }

        StringBuilder ctx = new StringBuilder();
        int currentTokens = 0;

        // Bloco: STORY_CORE (Obrigatório / Prioritário)
        StringBuilder storyCore = new StringBuilder();
        storyCore.append("### HISTÓRIA GERAL\n");
        storyCore.append("Título: ").append(story.getTitle()).append("\n");
        if (story.getGenre() != null) storyCore.append("Gênero: ").append(story.getGenre()).append("\n");
        if (story.getTone() != null) storyCore.append("Tom Geral: ").append(story.getTone()).append("\n");
        if (story.getMainIdea() != null) storyCore.append("Premissa: ").append(story.getMainIdea()).append("\n");
        if (story.getSummary() != null) storyCore.append("Sinopse: ").append(story.getSummary()).append("\n");
        storyCore.append("\n");
        
        ctx.append(storyCore);
        currentTokens += tokenEstimator.estimateTokens(storyCore.toString());

        Chapter currentChapter = null;
        if (request.chapterId() != null) {
            currentChapter = chapterRepository.findByIdAndStoryId(request.chapterId(), request.storyId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capítulo inválido para esta história."));
            
            // Bloco: CURRENT_CHAPTER_METADATA
            if (profile != ContextProfile.CHAPTER_FROM_BEATS) {
                StringBuilder meta = new StringBuilder();
                meta.append("### CAPÍTULO ATUAL\n");
                meta.append("Título: ").append(currentChapter.getTitle()).append("\n");
                if (currentChapter.getGoal() != null && !currentChapter.getGoal().isBlank()) {
                    meta.append("Objetivo: ").append(currentChapter.getGoal()).append("\n");
                }
                boolean currentChapterSummaryCurrent =
                        ChapterMemorySupport.hasCurrentSummary(currentChapter);
                diagnostics.put("currentChapterSummaryCurrent", currentChapterSummaryCurrent);
                if (currentChapterSummaryCurrent) {
                    meta.append("Memória validada: ")
                            .append(currentChapter.getSummary())
                            .append("\n");
                }
                meta.append("\n");
                ctx.append(meta);
                currentTokens += tokenEstimator.estimateTokens(meta.toString());
            }
        }

        // TEXT_BEFORE_SELECTION / TEXT_AFTER_SELECTION / IMMEDIATE_CONTINUITY
        String persistedChapterText = currentChapter != null
                ? ChapterMemorySupport.normalizePlainText(currentChapter.getContent())
                : "";
        String editorText = request.currentText() != null
                ? ChapterMemorySupport.normalizePlainText(request.currentText())
                : persistedChapterText;
        diagnostics.put(
                "currentChapterTextSource",
                request.currentText() != null ? "REQUEST" : "DATABASE"
        );
        if (request.selectedText() != null && !request.selectedText().isBlank() && (profile == ContextProfile.SELECTION_EDIT || profile == ContextProfile.INLINE_LOW_COST)) {
             // A própria seleção está no prompt de ação, mas podemos mandar contexto arredor
             ctx.append("### CONTEXTO DO TRECHO\n");
             ctx.append("(O trecho selecionado será enviado separadamente na instrução da ação)\n\n");
        } else if (profile == ContextProfile.CHAPTER_TITLE || profile == ContextProfile.CHAPTER_REVIEW) {
             if (!editorText.isBlank()) {
                 ctx.append("### CONTEUDO DO CAPITULO ATUAL\n");
                 int availableChars = tokenEstimator.tokensToChars(budget.getMaxExceptionalTokens()) - ctx.length();
                 int maxChars = profile == ContextProfile.CHAPTER_REVIEW
                         ? Math.max(1000, (int) (availableChars * 0.65))
                         : Math.max(1000, availableChars);
                 if (editorText.length() > maxChars) {
                     int headChars = maxChars / 2;
                     int tailChars = maxChars - headChars;
                     ctx.append(editorText, 0, headChars)
                             .append("\n...[MEIO DO CAPITULO OMITIDO PARA CONTROLAR O CUSTO]...\n")
                             .append(editorText, editorText.length() - tailChars, editorText.length());
                     diagnostics.put("currentChapterIncludedMode", "TRUNCATED_MIDDLE");
                     diagnostics.put("currentChapterChars", maxChars);
                 } else {
                     ctx.append(editorText);
                     diagnostics.put("currentChapterIncludedMode", "FULL");
                     diagnostics.put("currentChapterChars", editorText.length());
                 }
                 ctx.append("\n\n");
             }
        } else if (profile == ContextProfile.SCENE_CONTINUITY || profile == ContextProfile.CONTINUATION_SUGGESTIONS || profile == ContextProfile.INLINE_LOW_COST) {
             if (!editorText.isBlank()) {
                 ctx.append("### CENA ATUAL (CONTINUIDADE IMEDIATA)\n");
                 // Truncamento: pegar finalzinho do capítulo
                 int maxChars = tokenEstimator.tokensToChars(budget.getMaxExceptionalTokens() / 3); // 1/3 do orçamento para a cena atual
                 if (editorText.length() > maxChars) {
                     ctx.append(editorText.substring(editorText.length() - maxChars));
                     diagnostics.put("immediateContinuityChars", maxChars);
                     diagnostics.put("currentChapterIncludedMode", "TRUNCATED_END");
                 } else {
                     ctx.append(editorText);
                     diagnostics.put("immediateContinuityChars", editorText.length());
                     diagnostics.put("currentChapterIncludedMode", "FULL");
                 }
                 ctx.append("\n\n");
             }
        }
        
        // CHAPTER_BEATS
        if (profile == ContextProfile.CHAPTER_FROM_BEATS && request.instruction() != null) {
            // Os beats vão na instrução do action prompt para este perfil
            diagnostics.put("currentChapterIncludedMode", "BEATS_ONLY");
        }

        // PERSONAGENS RELEVANTES
        List<StoryCharacter> chars = characterRepository.findByStoryId(story.getId());
        diagnostics.put("availableCharacterCount", chars.size());
        if (!chars.isEmpty() && budget.getMaxCharacters() > 0) {
            List<StoryCharacter> relevantChars = selectRelevantCharacters(chars, request, editorText, budget.getMaxCharacters());
            diagnostics.put("characterCount", relevantChars.size());
            diagnostics.put(
                    "excludedCharacterCount",
                    chars.size() - relevantChars.size()
            );
            var selectedCharacterIds = diagnostics.putArray("selectedCharacterIds");
            var selectedCharacterNames = diagnostics.putArray("selectedCharacterNames");
            for (StoryCharacter character : relevantChars) {
                selectedCharacterIds.add(character.getId());
                selectedCharacterNames.add(character.getName());
            }
            var excludedCharacterIds = diagnostics.putArray("excludedCharacterIds");
            var excludedCharacterNames = diagnostics.putArray("excludedCharacterNames");
            for (StoryCharacter character : chars) {
                if (!relevantChars.contains(character)) {
                    excludedCharacterIds.add(character.getId());
                    excludedCharacterNames.add(character.getName());
                }
            }
            if (!relevantChars.isEmpty()) {
                ctx.append("### PERSONAGENS RELEVANTES\n");
                int charChars = 0;
                for (StoryCharacter c : relevantChars) {
                    StringBuilder cb = new StringBuilder();
                    cb.append("Nome: ").append(c.getName()).append("\n");
                    if (c.getRole() != null) cb.append("Papel: ").append(c.getRole()).append("\n");
                    if (c.getPersonality() != null) cb.append("Personalidade: ").append(c.getPersonality()).append("\n");
                    if (c.getGoal() != null) cb.append("Objetivo: ").append(c.getGoal()).append("\n");
                    if (c.getConflict() != null) cb.append("Conflito: ").append(c.getConflict()).append("\n");
                    cb.append("\n");
                    ctx.append(cb);
                    charChars += cb.length();
                }
                diagnostics.put("characterChars", charChars);
            }
        }

        // NOTAS DE MUNDO RELEVANTES
        List<WorldNote> notes = worldNoteRepository.findByStoryId(story.getId());
        diagnostics.put("availableWorldNoteCount", notes.size());
        if (!notes.isEmpty() && budget.getMaxWorldNotes() > 0) {
            List<WorldNote> relevantNotes = selectRelevantNotes(notes, request, editorText, budget.getMaxWorldNotes());
            diagnostics.put("worldNoteCount", relevantNotes.size());
            diagnostics.put(
                    "excludedWorldNoteCount",
                    notes.size() - relevantNotes.size()
            );
            var selectedWorldNoteIds = diagnostics.putArray("selectedWorldNoteIds");
            var selectedWorldNoteTitles = diagnostics.putArray("selectedWorldNoteTitles");
            for (WorldNote note : relevantNotes) {
                selectedWorldNoteIds.add(note.getId());
                selectedWorldNoteTitles.add(note.getTitle());
            }
            var excludedWorldNoteIds = diagnostics.putArray("excludedWorldNoteIds");
            var excludedWorldNoteTitles = diagnostics.putArray("excludedWorldNoteTitles");
            for (WorldNote note : notes) {
                if (!relevantNotes.contains(note)) {
                    excludedWorldNoteIds.add(note.getId());
                    excludedWorldNoteTitles.add(note.getTitle());
                }
            }
            if (!relevantNotes.isEmpty()) {
                ctx.append("### REGRAS DO MUNDO E FATOS ESTABELECIDOS\n");
                int noteChars = 0;
                for (WorldNote note : relevantNotes) {
                    StringBuilder nb = new StringBuilder();
                    nb.append("Título: ").append(note.getTitle()).append("\n");
                    nb.append("Conteúdo: ").append(note.getContent()).append("\n\n");
                    ctx.append(nb);
                    noteChars += nb.length();
                }
                diagnostics.put("worldNoteChars", noteChars);
            }
        }

        // MEMÓRIA RECENTE DE CAPÍTULOS
        if (currentChapter != null && currentChapter.getChapterOrder() != null && budget.getMaxPreviousChapters() > 0) {
            List<Chapter> previous = chapterRepository.findByStoryIdOrderByChapterOrderAsc(story.getId());
            final Integer order = currentChapter.getChapterOrder();
            List<Chapter> validPrev = previous.stream()
                .filter(p -> p.getChapterOrder() != null && p.getChapterOrder() < order)
                .collect(Collectors.toList());
            
            if (!validPrev.isEmpty()) {
                ctx.append("### CAPÍTULOS ANTERIORES (RESUMO RECENTE)\n");
                int startIdx = Math.max(0, validPrev.size() - budget.getMaxPreviousChapters());
                int prevChars = 0;
                int currentSummaryCount = 0;
                int fallbackCount = 0;
                int missingMemoryCount = 0;
                var previousChapterMemorySources = diagnostics.putArray("previousChapterMemorySources");
                for (int i = startIdx; i < validPrev.size(); i++) {
                    Chapter p = validPrev.get(i);
                    StringBuilder pb = new StringBuilder();
                    pb.append("Capítulo ").append(p.getChapterOrder()).append(" (").append(p.getTitle()).append("):\n");
                    if (ChapterMemorySupport.hasCurrentSummary(p)) {
                        pb.append(p.getSummary()).append("\n\n");
                        currentSummaryCount++;
                        previousChapterMemorySources.addObject()
                                .put("chapterId", p.getId())
                                .put("chapterOrder", p.getChapterOrder())
                                .put("source", "CURRENT_SUMMARY");
                    } else if (p.getContent() != null && !p.getContent().isBlank()) {
                        String fallback = ChapterMemorySupport.buildFallbackExcerpt(p.getContent());
                        pb.append("(MEMÓRIA AUTOMÁTICA AUSENTE OU DESATUALIZADA. ")
                                .append("Trechos factuais temporários do capítulo:)\n")
                                .append(fallback)
                                .append("\n\n");
                        fallbackCount++;
                        previousChapterMemorySources.addObject()
                                .put("chapterId", p.getId())
                                .put("chapterOrder", p.getChapterOrder())
                                .put("source", "FACTUAL_EXCERPT_FALLBACK");
                    } else {
                        pb.append("(Sem conteúdo)\n\n");
                        missingMemoryCount++;
                        previousChapterMemorySources.addObject()
                                .put("chapterId", p.getId())
                                .put("chapterOrder", p.getChapterOrder())
                                .put("source", "MISSING");
                    }
                    ctx.append(pb);
                    prevChars += pb.length();
                }
                diagnostics.put("previousChapterCountConsidered", validPrev.size() - startIdx);
                diagnostics.put("previousChapterSummaryCount", currentSummaryCount);
                diagnostics.put("previousChapterFallbackCount", fallbackCount);
                diagnostics.put("previousChapterMissingMemoryCount", missingMemoryCount);
                diagnostics.put("previousChapterChars", prevChars);
            }
        }

        // TRUNCAMENTO FINAL (Safe fallback global por character)
        String finalContext = ctx.toString();
        int totalChars = finalContext.length();
        int maxAllowedChars = tokenEstimator.tokensToChars(budget.getMaxExceptionalTokens());
        if (totalChars > maxAllowedChars) {
            int headChars = (int) (maxAllowedChars * 0.6);
            int tailChars = maxAllowedChars - headChars;
            finalContext = finalContext.substring(0, headChars)
                    + "\n...[CONTEXTO INTERMEDIÁRIO TRUNCADO]...\n"
                    + finalContext.substring(finalContext.length() - tailChars);
            diagnostics.put("contextTruncated", true);
            diagnostics.put("truncatedSections", "GLOBAL_MIDDLE");
            diagnostics.put(
                    "previousChapterMemoryMayBeTruncated",
                    diagnostics.path("previousChapterCountConsidered").asInt(0) > 0
            );
        } else {
            diagnostics.put("contextTruncated", false);
            diagnostics.put("previousChapterMemoryMayBeTruncated", false);
        }

        diagnostics.put("estimatedInputTokens", tokenEstimator.estimateTokens(finalContext));
        diagnostics.put("maxInputTokens", budget.getMaxExceptionalTokens());
        diagnostics.put("outputLimit", budget.getMaxOutputTokens());

        return new ContextResult(finalContext, diagnostics);
    }

    private List<StoryCharacter> selectRelevantCharacters(List<StoryCharacter> all, NovelAiRequest req, String content, int limit) {
        String searchSpace = buildSearchSpace(req, content);
        
        List<Integer> explicitIds = req.characterIds() != null ? req.characterIds() : new ArrayList<>();
        
        // 1. IDs explicitos, 2. Mencionados no espaço, 3. Protagonistas
        return all.stream()
            .map(character -> new RankedCharacter(
                    character,
                    characterRelevance(character, explicitIds, searchSpace)
            ))
            .filter(ranked -> ranked.rank() < Integer.MAX_VALUE)
            .sorted(
                    Comparator.comparingInt(RankedCharacter::rank)
                            .thenComparing(
                                    ranked -> ranked.character().getId(),
                                    Comparator.nullsLast(Integer::compareTo)
                            )
            )
            .limit(limit)
            .map(RankedCharacter::character)
            .collect(Collectors.toList());
    }

    private List<WorldNote> selectRelevantNotes(List<WorldNote> all, NovelAiRequest req, String content, int limit) {
        String searchSpace = buildSearchSpace(req, content);
        
        List<Integer> explicitIds = req.worldNoteIds() != null ? req.worldNoteIds() : new ArrayList<>();
        
        return all.stream()
            .map(note -> new RankedWorldNote(
                    note,
                    worldNoteRelevance(note, explicitIds, searchSpace)
            ))
            .filter(ranked -> ranked.rank() < Integer.MAX_VALUE)
            .sorted(
                    Comparator.comparingInt(RankedWorldNote::rank)
                            .thenComparing(
                                    ranked -> ranked.note().getId(),
                                    Comparator.nullsLast(Integer::compareTo)
                            )
            )
            .limit(limit)
            .map(RankedWorldNote::note)
            .collect(Collectors.toList());
    }

    private int characterRelevance(
            StoryCharacter character,
            List<Integer> explicitIds,
            String searchSpace
    ) {
        if (explicitIds.contains(character.getId())) {
            return 0;
        }
        if (containsNormalizedPhrase(searchSpace, character.getName())) {
            return 1;
        }
        if ("Protagonista".equalsIgnoreCase(character.getRole())
                || "Protagonista".equalsIgnoreCase(character.getNarrativeRole())) {
            return 2;
        }
        return Integer.MAX_VALUE;
    }

    private int worldNoteRelevance(
            WorldNote note,
            List<Integer> explicitIds,
            String searchSpace
    ) {
        if (explicitIds.contains(note.getId())) {
            return 0;
        }
        if (containsReferenceTerm(searchSpace, note.getTitle())
                || containsReferenceTerm(searchSpace, note.getTags())) {
            return 1;
        }
        return Integer.MAX_VALUE;
    }

    private String buildSearchSpace(NovelAiRequest request, String content) {
        String recentContent = content.length() > 4000
                ? content.substring(content.length() - 4000)
                : content;
        return normalizeForSearch(
                (request.instruction() != null ? request.instruction() : "")
                        + " "
                        + (request.selectedText() != null
                                ? request.selectedText()
                                : "")
                        + " "
                        + recentContent
        );
    }

    private boolean containsReferenceTerm(String searchSpace, String reference) {
        String normalizedReference = normalizeForSearch(reference);
        if (normalizedReference.isBlank()) {
            return false;
        }
        if (containsNormalizedPhrase(searchSpace, normalizedReference)) {
            return true;
        }
        for (String token : normalizedReference.split(" ")) {
            if (token.length() >= 3
                    && !REFERENCE_STOP_WORDS.contains(token)
                    && containsNormalizedPhrase(searchSpace, token)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNormalizedPhrase(String searchSpace, String value) {
        String normalizedValue = normalizeForSearch(value);
        return !normalizedValue.isBlank()
                && (" " + searchSpace + " ").contains(
                        " " + normalizedValue + " "
                );
    }

    private String normalizeForSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private record RankedCharacter(StoryCharacter character, int rank) {}

    private record RankedWorldNote(WorldNote note, int rank) {}
}
