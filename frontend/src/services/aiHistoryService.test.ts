import { beforeEach, describe, expect, it, vi, type Mock } from "vitest";
import { authenticatedFetch } from "./authenticatedFetch";
import { getChapterAiHistory } from "./aiHistoryService";

vi.mock("./authenticatedFetch", () => ({ authenticatedFetch: vi.fn() }));

describe("aiHistoryService", () => {
  beforeEach(() => vi.clearAllMocks());

  it("normalizes a serialized structured_output from stored title history", async () => {
    (authenticatedFetch as unknown as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        content: [{
          id: 38,
          action: "GENERATE_CHAPTER_TITLE",
          outputType: "STRUCTURED",
          content: null,
          structured_output: '{"options":["O Zumbido no Bueiro","Memórias de Outro Mundo","Sombras na Fábrica de Processamento"]}',
          provider: "Cloudflare",
          model: "gemma",
          createdAt: "2026-08-02T14:35:01",
        }],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      }),
    });

    const page = await getChapterAiHistory(21, 47);

    expect(page.content[0].structuredResult).toEqual({
      options: ["O Zumbido no Bueiro", "Memórias de Outro Mundo", "Sombras na Fábrica de Processamento"],
    });
  });
});
