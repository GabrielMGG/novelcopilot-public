/**
 * @vitest-environment jsdom
 */
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listStories } from "../../../services/storyService";
import type { ApiStory } from "../../../types/story";
import { LibraryPage } from "./LibraryPage";

const coverMocks = vi.hoisted(() => ({
  getCoverDisplayUrl: vi.fn(() => null),
  loadStoryCovers: vi.fn(),
  refreshStoryCover: vi.fn(),
  removeStoryCover: vi.fn(),
}));
const translate = vi.hoisted(() => (key: string) => key);
const topbarProps = vi.hoisted(() => ({ current: {} as Record<string, unknown> }));
const bookshelfProps = vi.hoisted(() => ({ current: {} as Record<string, unknown> }));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: translate }),
}));

vi.mock("../../../hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "user-1" } }),
}));

vi.mock("../../../services/storyService", () => ({
  listStories: vi.fn(),
  createStory: vi.fn(),
  updateStory: vi.fn(),
  moveStoryToTrash: vi.fn(),
}));

vi.mock("../hooks/useStoryCovers", () => ({
  useStoryCovers: () => coverMocks,
}));

vi.mock("../../../components/layout/Topbar", () => ({
  Topbar: (props: Record<string, unknown>) => {
    topbarProps.current = props;
    return <div data-testid="topbar" />;
  },
}));

vi.mock("../../../components/layout/Sidebar", () => ({
  Sidebar: () => <div data-testid="sidebar" />,
}));

vi.mock("../components/Bookshelf/Bookshelf", () => ({
  Bookshelf: (props: Record<string, unknown>) => {
    bookshelfProps.current = props;
    return <div data-testid="bookshelf" />;
  },
}));

vi.mock("../components/CreateStoryModal/CreateStoryModal", () => ({
  CreateStoryModal: () => <div data-testid="create-story-modal" />,
}));

vi.mock("../components/EditStoryModal/EditStoryModal", () => ({
  EditStoryModal: () => <div data-testid="edit-story-modal" />,
}));

const story: ApiStory = {
  id: 1,
  title: "História de teste",
  genre: null,
  tone: null,
  summary: null,
  mainIdea: null,
  status: "draft",
  createdAt: null,
  updatedAt: null,
  deletedAt: null,
  coverImagePath: "covers/test.png",
};

describe("LibraryPage states", () => {
  beforeEach(() => {
    vi.mocked(listStories).mockReset();
    coverMocks.loadStoryCovers.mockReset();
    topbarProps.current = {};
    bookshelfProps.current = {};
  });

  it("mantém o carregamento até a lista de histórias responder", () => {
    vi.mocked(listStories).mockReturnValue(new Promise(() => {}));

    render(<LibraryPage />);

    expect(screen.getByRole("status")).toHaveTextContent("states.loading");
    expect(screen.queryByText("states.emptyTitle")).not.toBeInTheDocument();
  });

  it("mostra um estado vazio com ação para criar a primeira história", async () => {
    vi.mocked(listStories).mockResolvedValue([]);

    render(<LibraryPage />);

    expect(await screen.findByText("states.emptyTitle")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "states.emptyAction" })).toBeInTheDocument();
    expect(screen.queryByTestId("bookshelf")).not.toBeInTheDocument();
  });

  it("diferencia falha de lista vazia e permite tentar novamente", async () => {
    vi.mocked(listStories)
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce([story]);

    render(<LibraryPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent("errors.loadStories");
    fireEvent.click(screen.getByRole("button", { name: "states.retry" }));

    expect(await screen.findByTestId("bookshelf")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("exibe as histórias sem esperar o carregamento das capas", async () => {
    vi.mocked(listStories).mockResolvedValue([story]);
    coverMocks.loadStoryCovers.mockReturnValue(new Promise(() => {}));

    render(<LibraryPage />);

    expect(await screen.findByTestId("bookshelf")).toBeInTheDocument();
    await waitFor(() => expect(coverMocks.loadStoryCovers).toHaveBeenCalledWith([story]));
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("filtra histórias por título, gênero e descrição sem nova chamada ao backend", async () => {
    const secondStory = { ...story, id: 2, title: "Castelo Solar", genre: "Fantasia", summary: "Uma jornada luminosa" };
    vi.mocked(listStories).mockResolvedValue([story, secondStory]);

    render(<LibraryPage />);
    expect(await screen.findByTestId("bookshelf")).toBeInTheDocument();

    const onSearchChange = topbarProps.current.onSearchChange as (value: string) => void;
    onSearchChange("fantasia");

    await waitFor(() => {
      const shelves = bookshelfProps.current.shelves as Array<Array<{ title?: string }>>;
      expect(shelves.flat().map((item) => item.title)).toEqual(["Castelo Solar"]);
    });
    expect(listStories).toHaveBeenCalledTimes(1);
  });
});
