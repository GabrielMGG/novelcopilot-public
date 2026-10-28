import type {
  ApiChapter,
  CreateChapterPayload,
  UpdateChapterPayload,
} from "../types/chapter";
import { API_BASE_URL } from "../config/api";
import { authenticatedFetch } from "./authenticatedFetch";

export class ChapterVersionConflictError extends Error {
  readonly code = "CHAPTER_VERSION_CONFLICT";

  constructor() {
    super("O capítulo foi alterado em outra aba ou sessão.");
    this.name = "ChapterVersionConflictError";
  }
}

export async function listChaptersByStory(storyId: number): Promise<ApiChapter[]> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}/chapters`
  );

  if (response.status === 404) {
    throw new Error("NOT_FOUND");
  }

  if (!response.ok) {
    throw new Error("Não foi possível carregar os capítulos.");
  }

  return response.json();
}

export async function createChapter(storyId: number, payload: CreateChapterPayload) {
  const response = await authenticatedFetch(`${API_BASE_URL}/stories/${storyId}/chapters`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Erro ao criar capítulo.");
  }

  return response.json() as Promise<ApiChapter>;
}

export async function getChapterById(chapterId: number): Promise<ApiChapter> {
  const response = await authenticatedFetch(`${API_BASE_URL}/chapters/${chapterId}`);

  if (!response.ok) {
    throw new Error("Não foi possível carregar a versão mais recente do capítulo.");
  }

  return response.json() as Promise<ApiChapter>;
}

export async function updateChapter(chapterId: number, payload: UpdateChapterPayload) {
  const response = await authenticatedFetch(`${API_BASE_URL}/chapters/${chapterId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (response.status === 409) {
    const errorBody = await response.json().catch(() => null) as { code?: string } | null;
    if (errorBody?.code === "CHAPTER_VERSION_CONFLICT") {
      throw new ChapterVersionConflictError();
    }
  }

  if (!response.ok) {
    throw new Error("Erro ao atualizar capítulo.");
  }

  return response.json() as Promise<ApiChapter>;
}

export async function deleteChapter(chapterId: number) {
  const response = await authenticatedFetch(`${API_BASE_URL}/chapters/${chapterId}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error("Erro ao excluir capítulo.");
  }
}
