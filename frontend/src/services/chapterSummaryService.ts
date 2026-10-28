import { API_BASE_URL } from "../config/api";
import { authenticatedFetch } from "./authenticatedFetch";

export type GenerateChapterSummaryResponse = {
  chapterId: number;
  summary: string;
  chapterVersion: number;
  summaryContentHash: string;
  summaryGeneratedAt: string;
};

export async function generateChapterSummary(
  storyId: number,
  chapterId: number
): Promise<GenerateChapterSummaryResponse> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}/chapters/${chapterId}/summary`,
    {
      method: "POST",
    }
  );

  if (!response.ok) {
    if (response.status === 429) {
      let rateLimitMessage = "";
      try {
        const errorData = await response.json();
        if (typeof errorData.message === "string" && errorData.message) {
          rateLimitMessage = errorData.message;
        }
      } catch {
        // Use the safe fallback below when the response body is not JSON.
      }
      throw new Error(
        rateLimitMessage
        || "Limite de uso da IA excedido. Tente novamente em instantes."
      );
    }
    throw new Error("Não foi possível gerar o resumo do capítulo.");
  }

  return response.json();
}
