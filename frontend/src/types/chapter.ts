export type ApiChapter = {
  id: number;
  storyId: number;
  title: string;
  chapterOrder: number;
  type: string;
  goal: string;
  summary: string;
  summaryContentHash?: string | null;
  summaryGeneratedAt?: string | null;
  summaryProvider?: string | null;
  summaryModel?: string | null;
  content: string;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type CreateChapterPayload = {
  title: string;
  chapterOrder: number;
  type: string;
  goal: string;
  summary: string;
  content: string;
};

export type UpdateChapterPayload = {
  title: string;
  chapterOrder: number;
  type: string;
  goal: string;
  summary: string;
  content: string;
  version: number;
};
