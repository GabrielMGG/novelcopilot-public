import { authenticatedFetch } from "./authenticatedFetch";
import type { AiHistoryPage } from "../types/ai";
import { readStructuredAiResult } from "../utils/structuredAiResult";

export async function getChapterAiHistory(
  storyId: number,
  chapterId: number,
  page = 0,
  size = 10,
  signal?: AbortSignal
): Promise<AiHistoryPage> {
  const response = await authenticatedFetch(
    `/api/stories/${storyId}/chapters/${chapterId}/ai-history?page=${page}&size=${size}`,
    { method: "GET", signal }
  );

  if (!response.ok) {
    throw new Error();
  }

  const payload = await response.json() as AiHistoryPage;
  return {
    ...payload,
    content: payload.content.map((item) => ({
      ...item,
      structuredResult: readStructuredAiResult(item as unknown as Record<string, unknown>),
    })),
  };
}
