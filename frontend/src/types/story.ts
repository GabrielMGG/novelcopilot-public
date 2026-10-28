export type ApiStory = {
  id: number;
  title: string;
  genre: string | null;
  tone: string | null;
  summary: string | null;
  mainIdea: string | null;
  status: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  deletedAt: string | null;
  coverImagePath: string | null;
};



export type CreateStoryPayload = {
  title: string;
  genre: string;
  tone: string;
  summary: string;
  mainIdea: string;
  coverImagePath?: string | null;
};

export type UpdateStoryPayload = {
  title: string;
  genre: string;
  tone: string;
  summary: string;
  mainIdea: string;
  status: string;
  coverImagePath: string | null;
};