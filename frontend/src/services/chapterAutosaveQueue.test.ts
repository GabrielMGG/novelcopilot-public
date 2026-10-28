import { describe, expect, it, vi } from "vitest";
import type { ApiChapter, UpdateChapterPayload } from "../types/chapter";
import { ChapterVersionConflictError } from "./chapterService";
import { ChapterAutosaveQueue } from "./chapterAutosaveQueue";

function chapter(version: number, content: string): ApiChapter {
  return {
    id: 7,
    storyId: 2,
    title: "Capítulo 1",
    chapterOrder: 1,
    type: "chapter",
    goal: "",
    summary: "",
    content,
    createdAt: "2026-07-27T12:00:00",
    updatedAt: "2026-07-27T12:00:00",
    version,
  };
}

function payload(version: number, content: string): UpdateChapterPayload {
  return {
    title: "Capítulo 1",
    chapterOrder: 1,
    type: "chapter",
    goal: "",
    summary: "",
    content,
    version,
  };
}

describe("ChapterAutosaveQueue", () => {
  it("serializa alterações e usa a nova versão na gravação pendente", async () => {
    let resolveFirst!: (value: ApiChapter) => void;
    let resolveSecond!: (value: ApiChapter) => void;
    const firstRequest = new Promise<ApiChapter>((resolve) => {
      resolveFirst = resolve;
    });
    const secondRequest = new Promise<ApiChapter>((resolve) => {
      resolveSecond = resolve;
    });
    const saveChapter = vi
      .fn()
      .mockReturnValueOnce(firstRequest)
      .mockReturnValueOnce(secondRequest);
    const onSaved = vi.fn();
    const queue = new ChapterAutosaveQueue(saveChapter, {
      onSaved,
      onConflict: vi.fn(),
      onError: vi.fn(),
    });

    const firstSave = queue.enqueue(7, payload(3, "Primeira alteração"));
    const secondSave = queue.enqueue(7, payload(3, "Alteração mais recente"));

    expect(saveChapter).toHaveBeenCalledTimes(1);
    resolveFirst(chapter(4, "Primeira alteração"));
    await firstSave;
    await vi.waitFor(() => expect(saveChapter).toHaveBeenCalledTimes(2));

    expect(saveChapter.mock.calls[1][1]).toMatchObject({
      content: "Alteração mais recente",
      version: 4,
    });

    resolveSecond(chapter(5, "Alteração mais recente"));
    await secondSave;
    expect(onSaved).toHaveBeenLastCalledWith(
      expect.objectContaining({ version: 5 }),
      false
    );
  });

  it("pausa a fila quando o servidor informa conflito", async () => {
    const conflict = new ChapterVersionConflictError();
    const saveChapter = vi.fn().mockRejectedValue(conflict);
    const onConflict = vi.fn();
    const queue = new ChapterAutosaveQueue(saveChapter, {
      onSaved: vi.fn(),
      onConflict,
      onError: vi.fn(),
    });

    await expect(queue.enqueue(7, payload(2, "Texto local"))).rejects.toBe(conflict);
    expect(onConflict).toHaveBeenCalledWith(7);
    await expect(queue.enqueue(7, payload(2, "Novo texto local"))).rejects.toBeInstanceOf(
      ChapterVersionConflictError
    );
    expect(saveChapter).toHaveBeenCalledTimes(1);
  });

  it("ignora conflito de uma gravação pertencente a uma carga anterior", async () => {
    let rejectSave!: (error: unknown) => void;
    const saveChapter = vi.fn().mockReturnValue(
      new Promise<ApiChapter>((_resolve, reject) => {
        rejectSave = reject;
      })
    );
    const onConflict = vi.fn();
    const queue = new ChapterAutosaveQueue(saveChapter, {
      onSaved: vi.fn(),
      onConflict,
      onError: vi.fn(),
    });

    const staleSave = queue.enqueue(7, payload(2, "Texto da carga antiga"));
    queue.reset();
    rejectSave(new ChapterVersionConflictError());

    await expect(staleSave).rejects.toBeInstanceOf(ChapterVersionConflictError);
    expect(onConflict).not.toHaveBeenCalled();
  });
});
