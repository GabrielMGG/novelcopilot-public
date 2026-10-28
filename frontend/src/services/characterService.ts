import { API_BASE_URL } from "../config/api";
import type {
  ApiCharacter,
  CreateCharacterPayload,
  UpdateCharacterPayload,
} from "../types/character";
import { authenticatedFetch } from "./authenticatedFetch";


export async function listCharactersByStory(
  storyId: number
): Promise<ApiCharacter[]> {
  const response = await authenticatedFetch(
    `${API_BASE_URL}/stories/${storyId}/characters`
  );

  if (response.status === 404) {
    throw new Error("NOT_FOUND");
  }

  if (!response.ok) {
    throw new Error("Failed to list characters");
  }

  return response.json();
}

export async function createCharacter(
  storyId: number,
  payload: CreateCharacterPayload
): Promise<ApiCharacter> {
  const response = await authenticatedFetch(`${API_BASE_URL}/stories/${storyId}/characters`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Failed to create character");
  }

  return response.json();
}

export async function updateCharacter(
  characterId: number,
  payload: UpdateCharacterPayload
): Promise<ApiCharacter> {
  const response = await authenticatedFetch(`${API_BASE_URL}/characters/${characterId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Failed to update character");
  }

  return response.json();
}

export async function deleteCharacter(characterId: number): Promise<void> {
  const response = await authenticatedFetch(`${API_BASE_URL}/characters/${characterId}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error("Failed to delete character");
  }
}