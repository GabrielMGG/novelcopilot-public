import { authenticatedFetch } from './authenticatedFetch';
import { readStructuredAiResult } from '../utils/structuredAiResult';
import type {
  AiEstimateResponse,
  CreditBalanceResponse,
  CreditTransactionPage,
} from '../types/credits';

export const AI_ESTIMATE_TIMEOUT_MS = 20_000;
export const AI_GENERATION_TIMEOUT_MS = 70_000;

function createTimeoutSignal(signal: AbortSignal | undefined, timeoutMs: number) {
  const controller = new AbortController();
  let timedOut = false;
  const timeoutId = window.setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, timeoutMs);
  const handleExternalAbort = () => controller.abort(signal?.reason);

  if (signal?.aborted) {
    handleExternalAbort();
  } else {
    signal?.addEventListener('abort', handleExternalAbort, { once: true });
  }

  return {
    signal: controller.signal,
    didTimeOut: () => timedOut,
    cleanup: () => {
      window.clearTimeout(timeoutId);
      signal?.removeEventListener('abort', handleExternalAbort);
    },
  };
}

async function authenticatedAiFetch(
  input: string,
  init: RequestInit,
  signal: AbortSignal | undefined,
  timeoutMs: number,
  timeoutMessage: string
) {
  const timeout = createTimeoutSignal(signal, timeoutMs);

  try {
    return await authenticatedFetch(input, {
      ...init,
      signal: timeout.signal,
    });
  } catch (error) {
    if (timeout.didTimeOut()) {
      throw new Error(timeoutMessage, { cause: error });
    }
    throw error;
  } finally {
    timeout.cleanup();
  }
}

export type NovelAiRequest = {
  action: string;
  storyId?: number;
  chapterId?: number;
  selectedText?: string;
  instruction?: string;
  language?: string;
  tone?: string;
  pov?: string;
  desiredLength?: number;
  reviewType?: string;
  characterIds?: number[];
  worldNoteIds?: number[];
  includePreviousChapters?: boolean;
  idempotencyKey?: string;
  currentText?: string;
};

export type NovelAiResponse = {
  requestId: string;
  action: string;
  content: string;
  structuredResult?: Record<string, unknown>;
  provider: string;
  model: string;
  fallbackUsed: boolean;
  inputTokens: number;
  cachedInputTokens: number;
  outputTokens: number;
  totalTokens: number;
  providerReportedCostUsd: number;
  calculatedCostUsd: number;
  effectiveCostUsd: number;
  costSource: string;
  durationMs: number;
  contextTruncated: boolean;
  createdAt: string;
};

export async function generateNovelAi(request: NovelAiRequest, signal?: AbortSignal): Promise<NovelAiResponse> {
  const response = await authenticatedAiFetch('/api/ai/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  }, signal, AI_GENERATION_TIMEOUT_MS, 'A geração demorou mais que o esperado. Tente novamente.');
  
  if (!response.ok) {
    let errorCode = '';
    let errorMessage = '';
    let retryAfterSeconds: number | undefined;
    try {
      const errorData = await response.json();
      errorCode = errorData.code || errorData.error || '';
      errorMessage = errorData.message || '';
      retryAfterSeconds = typeof errorData.retryAfterSeconds === 'number'
        ? errorData.retryAfterSeconds
        : undefined;
    } catch {
      // Ignore parse error
    }

    if (response.status === 429) {
      const waitMessage = retryAfterSeconds
        ? ` Tente novamente em ${retryAfterSeconds} segundos.`
        : ' Tente novamente em instantes.';
      throw new Error(errorMessage || `Limite de uso da IA excedido.${waitMessage}`);
    }

    switch (errorCode) {
      case 'AI_STORY_ACCESS_DENIED':
        throw new Error("Acesso negado a esta história.");
      case 'AI_CHAPTER_NOT_FOUND':
        throw new Error("Capítulo não encontrado.");
      case 'AI_INVALID_CHARACTER_CONTEXT':
        throw new Error("O contexto de personagens é inválido.");
      case 'AI_INVALID_WORLD_CONTEXT':
        throw new Error("O contexto de mundo é inválido.");
      case 'AI_INVALID_REFERENCE_CONTEXT':
        throw new Error("O contexto de referências é inválido.");
      case 'AI_INVALID_PROVIDER_RESPONSE':
        throw new Error("A IA não conseguiu gerar uma resposta válida. Tente novamente.");
      case 'AI_PROVIDER_ERROR':
        throw new Error("O provedor de IA falhou. Tente novamente mais tarde.");
      case 'AI_INSUFFICIENT_CREDITS':
        throw new Error("Créditos insuficientes para esta geração.");
      default:
        throw new Error(
          response.status >= 500
            ? "Erro interno no servidor."
            : errorMessage || "Erro ao processar a requisição da IA."
        );
    }
  }
  
  const payload = await response.json() as Record<string, unknown>;
  const structuredResult = readStructuredAiResult(payload);

  return {
    ...payload,
    structuredResult: structuredResult ?? undefined,
  } as NovelAiResponse;
}

export async function estimateNovelAi(request: NovelAiRequest, signal?: AbortSignal): Promise<AiEstimateResponse> {
  const response = await authenticatedAiFetch('/api/ai/estimate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  }, signal, AI_ESTIMATE_TIMEOUT_MS, 'A estimativa demorou mais que o esperado. Tente novamente.');
  
  if (!response.ok) {
    let errorMessage = '';
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || '';
    } catch {
      // Ignore parse error
    }
    throw new Error(
      response.status >= 500
        ? "Erro interno no servidor."
        : errorMessage || "Erro ao estimar custo da requisição."
    );
  }
  
  return response.json();
}

export async function getCreditBalance(signal?: AbortSignal): Promise<CreditBalanceResponse> {
  const response = await authenticatedFetch('/api/credits/balance', {
    method: 'GET',
    signal
  });
  
  if (!response.ok) {
    throw new Error("Erro ao buscar saldo de créditos.");
  }
  
  return response.json();
}

export async function getCreditTransactions(page = 0, size = 10, signal?: AbortSignal): Promise<CreditTransactionPage> {
  const response = await authenticatedFetch(`/api/credits/transactions?page=${page}&size=${size}`, {
    method: 'GET',
    signal
  });
  
  if (!response.ok) {
    throw new Error("Erro ao buscar extrato de créditos.");
  }
  
  return response.json();
}

