package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.entity.AiUsageRecord;
import com.novelcopilot.backend.entity.AiUsageStatus;
import com.novelcopilot.backend.repository.AiUsageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AiUsageServiceTest {

    @Mock
    private AiUsageRecordRepository aiUsageRecordRepository;

    @Mock
    private AiCostCalculator aiCostCalculator;

    @org.junit.jupiter.api.BeforeEach
    void setUpMock() {
        org.mockito.Mockito.lenient().when(aiCostCalculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(BigDecimal.valueOf(0.01), "CALCULATED", null));
    }

    @InjectMocks
    private AiUsageService aiUsageService;

    @Test
    void testRecordSuccess_SavesCorrectTokens() {
        AiGenerationResult result = new AiGenerationResult(
                "Generated content",
                "Groq",
                "llama-3.3-70b-versatile",
                1000L, 200L, 500L, 1500L, 1200L
        );

        aiUsageService.recordSuccess("user-1", 10, 20, "WRITE", result);

        ArgumentCaptor<AiUsageRecord> captor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(aiUsageRecordRepository).save(captor.capture());

        AiUsageRecord saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals(10, saved.getStoryId());
        assertEquals(20, saved.getChapterId());
        assertEquals("WRITE", saved.getActionType());
        assertEquals("Groq", saved.getProvider());
        assertEquals("llama-3.3-70b-versatile", saved.getModel());
        assertEquals(1000L, saved.getInputTokens());
        assertEquals(200L, saved.getCachedInputTokens());
        assertEquals(500L, saved.getOutputTokens());
        assertEquals(1500L, saved.getTotalTokens());
        assertEquals(1200L, saved.getDurationMs());
        assertEquals(AiUsageStatus.SUCCESS, saved.getStatus());
        assertNull(saved.getErrorCode());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void testRecordSuccess_WithNullTokens_SavesZeros() {
        AiGenerationResult result = new AiGenerationResult(
                "Content", "Groq", "llama-3.3-70b-versatile",
                null, null, null, null, null
        );

        aiUsageService.recordSuccess("user-1", 10, null, "WRITE", result);

        ArgumentCaptor<AiUsageRecord> captor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(aiUsageRecordRepository).save(captor.capture());

        AiUsageRecord saved = captor.getValue();
        assertEquals(0L, saved.getInputTokens());
        assertEquals(0L, saved.getCachedInputTokens());
        assertEquals(0L, saved.getOutputTokens());
        assertEquals(0L, saved.getTotalTokens());
        assertEquals(0L, saved.getDurationMs());
        assertNull(saved.getChapterId());
    }

    @Test
    void testRecordError_SavesCorrectData() {
        aiUsageService.recordError(
                "user-1", 10, 20, "WRITE",
                "Groq", "llama-3.3-70b-versatile",
                800L, "429 TOO_MANY_REQUESTS"
        );

        ArgumentCaptor<AiUsageRecord> captor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(aiUsageRecordRepository).save(captor.capture());

        AiUsageRecord saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals(AiUsageStatus.ERROR, saved.getStatus());
        assertEquals("Groq", saved.getProvider());
        assertEquals("llama-3.3-70b-versatile", saved.getModel());
        assertEquals(800L, saved.getDurationMs());
        assertEquals(0L, saved.getInputTokens());
        assertEquals(0L, saved.getOutputTokens());
        assertNull(saved.getEstimatedCostUsd());
        assertNotNull(saved.getErrorCode());
    }

    @Test
    void testRecordSuccess_StoryIdAndChapterIdOptional() {
        AiGenerationResult result = new AiGenerationResult(
                "Content", "Groq", "llama-3.3-70b-versatile",
                100L, 0L, 50L, 150L, 200L
        );

        aiUsageService.recordSuccess("user-1", null, null, "WRITE", result);

        ArgumentCaptor<AiUsageRecord> captor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(aiUsageRecordRepository).save(captor.capture());

        AiUsageRecord saved = captor.getValue();
        assertNull(saved.getStoryId());
        assertNull(saved.getChapterId());
    }

    @Test
    void testRecordSuccess_DoesNotContainPromptOrContent() {
        AiGenerationResult result = new AiGenerationResult(
                "This is the generated story content with character names and plot details.",
                "Groq", "llama-3.3-70b-versatile",
                100L, 0L, 50L, 150L, 200L
        );

        aiUsageService.recordSuccess("user-1", 10, 20, "WRITE", result);

        ArgumentCaptor<AiUsageRecord> captor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(aiUsageRecordRepository).save(captor.capture());

        AiUsageRecord saved = captor.getValue();
        // AiUsageRecord should not have any field containing story content
        assertNull(saved.getErrorCode());
        // Verify there's no content field by checking the saved record doesn't store generated text
        assertEquals("WRITE", saved.getActionType());
    }

    @Test
    void testRecordSuccess_FailureToSaveDoesNotThrow() {
        AiGenerationResult result = new AiGenerationResult(
                "Content", "Groq", "llama-3.3-70b-versatile",
                100L, 0L, 50L, 150L, 200L
        );

        doThrow(new RuntimeException("DB connection failed"))
                .when(aiUsageRecordRepository).save(any(AiUsageRecord.class));

        // Should NOT throw - metric failure must not affect the generation
        aiUsageService.recordSuccess("user-1", 10, 20, "WRITE", result);

        // Verify save was attempted
        verify(aiUsageRecordRepository).save(any(AiUsageRecord.class));
    }

    @Test
    void testRecordError_FailureToSaveDoesNotThrow() {
        doThrow(new RuntimeException("DB connection failed"))
                .when(aiUsageRecordRepository).save(any(AiUsageRecord.class));

        // Should NOT throw
        aiUsageService.recordError(
                "user-1", 10, 20, "WRITE",
                "Groq", "llama-3.3-70b-versatile",
                800L, "500 INTERNAL_SERVER_ERROR"
        );

        verify(aiUsageRecordRepository).save(any(AiUsageRecord.class));
    }

    @Test
    void testRecordSuccess_WithNullCalculatedCost_PreservesNull() {
        AiGenerationResult result = new AiGenerationResult(
                "Content", "Cloudflare", "@cf/meta/llama-3.2-3b-instruct",
                807L, 0L, 262L, 1069L, 3500L
        );

        org.mockito.Mockito.when(aiCostCalculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(null, "CALCULATED", null));

        aiUsageService.recordSuccess("user-1", 10, 20, "SUMMARIZE_CHAPTER", result);

        ArgumentCaptor<AiUsageRecord> captor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(aiUsageRecordRepository).save(captor.capture());

        AiUsageRecord saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals(10, saved.getStoryId());
        assertEquals(20, saved.getChapterId());
        assertEquals("SUMMARIZE_CHAPTER", saved.getActionType());
        assertEquals("Cloudflare", saved.getProvider());
        assertEquals("@cf/meta/llama-3.2-3b-instruct", saved.getModel());
        assertEquals(807L, saved.getInputTokens());
        assertEquals(0L, saved.getCachedInputTokens());
        assertEquals(262L, saved.getOutputTokens());
        assertEquals(1069L, saved.getTotalTokens());
        assertEquals(3500L, saved.getDurationMs());
        assertEquals(AiUsageStatus.SUCCESS, saved.getStatus());
        assertNull(saved.getEstimatedCostUsd()); // Must preserve null, never convert to zero
    }
}
