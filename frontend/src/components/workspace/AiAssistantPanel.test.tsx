/**
 * @vitest-environment jsdom
 */
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AiAssistantPanel } from "./AiAssistantPanel";
import { getChapterAiHistory } from "../../services/aiHistoryService";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, options?: { current?: number; total?: number; defaultValue?: string }) => {
      if (key === "ai.history.page") return `Page ${options?.current} of ${options?.total}`;
      return options?.defaultValue ?? key;
    },
  }),
}));

vi.mock("../../hooks/useCredits", () => ({
  useCredits: () => ({
    balance: { availableCredits: 9.9, reservedCredits: 0.2 },
    isLoading: false,
  }),
}));

vi.mock("../../services/aiHistoryService", () => ({
  getChapterAiHistory: vi.fn(),
}));

const mockedGetHistory = vi.mocked(getChapterAiHistory);

function historyPage(page = 0, last = true) {
  return {
    content: [{
      id: 7,
      usageId: 101,
      action: "CONTINUE_SCENE" as const,
      outputType: "PROSE" as const,
      content: "A resposta original da IA.",
      structuredResult: null,
      provider: "cloudflare",
      model: "gemma",
      createdAt: "2026-08-02T10:00:00",
    }],
    number: page,
    size: 10,
    totalElements: last ? 1 : 11,
    totalPages: last ? 1 : 2,
    first: page === 0,
    last,
  };
}

function renderPanel(onOpenHistoryResult = vi.fn()) {
  render(
    <MemoryRouter>
      <AiAssistantPanel
        isOpen
        isLoading={false}
        instructionText=""
        resultText=""
        errorMessage=""
        actions={[]}
        onToggleOpen={vi.fn()}
        onInstructionChange={vi.fn()}
        onGenerate={vi.fn()}
        onOpenSuggestion={vi.fn()}
        storyId={21}
        chapterId={50}
        onOpenHistoryResult={onOpenHistoryResult}
      />
    </MemoryRouter>
  );
}

describe("AiAssistantPanel history", () => {
  beforeEach(() => {
    mockedGetHistory.mockReset();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    });
  });

  it("shows fractional available and reserved credits", () => {
    renderPanel();

    expect(screen.getByText(/9,9 disponíveis/)).toBeInTheDocument();
    expect(screen.getByText(/0,2 reservados/)).toBeInTheDocument();
  });

  it("loads only the selected chapter when the history tab opens", async () => {
    mockedGetHistory.mockResolvedValue(historyPage());
    renderPanel();

    fireEvent.click(screen.getByRole("tab", { name: "ai.history.historyTab" }));

    expect(await screen.findByText("A resposta original da IA.")).toBeInTheDocument();
    expect(mockedGetHistory).toHaveBeenCalledWith(21, 50, 0, 10, expect.any(AbortSignal));
  });

  it("opens a stored response through the existing suggestion flow", async () => {
    mockedGetHistory.mockResolvedValue(historyPage());
    const onOpen = vi.fn();
    renderPanel(onOpen);
    fireEvent.click(screen.getByRole("tab", { name: "ai.history.historyTab" }));

    fireEvent.click(await screen.findByRole("button", { name: "ai.history.open" }));

    expect(onOpen).toHaveBeenCalledWith({
      format: "prose",
      content: "A resposta original da IA.",
    });
  });

  it("opens stored chapter title options", async () => {
    mockedGetHistory.mockResolvedValue({
      ...historyPage(),
      content: [{
        ...historyPage().content[0],
        action: "GENERATE_CHAPTER_TITLE",
        outputType: "STRUCTURED",
        content: null,
        structuredResult: {
          options: ["O Zumbido no Bueiro", "Memórias de Outro Mundo", "Sombras na Fábrica de Processamento"],
        },
      }],
    });
    const onOpen = vi.fn();
    renderPanel(onOpen);
    fireEvent.click(screen.getByRole("tab", { name: "ai.history.historyTab" }));

    const openButton = await screen.findByRole("button", { name: "ai.history.open" });
    expect(openButton).toBeEnabled();
    fireEvent.click(openButton);

    expect(onOpen).toHaveBeenCalledWith({
      format: "title-suggestions",
      options: ["O Zumbido no Bueiro", "Memórias de Outro Mundo", "Sombras na Fábrica de Processamento"],
    });
  });

  it("requests the next server page without mixing chapters", async () => {
    mockedGetHistory
      .mockResolvedValueOnce(historyPage(0, false))
      .mockResolvedValue(historyPage(1, true));
    renderPanel();
    fireEvent.click(screen.getByRole("tab", { name: "ai.history.historyTab" }));
    await screen.findByText("Page 1 of 2");

    fireEvent.click(screen.getByRole("button", { name: "ai.history.next" }));

    await waitFor(() => {
      expect(mockedGetHistory).toHaveBeenLastCalledWith(21, 50, 1, 10, expect.any(AbortSignal));
    });
  });

  it("copies a stored response without changing the editor", async () => {
    mockedGetHistory.mockResolvedValue(historyPage());
    const onOpen = vi.fn();
    renderPanel(onOpen);
    fireEvent.click(screen.getByRole("tab", { name: "ai.history.historyTab" }));

    fireEvent.click(await screen.findByRole("button", { name: "ai.history.copy" }));

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("A resposta original da IA.");
    expect(onOpen).not.toHaveBeenCalled();
  });

  it("shows a clear empty state for a chapter without responses", async () => {
    mockedGetHistory.mockResolvedValue({ ...historyPage(), content: [], totalElements: 0 });
    renderPanel();
    fireEvent.click(screen.getByRole("tab", { name: "ai.history.historyTab" }));

    expect(await screen.findByText("ai.history.empty")).toBeInTheDocument();
  });
});
