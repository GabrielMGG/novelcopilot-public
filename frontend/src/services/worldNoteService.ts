import { API_BASE_URL } from "../config/api";
import type {
  ApiWorldNote,
  CreateWorldNotePayload,
  UpdateWorldNotePayload,
} from "../types/worldNote";
import { authenticatedFetch } from "./authenticatedFetch";

export async function listWorldNotesByStory(
  storyId: number
): Promise<ApiWorldNote[]> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}/world-notes`
  );

  if (response.status === 404) {
    throw new Error("NOT_FOUND");
  }

  if (!response.ok) {
    throw new Error("Failed to list world notes");
  }

  return response.json();
}

export async function createWorldNote(
  storyId: number,
  payload: CreateWorldNotePayload
): Promise<ApiWorldNote> {
  const response = await authenticatedFetch(`${API_BASE_URL}/stories/${storyId}/world-notes`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Failed to create world note");
  }

  return response.json();
}

export async function updateWorldNote(
  worldNoteId: number,
  payload: UpdateWorldNotePayload
): Promise<ApiWorldNote> {
  const response = await authenticatedFetch(`${API_BASE_URL}/world-notes/${worldNoteId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Failed to update world note");
  }

  return response.json();
}

export async function deleteWorldNote(worldNoteId: number): Promise<void> {
  const response = await authenticatedFetch(`${API_BASE_URL}/world-notes/${worldNoteId}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error("Failed to delete world note");
  }
}