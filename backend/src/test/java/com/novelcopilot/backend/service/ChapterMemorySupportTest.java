package com.novelcopilot.backend.service;

import com.novelcopilot.backend.entity.Chapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterMemorySupportTest {

    @Test
    void normalizesPersistedEditorHtmlBeforeMemoryUse() {
        String normalized = ChapterMemorySupport.normalizePlainText(
                "<p>Arkendu &amp; Kastor</p><p>evitam <strong>perigo</strong>.</p>"
        );

        assertEquals("Arkendu & Kastor\nevitam perigo.", normalized);
        assertFalse(normalized.contains("<p>"));
    }

    @Test
    void summaryIsCurrentOnlyWhileItsContentHashMatches() {
        Chapter chapter = new Chapter();
        chapter.setContent("Kastor evita riscos.");
        chapter.setSummary("Kastor evita riscos.");
        chapter.setSummaryContentHash(ChapterMemorySupport.contentHash(chapter.getContent()));

        assertTrue(ChapterMemorySupport.hasCurrentSummary(chapter));

        chapter.setContent("Kastor agora aceitou o caso.");
        assertFalse(ChapterMemorySupport.hasCurrentSummary(chapter));
    }

    @Test
    void fallbackPreservesBeginningMiddleAndEndOfLongChapter() {
        String content = "INICIO-ARKENDU " + "a".repeat(1800)
                + " MEIO-KASTOR " + "b".repeat(1800)
                + " FINAL-NAO-INVESTIGAR";

        String fallback = ChapterMemorySupport.buildFallbackExcerpt(content);

        assertTrue(fallback.contains("INICIO-ARKENDU"));
        assertTrue(fallback.contains("MEIO-KASTOR"));
        assertTrue(fallback.contains("FINAL-NAO-INVESTIGAR"));
        assertTrue(fallback.contains("TRECHO INTERMEDIÁRIO"));
        assertTrue(fallback.contains("TRECHO FINAL"));
    }
}
