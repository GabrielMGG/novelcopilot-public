import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest';
import {
  AI_ESTIMATE_TIMEOUT_MS,
  AI_GENERATION_TIMEOUT_MS,
  estimateNovelAi,
  generateNovelAi,
} from './novelAiApi';
import { generateChapterSummary } from './chapterSummaryService';
import { estimateAiText, generateAiText } from './aiService';
import type { NovelAiRequest } from './novelAiApi';
import { authenticatedFetch } from './authenticatedFetch';

// Mock the authenticatedFetch function
vi.mock('./authenticatedFetch', () => ({
  authenticatedFetch: vi.fn(),
}));

describe('novelAiApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('generateNovelAi calls /api/ai/generate with POST and correct payload', async () => {
    const mockResponse = {
      id: 'uuid-123',
      requestId: 'req-456',
      action: 'CONTINUE_SCENE',
      content: 'Generated text',
      provider: 'OpenRouter',
      model: 'qwen3-235b',
      fallbackUsed: false,
      inputTokens: 10,
      outputTokens: 50,
      totalTokens: 60,
      costUsd: 0.01,
      costSource: 'PROVIDER',
      durationMs: 500,
      createdAt: '2026-07-21T10:00:00Z',
    };

    // Setup the mock to return a successful response
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    const requestPayload: NovelAiRequest = {
      storyId: 1,
      chapterId: 2,
      action: 'CONTINUE_SCENE',
      instruction: 'Make it tense',
      selectedText: 'It was a dark night.',
      idempotencyKey: 'idem-123',
    };

    const result = await generateNovelAi(requestPayload);

    // Assert the fetch was called correctly
    expect(authenticatedFetch).toHaveBeenCalledTimes(1);
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/ai/generate', expect.objectContaining({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestPayload),
      signal: expect.any(AbortSignal),
    }));

    // Assert the result is as expected
    expect(result).toEqual(mockResponse);
  });

  it('generateNovelAi throws an error when response is not ok', async () => {
    // Setup the mock to return a failed response
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    });

    const requestPayload: NovelAiRequest = {
      storyId: 1,
      action: 'CONTINUE_SCENE',
      instruction: '',
      idempotencyKey: 'idem-123',
    };

    await expect(generateNovelAi(requestPayload)).rejects.toThrow('Erro interno no servidor.');
  });

  it('generateNovelAi reports retry time when rate limited', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: false,
      status: 429,
      json: async () => ({
        code: 'AI_RATE_LIMIT_EXCEEDED',
        message: 'Limite de uso da IA excedido. Tente novamente em 30 segundos.',
        retryAfterSeconds: 30,
      }),
    });

    await expect(generateNovelAi({
      storyId: 1,
      action: 'CONTINUE_SCENE',
      idempotencyKey: 'idem-limited',
    })).rejects.toThrow('Tente novamente em 30 segundos.');
  });

  it('generateNovelAi reports insufficient credits', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: false,
      status: 402,
      json: async () => ({
        code: 'AI_INSUFFICIENT_CREDITS',
        message: 'Créditos insuficientes para esta geração.',
      }),
    });

    await expect(generateNovelAi({
      storyId: 1,
      action: 'CONTINUE_SCENE',
      idempotencyKey: 'idem-without-credits',
    })).rejects.toThrow('Créditos insuficientes para esta geração.');
  });

  it('generateNovelAi preserves an explicit cancellation', async () => {
    const abortController = new AbortController();
    (authenticatedFetch as unknown as Mock).mockImplementationOnce(
      (_input: string, init: RequestInit) =>
        new Promise((_resolve, reject) => {
          init.signal?.addEventListener('abort', () => {
            reject(init.signal?.reason);
          });
        })
    );

    const generation = generateNovelAi({
      storyId: 1,
      action: 'CONTINUE_SCENE',
      idempotencyKey: 'idem-cancelled',
    }, abortController.signal);
    abortController.abort();

    await expect(generation).rejects.toMatchObject({ name: 'AbortError' });
  });

  it('generateNovelAi fails visibly after the generation timeout', async () => {
    vi.useFakeTimers();
    (authenticatedFetch as unknown as Mock).mockImplementationOnce(
      (_input: string, init: RequestInit) =>
        new Promise((_resolve, reject) => {
          init.signal?.addEventListener('abort', () => {
            reject(init.signal?.reason);
          });
        })
    );

    const generation = generateNovelAi({
      storyId: 1,
      action: 'CONTINUE_SCENE',
      idempotencyKey: 'idem-timeout',
    });
    const timeoutExpectation = expect(generation).rejects.toThrow(
      'A geração demorou mais que o esperado.'
    );
    await vi.advanceTimersByTimeAsync(AI_GENERATION_TIMEOUT_MS);

    await timeoutExpectation;
    vi.useRealTimers();
  });

  it('estimateNovelAi fails visibly after the estimate timeout', async () => {
    vi.useFakeTimers();
    (authenticatedFetch as unknown as Mock).mockImplementationOnce(
      (_input: string, init: RequestInit) =>
        new Promise((_resolve, reject) => {
          init.signal?.addEventListener('abort', () => {
            reject(init.signal?.reason);
          });
        })
    );

    const estimate = estimateNovelAi({
      storyId: 1,
      action: 'CONTINUE_SCENE',
      idempotencyKey: 'estimate-timeout',
    });
    const timeoutExpectation = expect(estimate).rejects.toThrow(
      'A estimativa demorou mais que o esperado.'
    );
    await vi.advanceTimersByTimeAsync(AI_ESTIMATE_TIMEOUT_MS);

    await timeoutExpectation;
    vi.useRealTimers();
  });

  it('generateChapterSummary preserves the backend rate-limit message', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: false,
      status: 429,
      json: async () => ({
        message: 'Limite de uso da IA excedido. Tente novamente em 45 segundos.',
      }),
    });

    await expect(generateChapterSummary(1, 2))
      .rejects.toThrow('Tente novamente em 45 segundos.');
  });

  it('generateAiText sends the current unsaved editor text', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        content: 'Continuação',
        provider: 'Cloudflare',
        model: 'model',
      }),
    });

    await generateAiText(1, {
      chapterId: 2,
      actionType: 'CONTINUE_SCENE',
      instruction: 'Continue.',
      currentText: 'Texto atual que ainda está no editor.',
      selectedText: null,
      idempotencyKey: 'current-editor-text',
    });

    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/ai/generate',
      expect.objectContaining({
        body: expect.stringContaining('"currentText":"Texto atual que ainda está no editor."'),
      })
    );
  });

  it('estimateAiText uses the same current editor text as generation', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ estimatedCredits: 1 }),
    });

    await estimateAiText(1, {
      chapterId: 2,
      actionType: 'CONTINUE_SCENE',
      instruction: 'Continue.',
      currentText: 'Snapshot idêntico para estimativa e geração.',
      selectedText: null,
      idempotencyKey: 'same-snapshot',
    });

    expect(authenticatedFetch).toHaveBeenCalledWith(
      '/api/ai/estimate',
      expect.objectContaining({
        body: expect.stringContaining('"currentText":"Snapshot idêntico para estimativa e geração."'),
      })
    );
  });

  it('generateAiText parses three chapter title suggestions', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        structuredResult: { options: ['A Porta Aberta', 'A Chave de Prata', 'O Número Sete'] },
        provider: 'Cloudflare',
        model: 'model',
      }),
    });

    const response = await generateAiText(1, {
      chapterId: 2,
      actionType: 'GENERATE_CHAPTER_TITLE',
      instruction: 'Sugira títulos.',
      currentText: 'Lena encontrou uma chave de prata perto da estação.',
      selectedText: null,
    });

    expect(response.result).toEqual({
      format: 'title-suggestions',
      options: ['A Porta Aberta', 'A Chave de Prata', 'O Número Sete'],
    });
  });

  it('generateAiText accepts a serialized structured_result response', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        structured_result: '{"options":["O Zumbido no Bueiro","Memórias de Outro Mundo","Sombras na Fábrica de Processamento"]}',
        provider: 'Cloudflare',
        model: 'model',
      }),
    });

    const response = await generateAiText(1, {
      chapterId: 2,
      actionType: 'GENERATE_CHAPTER_TITLE',
      instruction: 'Sugira títulos.',
      currentText: 'Texto do capítulo.',
      selectedText: null,
    });

    expect(response.result).toEqual({
      format: 'title-suggestions',
      options: ['O Zumbido no Bueiro', 'Memórias de Outro Mundo', 'Sombras na Fábrica de Processamento'],
    });
  });

  it('generateAiText rejects duplicate chapter title suggestions', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        structuredResult: { options: ['A Porta Aberta', 'A Porta Aberta', 'O Número Sete'] },
        provider: 'Cloudflare',
        model: 'model',
      }),
    });

    await expect(generateAiText(1, {
      chapterId: 2,
      actionType: 'GENERATE_CHAPTER_TITLE',
      instruction: 'Sugira títulos.',
      currentText: 'Texto do capítulo.',
      selectedText: null,
    })).rejects.toThrow('títulos duplicados');
  });
  it('generateAiText parses an evidence-based chapter review', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        structuredResult: {
          chapterStage: 'opening',
          summary: 'The opening establishes the conflict.',
          findings: [{
            category: 'continuity',
            severity: 'important',
            issue: 'A decision changes without a transition.',
            evidence: 'The character refuses and then enters.',
            impact: 'The change weakens scene coherence.',
            suggestion: 'Explain what changed the decision.',
          }],
          strengths: ['Consistent atmosphere.'],
          limitations: ['The story is still beginning.'],
        },
        provider: 'Cloudflare',
        model: 'model',
      }),
    });

    const response = await generateAiText(1, {
      chapterId: 2,
      actionType: 'REVIEW_CHAPTER',
      instruction: 'Review.',
      currentText: 'Chapter text.',
      selectedText: null,
    });

    expect(response.result).toMatchObject({
      format: 'review',
      chapterStage: 'opening',
      findings: [expect.objectContaining({ severity: 'important' })],
    });
  });

  it('generateAiText safely normalizes an unknown review classification', async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        structuredResult: {
          chapterStage: 'opening',
          summary: 'Summary.',
          findings: [{ category: 'structure', severity: 'urgent', issue: 'Issue.', evidence: 'Excerpt.', impact: 'Impact.', suggestion: 'Suggestion.' }],
          strengths: ['Strength.', null],
          limitations: null,
        },
        provider: 'Cloudflare',
        model: 'model',
      }),
    });

    const response = await generateAiText(1, {
      chapterId: 2,
      actionType: 'REVIEW_CHAPTER',
      instruction: 'Review.',
      currentText: 'Text.',
      selectedText: null,
    });

    expect(response.result).toMatchObject({
      format: 'review',
      findings: [expect.objectContaining({ category: 'style', severity: 'optional' })],
      strengths: ['Strength.'],
      limitations: [],
    });
  });

  it('recovers a charged review from the matching history usage', async () => {
    const storedReview = {
      chapterStage: 'development',
      summary: 'The stored review is valid.',
      findings: [{
        category: 'pacing', severity: 'optional', issue: 'Fast transition.',
        evidence: 'The scene changes in one sentence.', impact: 'Less tension.',
        suggestion: 'Add one sensory detail.',
      }],
      strengths: ['Clear conflict.'],
      limitations: [],
    };
    (authenticatedFetch as unknown as Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          requestId: '43',
          structuredResult: { unexpectedWrapper: storedReview },
          provider: 'Cloudflare',
          model: 'model',
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          content: [{
            id: 9,
            usageId: 43,
            action: 'REVIEW_CHAPTER',
            outputType: 'STRUCTURED',
            content: null,
            structuredResult: storedReview,
            provider: 'Cloudflare',
            model: 'model',
            createdAt: '2026-08-02T16:07:52',
          }],
          number: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }),
      });

    const response = await generateAiText(21, {
      chapterId: 50,
      actionType: 'REVIEW_CHAPTER',
      instruction: 'Review.',
      currentText: 'Chapter text.',
      selectedText: null,
    });

    expect(response.result).toMatchObject({ format: 'review', summary: 'The stored review is valid.' });
    expect(authenticatedFetch).toHaveBeenNthCalledWith(
      2,
      '/api/stories/21/chapters/50/ai-history?page=0&size=10',
      expect.objectContaining({ method: 'GET' })
    );
  });
});
