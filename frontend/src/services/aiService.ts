import type { GenerateAiPayload, GenerateAiResponse, AiGenerateResult, AiReviewFinding, SuggestContinuationOption } from "../types/ai";
import { generateNovelAi, estimateNovelAi } from "./novelAiApi";
import type { NovelAiRequest } from "./novelAiApi";
import type { AiEstimateResponse } from "../types/credits";
import { getChapterAiHistory } from "./aiHistoryService";

function isSuggestionOption(value: unknown): value is SuggestContinuationOption {
  if (!value || typeof value !== "object") {
    return false;
  }

  const option = value as Record<string, unknown>;
  return (
    (option.type === "safe"
      || option.type === "unexpected"
      || option.type === "intense")
    && typeof option.title === "string"
    && option.title.trim() !== ""
    && typeof option.summary === "string"
    && option.summary.trim() !== ""
    && typeof option.snippet === "string"
    && option.snippet.trim() !== ""
  );
}

function parseReviewFinding(value: unknown): AiReviewFinding | null {
  if (!value || typeof value !== "object") return null;
  const finding = value as Record<string, unknown>;
  const categories = new Set(["coherence", "clarity", "pacing", "continuity", "character", "style"]);
  const severities = new Set(["critical", "important", "optional"]);
  if (!(typeof finding.issue === "string" && finding.issue.trim() !== ""
    && typeof finding.evidence === "string" && finding.evidence.trim() !== ""
    && typeof finding.impact === "string" && finding.impact.trim() !== ""
    && typeof finding.suggestion === "string" && finding.suggestion.trim() !== "")) {
    return null;
  }
  return {
    category: (typeof finding.category === "string" && categories.has(finding.category)
      ? finding.category
      : "style") as AiReviewFinding["category"],
    severity: (typeof finding.severity === "string" && severities.has(finding.severity)
      ? finding.severity
      : "optional") as AiReviewFinding["severity"],
    issue: finding.issue,
    evidence: finding.evidence,
    impact: finding.impact,
    suggestion: finding.suggestion,
  };
}

export function parseReviewResult(value: Record<string, unknown>): AiGenerateResult | null {
  const stages = new Set(["opening", "development", "climax", "resolution", "unknown"]);
  if (typeof value.summary !== "string" || value.summary.trim() === ""
      || !Array.isArray(value.findings)) {
    return null;
  }
  const findings = value.findings.map(parseReviewFinding);
  if (findings.some((finding) => finding === null)) return null;
  const strengths = Array.isArray(value.strengths)
    ? value.strengths.filter((item): item is string => typeof item === "string" && item.trim() !== "")
    : [];
  const limitations = Array.isArray(value.limitations)
    ? value.limitations.filter((item): item is string => typeof item === "string" && item.trim() !== "")
    : [];
  return {
    format: "review",
    chapterStage: (typeof value.chapterStage === "string" && stages.has(value.chapterStage)
      ? value.chapterStage
      : "unknown") as "opening" | "development" | "climax" | "resolution" | "unknown",
    summary: value.summary,
    findings: findings as AiReviewFinding[],
    strengths,
    limitations,
  };
}

export async function generateAiText(
  storyId: number,
  payload: GenerateAiPayload
): Promise<GenerateAiResponse> {
  
  const request: NovelAiRequest = {
    storyId,
    chapterId: payload.chapterId || undefined,
    action: payload.actionType,
    instruction: payload.instruction,
    selectedText: payload.selectedText || undefined,
    idempotencyKey: payload.idempotencyKey || crypto.randomUUID(),
    currentText: payload.currentText,
  };

  const response = await generateNovelAi(request, payload.abortSignal);

  let result: AiGenerateResult;

  if (payload.actionType === "REVIEW_CHAPTER") {
    let review = response.structuredResult ? parseReviewResult(response.structuredResult) : null;
    if (!review && payload.chapterId && response.requestId) {
      try {
        const history = await getChapterAiHistory(storyId, payload.chapterId, 0, 10, payload.abortSignal);
        const stored = history.content.find((item) => String(item.usageId) === String(response.requestId));
        review = stored?.structuredResult ? parseReviewResult(stored.structuredResult) : null;
      } catch {
        review = null;
      }
    }
    if (!review) throw new Error("A IA retornou uma revisão em formato inválido.");
    result = review;
  } else if (response.structuredResult && response.structuredResult.options && Array.isArray(response.structuredResult.options)) {
    const opts: unknown[] = response.structuredResult.options;
    if (opts.length !== 3) {
      throw new Error("A IA retornou um número incorreto de opções (esperado: 3).");
    }
    if (payload.actionType === "GENERATE_CHAPTER_TITLE") {
      if (!opts.every((option) => typeof option === "string" && option.trim() !== "")) {
        throw new Error("A IA retornou um título inválido.");
      }
      const titles = opts.map((option) => (option as string).trim());
      if (new Set(titles.map((title) => title.toLocaleLowerCase())).size !== 3) {
        throw new Error("A IA retornou títulos duplicados.");
      }
      result = {
        format: "title-suggestions",
        options: titles as [string, string, string],
      };
    } else if (!opts.every(isSuggestionOption)) {
      throw new Error("A IA retornou uma opção inválida.");
    } else {
      const types = new Set(opts.map((option) => option.type));
      if (types.size !== 3) throw new Error("A IA retornou opções com tipos duplicados.");

      result = {
        format: "suggestions",
        options: opts as [SuggestContinuationOption, SuggestContinuationOption, SuggestContinuationOption]
      };
    }
  } else {
    if (payload.actionType === "SUGGEST_CONTINUATION" || payload.actionType === "GENERATE_CHAPTER_TITLE") {
      throw new Error("A IA falhou em retornar as sugestões no formato correto.");
    }
    if (!response.content || typeof response.content !== "string" || response.content.trim() === "") {
      throw new Error("A IA retornou um texto vazio.");
    }
    result = {
      format: "prose",
      content: response.content
    };
  }

  return {
    generationId: null,
    provider: response.provider,
    model: response.model,
    result,
    status: "SUCCESS"
  };
}

export async function estimateAiText(
  storyId: number,
  payload: GenerateAiPayload
): Promise<AiEstimateResponse> {
  const request: NovelAiRequest = {
    storyId,
    chapterId: payload.chapterId || undefined,
    action: payload.actionType,
    instruction: payload.instruction,
    selectedText: payload.selectedText || undefined,
    idempotencyKey: payload.idempotencyKey || crypto.randomUUID(),
    currentText: payload.currentText,
  };

  return estimateNovelAi(request, payload.abortSignal);
}
