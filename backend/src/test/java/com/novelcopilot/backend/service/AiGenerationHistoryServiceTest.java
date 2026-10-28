package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationHistoryItem;
import com.novelcopilot.backend.dto.NovelAiAction;
import com.novelcopilot.backend.entity.AiGenerationHistory;
import com.novelcopilot.backend.entity.AiGenerationUsage;
import com.novelcopilot.backend.repository.AiGenerationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGenerationHistoryServiceTest {

    @Mock
    private AiGenerationHistoryRepository repository;

    private AiGenerationHistoryService service;

    @BeforeEach
    void setUp() {
        service = new AiGenerationHistoryService(repository);
    }

    @Test
    void recordsSuccessfulProseWithChapterOwnershipFields() {
        AiGenerationUsage usage = usage();

        service.record(usage, NovelAiAction.CONTINUE_SCENE, "Texto gerado.", null);

        ArgumentCaptor<AiGenerationHistory> captor = ArgumentCaptor.forClass(AiGenerationHistory.class);
        verify(repository).save(captor.capture());
        AiGenerationHistory saved = captor.getValue();
        assertEquals(91L, saved.getUsageId());
        assertEquals("user-1", saved.getUserId());
        assertEquals(21, saved.getStoryId());
        assertEquals(50, saved.getChapterId());
        assertEquals("CONTINUE_SCENE", saved.getActionType());
        assertEquals("PROSE", saved.getOutputType());
        assertEquals("Texto gerado.", saved.getContent());
        assertNull(saved.getStructuredOutput());
    }

    @Test
    void excludesInlineContinuationFromPermanentHistory() {
        service.record(usage(), NovelAiAction.INLINE_CONTINUATION, "Trecho curto", null);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotStoreGenerationWithoutChapter() {
        AiGenerationUsage usage = usage();
        usage.setChapterId(null);

        service.record(usage, NovelAiAction.CONTINUE_SCENE, "Texto", null);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listsOnlyTheRequestedUserStoryAndChapter() {
        AiGenerationHistory history = new AiGenerationHistory();
        history.setActionType("PLAN_SCENE");
        history.setOutputType("PROSE");
        history.setContent("Plano da cena");
        history.setProvider("cloudflare");
        history.setModel("gemma");
        history.setCreatedAt(LocalDateTime.of(2026, 8, 2, 10, 0));
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.findByUserIdAndStoryIdAndChapterId("user-1", 21, 50, pageable))
                .thenReturn(new PageImpl<>(List.of(history), pageable, 1));

        Page<AiGenerationHistoryItem> result = service.list("user-1", 21, 50, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Plano da cena", result.getContent().get(0).content());
        verify(repository).findByUserIdAndStoryIdAndChapterId("user-1", 21, 50, pageable);
    }

    private AiGenerationUsage usage() {
        AiGenerationUsage usage = new AiGenerationUsage();
        usage.setId(91L);
        usage.setUserId("user-1");
        usage.setStoryId(21);
        usage.setChapterId(50);
        usage.setProvider("cloudflare");
        usage.setModel("gemma");
        usage.setCreatedAt(LocalDateTime.of(2026, 8, 2, 10, 0));
        return usage;
    }
}
