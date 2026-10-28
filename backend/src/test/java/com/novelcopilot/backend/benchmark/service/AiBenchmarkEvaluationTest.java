package com.novelcopilot.backend.benchmark;

import com.novelcopilot.backend.benchmark.entity.AiBenchmarkEvaluation;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiBenchmarkEvaluationTest {

    @Mock
    private AiBenchmarkEvaluationRepository evaluationRepository;

    @Mock
    private AiBenchmarkResponseRepository responseRepository;

    @InjectMocks
    private AiBenchmarkEvaluationService evaluationService;

    @Test
    void testEvaluationSavesAndUpdatesCorrectly() {
        AiBenchmarkResponse r1 = new AiBenchmarkResponse();
        r1.setId(10L);
        when(responseRepository.findById(10L)).thenReturn(Optional.of(r1));
        
        when(evaluationRepository.findByResponseIdAndEvaluatorId(10L, "admin")).thenReturn(Optional.empty());
        
        evaluationService.saveEvaluation(10L, "admin", 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, "Good", "A");
        
        verify(evaluationRepository, times(1)).save(any(AiBenchmarkEvaluation.class));

        // Test Update
        AiBenchmarkEvaluation existing = new AiBenchmarkEvaluation();
        existing.setId(100L);
        when(evaluationRepository.findByResponseIdAndEvaluatorId(10L, "admin")).thenReturn(Optional.of(existing));

        evaluationService.saveEvaluation(10L, "admin", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, "Bad", "B");
        
        ArgumentCaptor<AiBenchmarkEvaluation> captor = ArgumentCaptor.forClass(AiBenchmarkEvaluation.class);
        verify(evaluationRepository, times(2)).save(captor.capture());
        
        AiBenchmarkEvaluation saved = captor.getValue();
        assertEquals(100L, saved.getId(), "Should update existing entity");
        assertEquals("B", saved.getPreference(), "Should update preference");
    }

    @Test
    void testInvalidScoreThrowsException() {
        AiBenchmarkResponse r1 = new AiBenchmarkResponse();
        r1.setId(10L);
        when(responseRepository.findById(10L)).thenReturn(Optional.of(r1));
        
        assertThrows(IllegalArgumentException.class, () -> {
            evaluationService.saveEvaluation(10L, "admin", 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, "", "A");
        });
    }
}
