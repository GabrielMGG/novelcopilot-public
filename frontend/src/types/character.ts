export type CharacterImportance = "MAIN" | "SUPPORTING" | "MINOR" | "";

export type CharacterNarrativeRole =
  | "PROTAGONIST"
  | "ANTAGONIST"
  | "VILLAIN"
  | "LOVE_INTEREST"
  | "MENTOR"
  | "ALLY"
  | "RIVAL"
  | "OTHER"
  | "";

export type ApiCharacter = {
  id: number;
  storyId: number;
  name: string;
  role: string;
  importance: CharacterImportance;
  narrativeRole: CharacterNarrativeRole;
  description: string | null;
  appearance: string | null;
  personality: string | null;
  goal: string | null;
  backstory: string | null;
  conflict: string | null;
  relationships: string | null;
  notes: string | null;
  imageUrl: string | null;
  imagePrompt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateCharacterPayload = {
  name: string;
  role: string;
  importance: CharacterImportance;
  narrativeRole: CharacterNarrativeRole;
  description: string;
  appearance: string;
  personality: string;
  goal: string;
  backstory: string;
  conflict: string;
  relationships: string;
  notes: string;
  imageUrl: string;
  imagePrompt: string;
};

export type UpdateCharacterPayload = CreateCharacterPayload;