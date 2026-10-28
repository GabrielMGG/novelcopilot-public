package com.novelcopilot.backend.benchmark;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AiBenchmarkSuiteValidationTest {

    private AiBenchmarkRunnerService runnerService;
    private AiBenchmarkRunRepository runRepository;
    
    @BeforeEach
    public void setup() {
        runRepository = mock(AiBenchmarkRunRepository.class);
        AiBenchmarkResponseRepository responseRepository = mock(AiBenchmarkResponseRepository.class);
        AiCostCalculator aiCostCalculator = mock(AiCostCalculator.class);
        AiUsageService aiUsageService = mock(AiUsageService.class);
        
        when(aiCostCalculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(BigDecimal.valueOf(0.01), "CALCULATED", null));

        when(runRepository.save(any(AiBenchmarkRun.class))).thenAnswer(i -> {
            AiBenchmarkRun run = i.getArgument(0);
            if (run.getId() == null) ReflectionTestUtils.setField(run, "id", 1L);
            return run;
        });

        AiBenchmarkParticipantProperties participantProperties = new AiBenchmarkParticipantProperties();
        AiBenchmarkParticipantProperties.Participant p1 = new AiBenchmarkParticipantProperties.Participant();
        p1.setEnabled(true); p1.setProvider("openrouter"); p1.setModel("qwen/qwen3-235b-a22b-2507");
        p1.setInputPricePerMillion(new BigDecimal("0.5")); p1.setOutputPricePerMillion(new BigDecimal("0.5"));
        
        AiBenchmarkParticipantProperties.Participant p2 = new AiBenchmarkParticipantProperties.Participant();
        p2.setEnabled(true); p2.setProvider("openrouter"); p2.setModel("mistralai/mistral-small-2603");
        p2.setInputPricePerMillion(new BigDecimal("0.5")); p2.setOutputPricePerMillion(new BigDecimal("0.5"));
        
        AiBenchmarkParticipantProperties.Participant p3 = new AiBenchmarkParticipantProperties.Participant();
        p3.setEnabled(true); p3.setProvider("openrouter"); p3.setModel("mistralai/mistral-small-3.2-24b-instruct");
        p3.setInputPricePerMillion(new BigDecimal("0.5")); p3.setOutputPricePerMillion(new BigDecimal("0.5"));

        AiBenchmarkParticipantProperties.Participant pLlama = new AiBenchmarkParticipantProperties.Participant();
        pLlama.setEnabled(true); pLlama.setProvider("groq"); pLlama.setModel("llama-3.3-70b-versatile");

        AiBenchmarkParticipantProperties.Participant pGptOss = new AiBenchmarkParticipantProperties.Participant();
        pGptOss.setEnabled(true); pGptOss.setProvider("groq"); pGptOss.setModel("openai/gpt-oss-120b");
        
        participantProperties.setParticipants(Map.of(
                "openrouter-qwen3-235b", p1,
                "openrouter-mistral-small-4", p2,
                "openrouter-mistral-small-3-2", p3,
                "groq-llama", pLlama,
                "groq-gpt-oss", pGptOss
        ));

        runnerService = new AiBenchmarkRunnerService(
                List.of(),
                runRepository,
                responseRepository,
                aiCostCalculator,
                aiUsageService,
                participantProperties
        );
        
        ReflectionTestUtils.setField(runnerService, "benchmarkEnabled", true);
        ReflectionTestUtils.setField(runnerService, "maxCasesPerRun", 20);
        ReflectionTestUtils.setField(runnerService, "maxRunCostUsd", new BigDecimal("10.0"));
    }

    @Test
    public void testCompatibilidadeRequiresExactlyThreeParticipants() {
        List<String> caseIds = List.of("case-compat-01", "case-compat-02", "case-compat-03", "case-compat-04", "case-compat-05", "case-compat-06", "case-compat-07", "case-compat-08");
        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Compatibilidade mínima NovelCopilot", caseIds, List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4"), "test-user")
        );
        assertTrue(e1.getMessage().contains("exige exatamente 3 participantes"));
    }

    @Test
    public void testCompatibilidadeRequiresExactlyEightCases() {
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2");
        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Compatibilidade mínima NovelCopilot", List.of("case-compat-01"), participants, "test-user")
        );
        assertTrue(e1.getMessage().contains("exige exatamente 8 casos"));
    }

    @Test
    public void testCompatibilidadeBlocksLlamaAndGptOss() {
        List<String> caseIds = List.of("case-compat-01", "case-compat-02", "case-compat-03", "case-compat-04", "case-compat-05", "case-compat-06", "case-compat-07", "case-compat-08");
        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Compatibilidade mínima NovelCopilot", caseIds, List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "groq-llama"), "test-user")
        );
        assertTrue(e1.getMessage().contains("Participante incompatível com a suíte de compatibilidade: groq-llama"));

        Exception e2 = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Compatibilidade mínima NovelCopilot", caseIds, List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "groq-gpt-oss"), "test-user")
        );
        assertTrue(e2.getMessage().contains("Participante incompatível com a suíte de compatibilidade: groq-gpt-oss"));
    }

    @Test
    public void testCompatibilidadeBlocksLiteraryCases() {
        List<String> caseIds = List.of("case-compat-01", "case-compat-02", "case-compat-03", "case-compat-04", "case-compat-05", "case-compat-06", "case-compat-07", "case-pt-01");
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2");
        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Compatibilidade mínima NovelCopilot", caseIds, participants, "test-user")
        );
        System.out.println("EXCEPTION IS: " + e1.getMessage());
        assertTrue(e1.getMessage().contains("case-pt-01") || e1.getMessage().contains("Participante") || e1.getMessage().contains("8 casos"));
    }

    @Test
    public void testCompatibilidadeSuccessWithValidCombination() {
        List<String> caseIds = List.of("case-compat-01", "case-compat-02", "case-compat-03", "case-compat-04", "case-compat-05", "case-compat-06", "case-compat-07", "case-compat-08");
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2");
        
        AiBenchmarkRun run = runnerService.runBenchmark("Compatibilidade mínima NovelCopilot", caseIds, participants, "test-user");
        assertNotNull(run);
        assertEquals("Compatibilidade mínima NovelCopilot", run.getSuiteName());
    }

    @Test
    public void testLiteraryDecisionRequiresExactlyTwelveCases() {
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2");
        List<String> caseIds = List.of("lit-dec-01");
        
        Exception e = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Benchmark literário decisório NovelCopilot", caseIds, participants, "test-user")
        );
        assertTrue(e.getMessage().contains("exige exatamente 12 casos"));
    }

    @Test
    public void testLiteraryDecisionBlocksLegacyCases() {
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2");
        List<String> caseIds = List.of("lit-dec-01", "lit-dec-02", "lit-dec-03", "lit-dec-04", "lit-dec-05", "lit-dec-06", "lit-dec-07", "lit-dec-08", "lit-dec-09", "lit-dec-10", "lit-dec-11", "case-pt-01");
        
        Exception e = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Benchmark literário decisório NovelCopilot", caseIds, participants, "test-user")
        );
        assertTrue(e.getMessage().contains("lit-dec-") || e.getMessage().toLowerCase().contains("liter"));
    }

    @Test
    public void testLiteraryDecisionRequiresThreeParticipants() {
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4");
        List<String> caseIds = List.of("lit-dec-01", "lit-dec-02", "lit-dec-03", "lit-dec-04", "lit-dec-05", "lit-dec-06", "lit-dec-07", "lit-dec-08", "lit-dec-09", "lit-dec-10", "lit-dec-11", "lit-dec-12");
        
        Exception e = assertThrows(IllegalArgumentException.class, () -> 
            runnerService.runBenchmark("Benchmark literário decisório NovelCopilot", caseIds, participants, "test-user")
        );
        assertTrue(e.getMessage().contains("exige exatamente 3 participantes"));
    }

    @Test
    public void testLiteraryDecisionSuccessWithValidCombination() {
        List<String> caseIds = List.of("lit-dec-01", "lit-dec-02", "lit-dec-03", "lit-dec-04", "lit-dec-05", "lit-dec-06", "lit-dec-07", "lit-dec-08", "lit-dec-09", "lit-dec-10", "lit-dec-11", "lit-dec-12");
        List<String> participants = List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2");
        
        AiBenchmarkRun run = runnerService.runBenchmark("Benchmark literário decisório NovelCopilot", caseIds, participants, "test-user");
        assertNotNull(run);
        assertEquals("Benchmark literário decisório NovelCopilot", run.getSuiteName());
    }
}
