package com.novelcopilot.backend.benchmark;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkEvaluation;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEvaluationService;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEvaluationService.EvaluationRequest;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService;
import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AiBenchmarkCompatEvaluationTest {

    private AiBenchmarkRunnerService runnerService;
    private AiBenchmarkEvaluationService evaluationService;
    private AiBenchmarkRunRepository runRepository;
    private AiBenchmarkResponseRepository responseRepository;
    private AiBenchmarkEvaluationRepository evaluationRepository;
    private List<AiBenchmarkResponse> savedResponses;
    private List<AiBenchmarkEvaluation> savedEvaluations;

    @BeforeEach
    public void setup() {
        runRepository = mock(AiBenchmarkRunRepository.class);
        responseRepository = mock(AiBenchmarkResponseRepository.class);
        evaluationRepository = mock(AiBenchmarkEvaluationRepository.class);
        AiCostCalculator aiCostCalculator = mock(AiCostCalculator.class);
        AiUsageService aiUsageService = mock(AiUsageService.class);

        savedResponses = new ArrayList<>();
        savedEvaluations = new ArrayList<>();

        when(aiCostCalculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(BigDecimal.valueOf(0.01), "CALCULATED", null));

        when(runRepository.save(any(AiBenchmarkRun.class))).thenAnswer(i -> {
            AiBenchmarkRun run = i.getArgument(0);
            if (run.getId() == null) ReflectionTestUtils.setField(run, "id", 1L);
            return run;
        });

        when(responseRepository.save(any(AiBenchmarkResponse.class))).thenAnswer(i -> {
            AiBenchmarkResponse r = i.getArgument(0);
            if (r.getId() == null) ReflectionTestUtils.setField(r, "id", (long) (savedResponses.size() + 1));
            savedResponses.add(r);
            return r;
        });

        when(responseRepository.findById(anyLong())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            return savedResponses.stream().filter(r -> r.getId().equals(id)).findFirst();
        });

        when(evaluationRepository.save(any(AiBenchmarkEvaluation.class))).thenAnswer(i -> {
            AiBenchmarkEvaluation e = i.getArgument(0);
            if (e.getId() == null) ReflectionTestUtils.setField(e, "id", (long) (savedEvaluations.size() + 1));
            savedEvaluations.add(e);
            return e;
        });
        
        when(evaluationRepository.findByResponseIdAndEvaluatorId(anyLong(), anyString())).thenReturn(Optional.empty());

        AiBenchmarkParticipantProperties participantProperties = new AiBenchmarkParticipantProperties();
        AiBenchmarkParticipantProperties.Participant p1 = new AiBenchmarkParticipantProperties.Participant();
        p1.setEnabled(true); p1.setProvider("mock-provider"); p1.setModel("qwen/qwen3-235b-a22b-2507");
        p1.setInputPricePerMillion(new BigDecimal("0.1")); p1.setOutputPricePerMillion(new BigDecimal("0.1"));
        
        AiBenchmarkParticipantProperties.Participant p2 = new AiBenchmarkParticipantProperties.Participant();
        p2.setEnabled(true); p2.setProvider("mock-provider"); p2.setModel("mistralai/mistral-small-2603");
        p2.setInputPricePerMillion(new BigDecimal("0.1")); p2.setOutputPricePerMillion(new BigDecimal("0.1"));

        AiBenchmarkParticipantProperties.Participant p3 = new AiBenchmarkParticipantProperties.Participant();
        p3.setEnabled(true); p3.setProvider("mock-provider"); p3.setModel("mistralai/mistral-small-3.2-24b-instruct");
        p3.setInputPricePerMillion(new BigDecimal("0.1")); p3.setOutputPricePerMillion(new BigDecimal("0.1"));

        participantProperties.setParticipants(Map.of(
                "openrouter-qwen3-235b", p1,
                "openrouter-mistral-small-4", p2,
                "openrouter-mistral-small-3-2", p3
        ));

        AiProvider mockProvider = mock(AiProvider.class);
        when(mockProvider.providerName()).thenReturn("mock-provider");
        when(mockProvider.generate(anyString(), anyString(), anyString())).thenReturn(
                new AiGenerationResult("Mock result", "mock-provider", "model", 10L, 5L, 20L, 30L, 100L)
        );

        runnerService = new AiBenchmarkRunnerService(
                List.of(mockProvider),
                runRepository,
                responseRepository,
                aiCostCalculator,
                aiUsageService,
                participantProperties
        ) {
            @Override
            public List<AiBenchmarkCase> getAvailableCases() {
                List<AiBenchmarkCase> cases = new ArrayList<>();
                for (int i = 1; i <= 8; i++) {
                    AiBenchmarkCase c = new AiBenchmarkCase();
                    c.setId("case-compat-" + i);
                    c.setCategory("Compatibilidade mínima NovelCopilot");
                    cases.add(c);
                }
                return cases;
            }
        };
        
        ReflectionTestUtils.setField(runnerService, "benchmarkEnabled", true);
        ReflectionTestUtils.setField(runnerService, "maxCasesPerRun", 20);
        ReflectionTestUtils.setField(runnerService, "maxRunCostUsd", new BigDecimal("10.0"));
        ReflectionTestUtils.setField(runnerService, "delayBetweenCallsMs", 1L); // speed up

        evaluationService = new AiBenchmarkEvaluationService(evaluationRepository, responseRepository);
    }

    @Test
    public void testCompatibilidadeFullPipeline() throws Exception {
        List<String> caseIds = List.of("case-compat-1", "case-compat-2", "case-compat-3", "case-compat-4", "case-compat-5", "case-compat-6", "case-compat-7", "case-compat-8");
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2");
        
        AiBenchmarkRun run = runnerService.runBenchmark("Compatibilidade mínima NovelCopilot", caseIds, participants, "test-user");
        
        // Wait for async execution without depending on machine speed.
        long deadline = System.currentTimeMillis() + 5_000;
        while (savedResponses.size() < caseIds.size() * participants.size()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }
        
        // 1. Check generated responses
        assertEquals(24, savedResponses.size(), "Must generate exactly 24 responses");
        
        // 2. Check each case has A, B, and C
        for (String cid : caseIds) {
            long countA = savedResponses.stream().filter(r -> r.getCaseId().equals(cid) && "A".equals(r.getAnonymousLabel())).count();
            long countB = savedResponses.stream().filter(r -> r.getCaseId().equals(cid) && "B".equals(r.getAnonymousLabel())).count();
            long countC = savedResponses.stream().filter(r -> r.getCaseId().equals(cid) && "C".equals(r.getAnonymousLabel())).count();
            
            assertEquals(1, countA);
            assertEquals(1, countB);
            assertEquals(1, countC);
        }
        
        // 3. Mock 24 evaluations independently
        List<EvaluationRequest> requests = new ArrayList<>();
        for (AiBenchmarkResponse r : savedResponses) {
            requests.add(new EvaluationRequest(
                r.getId(),
                null, null, null, null, null, null, null, null, null, null, "Justificativa compat", "atendeu"
            ));
        }
        
        evaluationService.evaluateCase(run.getId(), "case-compat-1", "admin", requests, ""); // preference is empty string
        
        // 4. Check 24 evaluations saved
        assertEquals(24, savedEvaluations.size(), "Must save 24 evaluations");
        
        // 5. Check preferredLabel/preference is NOT used for compatibility
        for (AiBenchmarkEvaluation eval : savedEvaluations) {
            assertNull(eval.getPreference(), "Preference should not be saved for compatibility");
            assertNotNull(eval.getCompatibilityStatus());
            assertNotNull(eval.getNotes());
        }
    }
}
