export type WorldNoteCategory =
  | "LOCATION"
  | "FACTION"
  | "CULTURE"
  | "HISTORY"
  | "MAGIC_SYSTEM"
  | "RULE"
  | "CREATURE"
  | "ITEM"
  | "RELIGION"
  | "POLITICS"
  | "ECONOMY"
  | "OTHER"
  | "";

export type WorldNoteImportance = "HIGH" | "MEDIUM" | "LOW" | "";

export type WorldNoteStatus = "CANON" | "DRAFT" | "ARCHIVED" | "";

export type ApiWorldNote = {
  id: number;
  storyId: number;
  title: string;
  category: WorldNoteCategory;
  importance: WorldNoteImportance;
  status: WorldNoteStatus;
  tags: string | null;
  content: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateWorldNotePayload = {
  title: string;
  category: WorldNoteCategory;
  importance: WorldNoteImportance;
  status: WorldNoteStatus;
  tags: string;
  content: string;
};

export type UpdateWorldNotePayload = CreateWorldNotePayload;