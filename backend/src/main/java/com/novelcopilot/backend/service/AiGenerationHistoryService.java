package com.novelcopilot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcopilot.backend.dto.AiGenerationHistoryItem;
import com.novelcopilot.backend.dto.NovelAiAction;
import com.novelcopilot.backend.entity.AiGenerationHistory;
import com.novelcopilot.backend.entity.AiGenerationUsage;
import com.novelcopilot.backend.repository.AiGenerationHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AiGenerationHistoryService {

    private final AiGenerationHistoryRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiGenerationHistoryService(AiGenerationHistoryRepository repository) {
        this.repository = repository;
    }

    public void record(
            AiGenerationUsage usage,
            NovelAiAction action,
            String content,
            JsonNode structuredResult
    ) {
        if (!shouldStore(action, usage.getStoryId(), usage.getChapterId())) {
            return;
        }

        AiGenerationHistory history = new AiGenerationHistory();
        history.setUsageId(usage.getId());
        history.setUserId(usage.getUserId());
        history.setStoryId(usage.getStoryId());
        history.setChapterId(usage.getChapterId());
        history.setActionType(action.name());
        history.setOutputType(structuredResult == null ? "PROSE" : "STRUCTURED");
        history.setContent(content);
        history.setStructuredOutput(structuredResult == null ? null : structuredResult.toString());
        history.setProvider(usage.getProvider());
        history.setModel(usage.getModel());
        history.setCreatedAt(usage.getCreatedAt());
        repository.save(history);
    }

    public Page<AiGenerationHistoryItem> list(
            String userId,
            Integer storyId,
            Integer chapterId,
            Pageable pageable
    ) {
        return repository.findByUserIdAndStoryIdAndChapterId(userId, storyId, chapterId, pageable)
                .map(this::toItem);
    }

    public Optional<AiGenerationHistoryItem> findByUsageId(Long usageId) {
        return repository.findByUsageId(usageId).map(this::toItem);
    }

    private boolean shouldStore(NovelAiAction action, Integer storyId, Integer chapterId) {
        return action != NovelAiAction.INLINE_CONTINUATION
                && storyId != null
                && chapterId != null;
    }

    private AiGenerationHistoryItem toItem(AiGenerationHistory history) {
        JsonNode structuredResult = null;
        if (history.getStructuredOutput() != null) {
            try {
                structuredResult = objectMapper.readTree(history.getStructuredOutput());
            } catch (Exception ignored) {
                structuredResult = null;
            }
        }

        return new AiGenerationHistoryItem(
                history.getId(), history.getUsageId(),
                history.getActionType(),
                history.getOutputType(),
                history.getContent(),
                structuredResult,
                history.getProvider(),
                history.getModel(),
                history.getCreatedAt()
        );
    }
}
