package com.novelcopilot.backend.benchmark.service;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiBenchmarkNc014RetestTest {

    private AiBenchmarkRunnerService runner;

    @BeforeEach
    void setUp() {
        AiBenchmarkParticipantProperties properties = new AiBenchmarkParticipantProperties();
        Map<String, AiBenchmarkParticipantProperties.Participant> participants = new LinkedHashMap<>();
        participants.put("groq-qwen-3-6-27b", participant("groq", "qwen/qwen3.6-27b"));
        participants.put("cloudflare-gemma", participant("cloudflare", "@cf/google/gemma-4-26b-a4b-it"));
        participants.put("cloudflare-llama4-scout", participant("cloudflare", "@cf/meta/llama-4-scout-17b-16e-instruct"));
        properties.setParticipants(participants);

        AiBenchmarkRunRepository runs = mock(AiBenchmarkRunRepository.class);
        when(runs.findAll()).thenReturn(List.of());
        when(runs.save(any(AiBenchmarkRun.class))).thenAnswer(invocation -> {
            AiBenchmarkRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", 1L);
            }
            return run;
        });

        AiCostCalculator calculator = mock(AiCostCalculator.class);
        when(calculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new AiCostCalculator.CalculationResult(new BigDecimal("0.01"), "CALCULATED", null));

        runner = new AiBenchmarkRunnerService(
                List.of(),
                runs,
                mock(AiBenchmarkResponseRepository.class),
                calculator,
                mock(AiUsageService.class),
                properties
        );
        ReflectionTestUtils.setField(runner, "benchmarkEnabled", true);
        ReflectionTestUtils.setField(runner, "maxCasesPerRun", 20);
        ReflectionTestUtils.setField(runner, "maxRunCostUsd", new BigDecimal("1.00"));
        ReflectionTestUtils.setField(runner, "maxDailyCostUsd", new BigDecimal("3.00"));
        ReflectionTestUtils.setField(runner, "maxRetries", 0);
        ReflectionTestUtils.setField(runner, "delayBetweenCallsMs", 0L);
    }

    @Test
    void frozenCaseContainsChapterMemoriesCharacterCardAndWorldCard() {
        AiBenchmarkCase testCase = nc014Case();
        String prompt = runner.buildPrompt(testCase);

        assertTrue(prompt.contains("Card de Mundo Relevante"));
        assertTrue(prompt.contains("Divisão de Saneamento e Inquérito (DSI)"));
        assertTrue(prompt.contains("CARD DE PERSONAGEM — Arkendu"));
        assertTrue(prompt.contains("MEMÓRIA APROVADA — CAPÍTULO 1"));
        assertTrue(prompt.contains("MEMÓRIA APROVADA — CAPÍTULO 2"));
        assertTrue(prompt.contains("evidências de ritualismo"));
        assertTrue(prompt.contains("não demonstrou decisão de investigar"));
        assertEquals(64, runner.promptSha256(testCase).length());
        assertEquals(runner.promptSha256(testCase), runner.promptSha256(testCase));
    }

    @Test
    void suiteRejectsAnyParticipantSetOtherThanTheThreeFrozenCandidates() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> runner.runBenchmark(
                AiBenchmarkRunnerService.NC014_RETEST_SUITE,
                List.of("nc014-retest-arkendu-01"),
                List.of("groq-qwen-3-6-27b", "cloudflare-gemma"),
                "admin"
        ));

        assertTrue(error.getMessage().contains("Qwen 3.6, Gemma 4 e Llama 4 Scout"));
    }

    private AiBenchmarkCase nc014Case() {
        return runner.getAvailableCases().stream()
                .filter(item -> "nc014-retest-arkendu-01".equals(item.getId()))
                .findFirst()
                .orElseThrow();
    }

    private static AiBenchmarkParticipantProperties.Participant participant(String provider, String model) {
        AiBenchmarkParticipantProperties.Participant participant = new AiBenchmarkParticipantProperties.Participant();
        participant.setEnabled(true);
        participant.setProvider(provider);
        participant.setModel(model);
        participant.setInputPricePerMillion(new BigDecimal("0.10"));
        participant.setCachedInputPricePerMillion(new BigDecimal("0.10"));
        participant.setOutputPricePerMillion(new BigDecimal("0.30"));
        return participant;
    }
}
