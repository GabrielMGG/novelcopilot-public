import type { ApiChapter, UpdateChapterPayload } from "../types/chapter";
import { ChapterVersionConflictError } from "./chapterService";

type SaveChapter = (
  chapterId: number,
  payload: UpdateChapterPayload
) => Promise<ApiChapter>;

type PendingSave = {
  chapterId: number;
  payload: UpdateChapterPayload;
  resolve: (chapter: ApiChapter) => void;
  reject: (error: unknown) => void;
};

export type ChapterAutosaveEvents = {
  onSaved: (chapter: ApiChapter, hasPendingSave: boolean) => void;
  onConflict: (chapterId: number) => void;
  onError: (error: unknown) => void;
};

export class ChapterAutosaveQueue {
  private pendingSave: PendingSave | null = null;
  private isSaving = false;
  private isPaused = false;
  private generation = 0;
  private readonly saveChapter: SaveChapter;
  private events: ChapterAutosaveEvents;

  constructor(
    saveChapter: SaveChapter,
    events: ChapterAutosaveEvents
  ) {
    this.saveChapter = saveChapter;
    this.events = events;
  }

  setEvents(events: ChapterAutosaveEvents) {
    this.events = events;
  }

  enqueue(
    chapterId: number,
    payload: UpdateChapterPayload
  ): Promise<ApiChapter> {
    if (this.isPaused) {
      return Promise.reject(new ChapterVersionConflictError());
    }

    return new Promise<ApiChapter>((resolve, reject) => {
      if (this.pendingSave) {
        this.pendingSave.reject(new Error("CHAPTER_SAVE_REPLACED"));
      }

      this.pendingSave = { chapterId, payload, resolve, reject };
      void this.drain();
    });
  }

  reset() {
    this.generation += 1;
    this.isPaused = false;
    if (this.pendingSave) {
      this.pendingSave.reject(new Error("CHAPTER_SAVE_CANCELLED"));
      this.pendingSave = null;
    }
  }

  private async drain() {
    if (this.isSaving || this.isPaused) {
      return;
    }

    this.isSaving = true;
    const activeGeneration = this.generation;

    while (this.pendingSave && !this.isPaused) {
      const currentSave = this.pendingSave;
      this.pendingSave = null;

      try {
        const savedChapter = await this.saveChapter(
          currentSave.chapterId,
          currentSave.payload
        );

        if (activeGeneration !== this.generation) {
          currentSave.resolve(savedChapter);
          break;
        }

        const pendingAfterSave = this.pendingSave as PendingSave | null;
        if (pendingAfterSave?.chapterId === savedChapter.id) {
          pendingAfterSave.payload = {
            ...pendingAfterSave.payload,
            version: savedChapter.version,
          };
        }

        this.events.onSaved(savedChapter, this.pendingSave !== null);
        currentSave.resolve(savedChapter);
      } catch (error) {
        if (activeGeneration !== this.generation) {
          currentSave.reject(error);
          break;
        }

        currentSave.reject(error);

        if (error instanceof ChapterVersionConflictError) {
          this.isPaused = true;
          const pendingAfterConflict = this.pendingSave as PendingSave | null;
          if (pendingAfterConflict) {
            pendingAfterConflict.reject(error);
            this.pendingSave = null;
          }
          this.events.onConflict(currentSave.chapterId);
        } else {
          this.events.onError(error);
        }
      }
    }

    this.isSaving = false;
    if (this.pendingSave && !this.isPaused) {
      void this.drain();
    }
  }
}
