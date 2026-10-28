package com.novelcopilot.backend.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AiBenchmarkFinalCasesValidationTest {

    private static final String FROZEN_FINAL_CASES_SHA256 =
            "09c0b8bb819f1793e6d9e0564ebe200947fe60a9736f2514b83882c776387943";

    @Test
    public void testFinalCasesValidation() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<AiBenchmarkCase> cases;
        
        try (InputStream is = new ClassPathResource("benchmark/benchmark-final-cases.json").getInputStream()) {
            cases = mapper.readValue(is, new TypeReference<List<AiBenchmarkCase>>() {});
        }

        // 9. existem exatamente 8 casos
        assertEquals(8, cases.size(), "Devem existir exatamente 8 casos.");

        long ptbrCount = cases.stream().filter(c -> "pt-BR".equals(c.getExpectedLanguage())).count();
        long enCount = cases.stream().filter(c -> "en".equals(c.getExpectedLanguage())).count();
        long esCount = cases.stream().filter(c -> "es".equals(c.getExpectedLanguage())).count();

        // 11. idiomas continuam 5 pt-BR, 2 en e 1 es
        assertEquals(5, ptbrCount);
        assertEquals(2, enCount);
        assertEquals(1, esCount);

        long fatosCount = cases.stream().filter(c -> c.getCategory().contains("Fatos")).count();
        long personagemCount = cases.stream().filter(c -> c.getCategory().contains("Personagem")).count();

        // 4 casos de Fatos e 4 de Personagem
        assertEquals(4, fatosCount);
        assertEquals(4, personagemCount);

        for (AiBenchmarkCase c : cases) {
            // Todos os IDs são lit-final-01 a lit-final-08
            assertTrue(c.getId().startsWith("lit-final-0"));
            
            // 10. todos os casos têm establishedFacts
            assertNotNull(c.getEstablishedFacts(), "Caso " + c.getId() + " deve ter establishedFacts");
            
            // 6 fatos estabelecidos
            String[] fatos = c.getEstablishedFacts().split("\n");
            assertTrue(fatos.length >= 6, "Caso " + c.getId() + " deve ter pelo menos 6 fatos estabelecidos");

            // 3 elementos obrigatórios
            assertNotNull(c.getMandatoryElements());
            assertTrue(c.getMandatoryElements().split("\n").length >= 3, "Caso " + c.getId() + " deve ter pelo menos 3 elementos obrigatórios");

            // 3 restrições
            assertNotNull(c.getRestrictions());
            assertTrue(c.getRestrictions().split("\n").length >= 3, "Caso " + c.getId() + " deve ter pelo menos 3 restrições");

            // approvalStatus = APROVADO
            assertEquals("APROVADO", c.getApprovalStatus());
        }

        // 1. lit-final-01 diferencia magia pessoal de infraestrutura mágica imperial
        AiBenchmarkCase case01 = cases.stream().filter(c -> "lit-final-01".equals(c.getId())).findFirst().orElseThrow();
        assertTrue(case01.getStoryContext().contains("uso pessoal de magia arcana foi banido"));
        assertTrue(case01.getStoryContext().contains("rede de iluminação alimentada por artefatos mágicos"));
        assertTrue(case01.getEstablishedFacts().contains("A rede imperial de iluminação mágica de Kaelen está desativada"));

        // 2. lit-final-02 contém "Rewrite and expand"
        AiBenchmarkCase case02 = cases.stream().filter(c -> "lit-final-02".equals(c.getId())).findFirst().orElseThrow();
        assertTrue(case02.getExactTask().contains("Rewrite and expand"));

        // 3. lit-final-04 proíbe explicitamente a senhora Blanco de ter subido
        AiBenchmarkCase case04 = cases.stream().filter(c -> "lit-final-04".equals(c.getId())).findFirst().orElseThrow();
        assertTrue(case04.getRestrictions().contains("La señora Blanco NO subió ni puede afirmar que subió"));

        // 4. lit-final-05 possui 6 fatos
        AiBenchmarkCase case05 = cases.stream().filter(c -> "lit-final-05".equals(c.getId())).findFirst().orElseThrow();
        assertEquals(6, case05.getEstablishedFacts().split("\n").length);

        // 5. lit-final-06 possui 6 fatos
        AiBenchmarkCase case06 = cases.stream().filter(c -> "lit-final-06".equals(c.getId())).findFirst().orElseThrow();
        assertEquals(6, case06.getEstablishedFacts().split("\n").length);

        // 6. lit-final-07 possui 6 fatos
        AiBenchmarkCase case07 = cases.stream().filter(c -> "lit-final-07".equals(c.getId())).findFirst().orElseThrow();
        assertEquals(6, case07.getEstablishedFacts().split("\n").length);

        // 7. lit-final-08 possui 6 fatos
        AiBenchmarkCase case08 = cases.stream().filter(c -> "lit-final-08".equals(c.getId())).findFirst().orElseThrow();
        assertEquals(6, case08.getEstablishedFacts().split("\n").length);

        // 8. lit-final-08 contém "Reescreva e expanda"
        assertTrue(case08.getExactTask().contains("Reescreva e expanda"));
    }

    @Test
    void finalCasesAreFrozenForNc014() throws Exception {
        byte[] content;
        try (InputStream is = new ClassPathResource(
                "benchmark/benchmark-final-cases.json"
        ).getInputStream()) {
            content = is.readAllBytes();
        }

        String actualHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );

        assertEquals(
                FROZEN_FINAL_CASES_SHA256,
                actualHash,
                "Os casos do NC-014 não podem mudar após o congelamento."
        );
    }
}
