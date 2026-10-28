package com.novelcopilot.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NarrativeLanguageDetectorTest {

    private final NarrativeLanguageDetector detector = new NarrativeLanguageDetector();

    @Test
    void detectsEnglishPortugueseAndSpanishChapterText() {
        assertLanguage("en", """
                Lena and Tomas had worked together for three years. Tomas refused to enter the abandoned station
                because he avoided unnecessary risks. They remained in the archive and did not go to the station.
                """);
        assertLanguage("pt", """
                Lena e Tomas trabalhavam juntos havia três anos. Tomas recusou-se a entrar na estação abandonada
                porque evitava riscos desnecessários. Eles permaneceram no arquivo e não foram até a estação.
                """);
        assertLanguage("es", """
                Lena y Tomas trabajaban juntos desde hacía tres años. Tomas se negó a entrar en la estación abandonada
                porque evitaba riesgos innecesarios. Permanecieron en el archivo y no fueron a la estación.
                """);
    }

    @Test
    void rejectsAmbiguousTextInsteadOfGuessing() {
        assertTrue(detector.detect("Prologue").isEmpty());
    }

    private void assertLanguage(String expectedIsoCode, String text) {
        NarrativeLanguageDetector.DetectionResult result = detector.detect(text).orElseThrow();
        assertEquals(expectedIsoCode, result.isoCode());
        assertTrue(result.relativeDistance() >= NarrativeLanguageDetector.MINIMUM_RELATIVE_DISTANCE);
    }
}
