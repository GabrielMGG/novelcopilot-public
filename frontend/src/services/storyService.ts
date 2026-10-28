import { API_BASE_URL } from "../config/api";
import { authenticatedFetch } from "./authenticatedFetch";
import type { ApiStory, CreateStoryPayload, UpdateStoryPayload } from "../types/story";

export async function listStories(): Promise<ApiStory[]> {
  const response = await authenticatedFetch(`${API_BASE_URL}/stories`);

  if (!response.ok) {
    throw new Error("Não foi possível carregar as histórias.");
  }

  return response.json();
}
export async function getStoryById(storyId: number): Promise<ApiStory> {
  const response = await authenticatedFetch(`${API_BASE_URL}/stories/${storyId}`);

  if (response.status === 404) {
    throw new Error("NOT_FOUND");
  }

  if (!response.ok) {
    throw new Error("Não foi possível carregar a história.");
  }

  return response.json();
}

export async function createStory(payload: CreateStoryPayload): Promise<ApiStory> {
  const response = await authenticatedFetch(`${API_BASE_URL}/stories`, {
    method: "POST",
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Não foi possível criar a história.");
  }

  return response.json();
}

export async function updateStory(
  storyId: number,
  payload: UpdateStoryPayload
): Promise<ApiStory> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}`,
    {
      method: "PUT",
      body: JSON.stringify(payload),
    }
  );

  if (!response.ok) {
    throw new Error("Não foi possível atualizar a história.");
  }

  return response.json();
}

export async function moveStoryToTrash(storyId: number): Promise<void> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}`,
    {
      method: "DELETE",
    }
  );

  if (!response.ok) {
    throw new Error("Não foi possível mover a história para a lixeira.");
  }
}

export async function listTrashStories(): Promise<ApiStory[]> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/trash`
  );

  if (!response.ok) {
    throw new Error("Não foi possível carregar a lixeira.");
  }

  return response.json();
}

export async function restoreStory(
  storyId: number
): Promise<ApiStory> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}/restore`,
    {
      method: "PATCH",
    }
  );

  if (!response.ok) {
    throw new Error("Não foi possível restaurar a história.");
  }

  return response.json();
}

export async function permanentlyDeleteStory(
  storyId: number
): Promise<void> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}/permanent`,
    {
      method: "DELETE",
    }
  );

  if (!response.ok) {
    throw new Error(
      "Não foi possível excluir definitivamente a história."
    );
  }
}