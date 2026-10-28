package com.novelcopilot.backend.service;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.novelcopilot.backend.entity.Chapter;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class ChapterMemorySupport {

    private static final int FALLBACK_TOTAL_CHARS = 2400;

    private ChapterMemorySupport() {
    }

    static boolean hasCurrentSummary(Chapter chapter) {
        return chapter != null
                && chapter.getSummary() != null
                && !chapter.getSummary().isBlank()
                && chapter.getSummaryContentHash() != null
                && chapter.getSummaryContentHash().equals(contentHash(chapter.getContent()));
    }

    static String contentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizePlainText(content).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    static String normalizePlainText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        return content
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("(?i)</div\\s*>", "\n")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\n *", "\n")
                .replaceAll(" +([.,;:!?])", "$1")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    static String buildFallbackExcerpt(String content) {
        String plainText = normalizePlainText(content);
        if (plainText.length() <= FALLBACK_TOTAL_CHARS) {
            return plainText;
        }

        int sectionSize = FALLBACK_TOTAL_CHARS / 3;
        int middleStart = Math.max(0, (plainText.length() - sectionSize) / 2);
        return plainText.substring(0, sectionSize)
                + "\n...[TRECHO INTERMEDIÁRIO]...\n"
                + plainText.substring(middleStart, middleStart + sectionSize)
                + "\n...[TRECHO FINAL]...\n"
                + plainText.substring(plainText.length() - sectionSize);
    }
}

@Service
class NarrativeLanguageDetector {

    static final double MINIMUM_RELATIVE_DISTANCE = 0.15;

    private final LanguageDetector detector;

    NarrativeLanguageDetector() {
        this.detector = LanguageDetectorBuilder
                .fromAllSpokenLanguages()
                .withLowAccuracyMode()
                .withMinimumRelativeDistance(MINIMUM_RELATIVE_DISTANCE)
                .build();
    }

    Optional<DetectionResult> detect(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Language language = detector.detectLanguageOf(text);
        if (language == Language.UNKNOWN) {
            return Optional.empty();
        }

        Map<Language, Double> confidenceValues = detector.computeLanguageConfidenceValues(text);
        Iterator<Double> values = confidenceValues.values().iterator();
        double highestConfidence = values.hasNext() ? values.next() : 1.0;
        double secondHighestConfidence = values.hasNext() ? values.next() : 0.0;
        double relativeDistance = Math.max(0.0, highestConfidence - secondHighestConfidence);

        String isoCode = language.getIsoCode639_1().toString().toLowerCase(Locale.ROOT);
        String instructionName = language.name().replace('_', ' ');

        return Optional.of(new DetectionResult(isoCode, instructionName, relativeDistance));
    }

    record DetectionResult(
            String isoCode,
            String instructionName,
            double relativeDistance
    ) {
    }
}
