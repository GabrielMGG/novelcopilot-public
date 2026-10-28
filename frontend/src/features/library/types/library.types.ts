
export type StoryBook = {
  id: number;
  title: string;
  genre: string | null;
  tone: string | null;
  summary: string | null;
  mainIdea: string | null;
  status: string | null;
  words: number;
  updatedAt: string;
  createdAt: string | null;
  coverImagePath: string | null;
  coverDisplayUrl?: string | null;
  accent: string;
};

// Card especial usado para representar o botão "Nova História" dentro da estante.
export type AddCard = {
  isAdd: true;
};

export type ShelfItem = StoryBook | AddCard;
