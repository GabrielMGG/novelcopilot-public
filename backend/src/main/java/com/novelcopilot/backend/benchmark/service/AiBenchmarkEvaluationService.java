package com.novelcopilot.backend.benchmark.service;

import com.novelcopilot.backend.benchmark.entity.AiBenchmarkEvaluation;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class AiBenchmarkEvaluationService {

    private final AiBenchmarkEvaluationRepository evaluationRepository;
    private final AiBenchmarkResponseRepository responseRepository;

    public AiBenchmarkEvaluationService(
            AiBenchmarkEvaluationRepository evaluationRepository,
            AiBenchmarkResponseRepository responseRepository
    ) {
        this.evaluationRepository = evaluationRepository;
        this.responseRepository = responseRepository;
    }

    public AiBenchmarkEvaluation saveEvaluation(
            Long responseId, 
            String evaluatorId,
            int literaryQuality,
            int contextCoherence,
            int instructionFollowing,
            int languageNaturalness,
            int repetitionControl,
            int stylePreservation,
            int factPreservation,
            int characterPreservation,
            int narrativeProgression,
            int noContradictions,
            String notes,
            String preference
    ) {
        AiBenchmarkResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("Response not found"));

        validateScore(literaryQuality);
        validateScore(contextCoherence);
        validateScore(instructionFollowing);
        validateScore(languageNaturalness);
        validateScore(repetitionControl);
        validateScore(stylePreservation);
        validateScore(factPreservation);
        validateScore(characterPreservation);
        validateScore(narrativeProgression);
        validateScore(noContradictions);

        AiBenchmarkEvaluation eval = evaluationRepository.findByResponseIdAndEvaluatorId(responseId, evaluatorId)
                .orElseGet(AiBenchmarkEvaluation::new);
        
        eval.setResponseId(responseId);
        eval.setEvaluatorId(evaluatorId);
        eval.setLiteraryQuality(literaryQuality);
        eval.setContextCoherence(contextCoherence);
        eval.setInstructionFollowing(instructionFollowing);
        eval.setLanguageNaturalness(languageNaturalness);
        eval.setRepetitionControl(repetitionControl);
        eval.setStylePreservation(stylePreservation);
        eval.setFactPreservation(factPreservation);
        eval.setCharacterPreservation(characterPreservation);
        eval.setNarrativeProgression(narrativeProgression);
        eval.setNoContradictions(noContradictions);
        
        BigDecimal overall = calculateWeightedScore(
                literaryQuality,
                contextCoherence,
                instructionFollowing,
                languageNaturalness,
                repetitionControl,
                stylePreservation,
                factPreservation,
                characterPreservation,
                narrativeProgression,
                noContradictions
        );
        eval.setOverallScore(overall);
        eval.setNotes(notes);
        eval.setPreference(preference);
        eval.setCreatedAt(LocalDateTime.now());
        
        return evaluationRepository.save(eval);
    }

    public AiBenchmarkEvaluation saveCompatibilityEvaluation(
            Long responseId, 
            String evaluatorId,
            String compatibilityStatus,
            String notes
    ) {
        AiBenchmarkResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("Response not found"));

        if (!"atendeu".equals(compatibilityStatus) && (notes == null || notes.isBlank())) {
            throw new IllegalArgumentException("Comentario obrigatorio quando a avaliacao nao for 'atendeu'");
        }

        AiBenchmarkEvaluation eval = evaluationRepository.findByResponseIdAndEvaluatorId(responseId, evaluatorId)
                .orElseGet(AiBenchmarkEvaluation::new);
        
        eval.setResponseId(responseId);
        eval.setEvaluatorId(evaluatorId);
        eval.setCompatibilityStatus(compatibilityStatus);
        eval.setNotes(notes);
        eval.setCreatedAt(LocalDateTime.now());
        
        return evaluationRepository.save(eval);
    }

    @Transactional
    public void evaluateCase(
            Long runId,
            String caseId,
            String evaluatorId,
            java.util.List<EvaluationRequest> requests,
            String preference
    ) {
        for (EvaluationRequest request : requests) {
            if (request.compatibilityStatus() != null && !request.compatibilityStatus().isEmpty()) {
                saveCompatibilityEvaluation(request.responseId(), evaluatorId, request.compatibilityStatus(), request.notes());
            } else {
                saveEvaluation(request.responseId(), evaluatorId, request.literaryQuality(), request.contextCoherence(),
                        request.instructionFollowing(), request.languageNaturalness(), request.repetitionControl(), request.stylePreservation(), 
                        request.factPreservation(), request.characterPreservation(), request.narrativeProgression(), request.noContradictions(), 
                        request.notes(), preference);
            }
        }
    }

    public record EvaluationRequest(
        Long responseId,
        Integer literaryQuality,
        Integer contextCoherence,
        Integer instructionFollowing,
        Integer languageNaturalness,
        Integer repetitionControl,
        Integer stylePreservation,
        Integer factPreservation,
        Integer characterPreservation,
        Integer narrativeProgression,
        Integer noContradictions,
        String notes,
        String compatibilityStatus
    ) {}

    private BigDecimal calculateWeightedScore(
            int ql, int cc, int inf, int ln, int rc, int sp, int fp, int cp, int np, int nc
    ) {
        // cumprimento da instrução: 20%; coerência com o contexto: 15%; qualidade geral: 15%;
        // preservação de fatos: 10%; preservação de personagem: 10%; naturalidade do idioma: 10%;
        // progressão narrativa: 8%; estilo e voz: 5%; controle de repetição: 4%; ausência de contradições: 3%.
        double score = (inf * 0.20) + 
                       (cc * 0.15) + 
                       (ql * 0.15) + 
                       (fp * 0.10) + 
                       (cp * 0.10) + 
                       (ln * 0.10) + 
                       (np * 0.08) + 
                       (sp * 0.05) + 
                       (rc * 0.04) + 
                       (nc * 0.03);
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateScore(Integer score) {
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }
    }
}
