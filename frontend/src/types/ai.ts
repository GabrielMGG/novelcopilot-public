export type AiActionType =
  | "CONTINUE_SCENE"
  | "INLINE_CONTINUATION"
  | "SUGGEST_CONTINUATION"
  | "WRITE_CHAPTER_FROM_BEATS"
  | "REWRITE_SELECTION"
  | "EXPAND_SELECTION"
  | "IMPROVE_DIALOGUE"
  | "ADD_TENSION"
  | "SIMPLIFY_SELECTION"
  | "CREATE_NEXT_CHAPTER"
  | "SUMMARIZE_SELECTION"
  | "REVIEW_CHAPTER"
  | "PLAN_SCENE"
  | "GENERATE_CHAPTER_TITLE";

export type SuggestContinuationOption = {
  type: "safe" | "unexpected" | "intense";
  title: string;
  summary: string;
  snippet: string;
};

export type AiProseResult = {
  format: "prose";
  content: string;
};

export type AiSuggestionsResult = {
  format: "suggestions";
  options: [SuggestContinuationOption, SuggestContinuationOption, SuggestContinuationOption];
};

export type AiTitleSuggestionsResult = {
  format: "title-suggestions";
  options: [string, string, string];
};

export type AiReviewFinding = {
  category: "coherence" | "clarity" | "pacing" | "continuity" | "character" | "style";
  severity: "critical" | "important" | "optional";
  issue: string;
  evidence: string;
  impact: string;
  suggestion: string;
};

export type AiReviewResult = {
  format: "review";
  chapterStage: "opening" | "development" | "climax" | "resolution" | "unknown";
  summary: string;
  findings: AiReviewFinding[];
  strengths: string[];
  limitations: string[];
};

export type AiGenerateResult = AiProseResult | AiSuggestionsResult | AiTitleSuggestionsResult | AiReviewResult;

export type GenerateAiPayload = {
  chapterId: number | null;
  actionType: AiActionType;
  instruction: string;
  currentText: string;
  selectedText: string | null;
  idempotencyKey?: string;
  abortSignal?: AbortSignal;
};

export type GenerateAiResponse = {
  generationId: number | null;
  provider: string;
  model: string;
  result: AiGenerateResult;
  status: string;
};

export type AiHistoryItem = {
  id: number;
  usageId: number;
  action: AiActionType;
  outputType: "PROSE" | "STRUCTURED";
  content: string | null;
  structuredResult: Record<string, unknown> | null;
  provider: string;
  model: string;
  createdAt: string;
};

export type AiHistoryPage = {
  content: AiHistoryItem[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
