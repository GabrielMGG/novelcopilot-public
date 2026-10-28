import "./LibraryPage.css";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Bookshelf } from "../components/Bookshelf/Bookshelf";
import { CreateStoryModal } from "../components/CreateStoryModal/CreateStoryModal";
import { Sidebar } from "../../../components/layout/Sidebar";
import { Topbar } from "../../../components/layout/Topbar";
import { createStory, listStories, updateStory, moveStoryToTrash } from "../../../services/storyService";
import type { ApiStory, CreateStoryPayload, UpdateStoryPayload } from "../../../types/story";
import type { ShelfItem, StoryBook } from "../types/library.types";
import { EditStoryModal } from "../components/EditStoryModal/EditStoryModal";
import { useAuth } from "../../../hooks/useAuth";
import { processStoryCover } from "../../../utils/processStoryCover";
import {
    uploadStoryCover,
    deleteStoryCover,
} from "../../../services/storage/storyCoverStorage";
import { useStoryCovers } from "../hooks/useStoryCovers";

const accentColors = [
    "#8FBFA0",
    "#E8A98C",
    "#A9B6C9",
    "#E3C25E",
    "#EFA46B",
];

export function LibraryPage() {
    const { t } = useTranslation("library");
    const { user } = useAuth();
    const [stories, setStories] = useState<ApiStory[]>([]);
    const [isLoadingStories, setIsLoadingStories] = useState(true);
    const [storiesError, setStoriesError] = useState("");
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isCreatingStory, setIsCreatingStory] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");
    const storiesRequestIdRef = useRef(0);

    const [editingStory, setEditingStory] = useState<ApiStory | null>(null);
    const [isSavingEdit, setIsSavingEdit] = useState(false);
    const [storyToTrash, setStoryToTrash] = useState<StoryBook | null>(null);
    const [isTrashing, setIsTrashing] = useState(false);

    const {
        getCoverDisplayUrl,
        loadStoryCovers,
        refreshStoryCover,
        removeStoryCover,
    } = useStoryCovers();

    const [shelfSize, setShelfSize] = useState(4);

    useEffect(() => {
        function handleResize() {
            const width = window.innerWidth;
            if (width >= 1200) setShelfSize(4);
            else if (width >= 900) setShelfSize(3);
            else if (width >= 600) setShelfSize(2);
            else setShelfSize(9999); // Put all items in a single shelf on mobile
        }
        window.addEventListener("resize", handleResize);
        handleResize(); // Initial check
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    const filteredStories = useMemo(() => {
        const normalizedQuery = searchQuery.trim().toLocaleLowerCase();
        if (!normalizedQuery) return stories;

        return stories.filter((story) =>
            [story.title, story.genre, story.summary, story.mainIdea]
                .filter(Boolean)
                .some((value) => value!.toLocaleLowerCase().includes(normalizedQuery))
        );
    }, [searchQuery, stories]);

    const shelves = useMemo(() => {
        const storyBooks: ShelfItem[] = filteredStories.map((story, index) => ({
            id: story.id,
            title: story.title,
            genre: story.genre,
            tone: story.tone,
            summary: story.summary,
            mainIdea: story.mainIdea,
            status: story.status,
            words: 0,
            updatedAt: story.updatedAt ?? "Recente",
            createdAt: story.createdAt,
            coverImagePath: story.coverImagePath,
            coverDisplayUrl: getCoverDisplayUrl(story.coverImagePath),
            accent: accentColors[index % accentColors.length],
        }));

        if (!searchQuery.trim()) storyBooks.unshift({ isAdd: true });

        const groupedStories: ShelfItem[][] = [];
        for (let index = 0; index < storyBooks.length; index += shelfSize) {
            groupedStories.push(storyBooks.slice(index, index + shelfSize));
        }

        return groupedStories;
    }, [filteredStories, getCoverDisplayUrl, searchQuery, shelfSize]);

    const loadStories = useCallback(async () => {
        const requestId = storiesRequestIdRef.current + 1;
        storiesRequestIdRef.current = requestId;
        setIsLoadingStories(true);
        setStoriesError("");

        try {
            const storiesFromApi = await listStories();

            if (storiesRequestIdRef.current !== requestId) return;

            setStories(storiesFromApi);
            setIsLoadingStories(false);
            void loadStoryCovers(storiesFromApi);
        } catch {
            if (storiesRequestIdRef.current !== requestId) return;

            setStories([]);
            setStoriesError(t("errors.loadStories"));
            setIsLoadingStories(false);
        }
    }, [t, loadStoryCovers]);

    useEffect(() => {
        const loadTimer = window.setTimeout(() => {
            void loadStories();
        }, 0);

        return () => {
            window.clearTimeout(loadTimer);
            storiesRequestIdRef.current += 1;
        };
    }, [loadStories]);

    function handleOpenCreateStory() {
        setIsCreateModalOpen(true);
    }

    function handleCloseCreateStory() {
        setIsCreateModalOpen(false);
    }

    async function handleCreateStory(payload: CreateStoryPayload, coverFile: File | null) {
        if (!user) return;
        
        let uploadedPath: string | null = null;

        try {
            setIsCreatingStory(true);

            const createdStory = await createStory({
                ...payload,
                coverImagePath: null,
            });

            let finalStory = createdStory;

            if (coverFile) {
                const processedBlob = await processStoryCover(coverFile);
                const uploadResult = await uploadStoryCover(processedBlob, user.id, createdStory.id);
                
                uploadedPath = uploadResult.path;

                finalStory = await updateStory(createdStory.id, {
                    title: createdStory.title,
                    genre: createdStory.genre ?? "",
                    tone: createdStory.tone ?? "",
                    summary: createdStory.summary ?? "",
                    mainIdea: createdStory.mainIdea ?? "",
                    status: createdStory.status ?? "draft",
                    coverImagePath: uploadedPath,
                });
                
                await refreshStoryCover(finalStory.coverImagePath!);
            }

            setStories((currentStories) => [...currentStories, finalStory]);
            setIsCreateModalOpen(false);
        } catch (error) {
            if (uploadedPath) {
                try {
                    await deleteStoryCover(uploadedPath);
                } catch (cleanupError) {
                    console.error("Falha ao limpar capa após erro:", cleanupError);
                }
            }
            console.error("Erro ao criar história com capa:", error);
            alert("Não foi possível criar a história.");
        } finally {
            setIsCreatingStory(false);
        }
    }

    function handleOpenEditStory(book: StoryBook) {
        const apiStory = stories.find(s => s.id === book.id);
        if (apiStory) {
            setEditingStory(apiStory);
        }
    }

    function handleCloseEditStory() {
        setEditingStory(null);
    }

    async function handleSaveEdit(
        storyId: number,
        payload: UpdateStoryPayload,
        coverFile: File | null,
        markedForRemoval: boolean,
        oldCoverPath: string | null
    ) {
        if (!user) return;

        try {
            setIsSavingEdit(true);

            if (coverFile) {
                const processedBlob = await processStoryCover(coverFile);
                const { path: newPath } = await uploadStoryCover(processedBlob, user.id, storyId);

                try {
                    const updatedStory = await updateStory(storyId, {
                        ...payload,
                        coverImagePath: newPath,
                    });

                    if (oldCoverPath) {
                        await deleteStoryCover(oldCoverPath).catch(() => {});
                    }

                    await refreshStoryCover(updatedStory.coverImagePath!);

                    setStories((currentStories) =>
                        currentStories.map((story) =>
                            story.id === updatedStory.id ? updatedStory : story
                        )
                    );
                    setEditingStory(null);
                } catch {
                    await deleteStoryCover(newPath).catch(() => {});
                    alert("Não foi possível salvar as alterações.");
                }
            } else if (markedForRemoval && oldCoverPath) {
                try {
                    const updatedStory = await updateStory(storyId, {
                        ...payload,
                        coverImagePath: null,
                    });

                    await deleteStoryCover(oldCoverPath).catch(() => {});

                    removeStoryCover(oldCoverPath);

                    setStories((currentStories) =>
                        currentStories.map((story) =>
                            story.id === updatedStory.id ? updatedStory : story
                        )
                    );
                    setEditingStory(null);
                } catch {
                    alert("Não foi possível salvar as alterações.");
                }
            } else {
                try {
                    const updatedStory = await updateStory(storyId, {
                        ...payload,
                        coverImagePath: oldCoverPath,
                    });

                    setStories((currentStories) =>
                        currentStories.map((story) =>
                            story.id === updatedStory.id ? updatedStory : story
                        )
                    );
                    setEditingStory(null);
                } catch {
                    alert("Não foi possível salvar as alterações.");
                }
            }
        } finally {
            setIsSavingEdit(false);
        }
    }

    function handleOpenTrashConfirm(book: StoryBook) {
        setStoryToTrash(book);
    }

    function handleCloseTrashConfirm() {
        setStoryToTrash(null);
    }

    async function handleConfirmTrash() {
        if (!storyToTrash) return;
        try {
            setIsTrashing(true);
            await moveStoryToTrash(storyToTrash.id);
            setStories((currentStories) =>
                currentStories.filter((story) => story.id !== storyToTrash.id)
            );
            setStoryToTrash(null);
        } catch {
            alert("Não foi possível mover a história para a lixeira.");
        } finally {
            setIsTrashing(false);
        }
    }

    return (
        <div className="app">
            <Topbar 
                onCreateStory={handleOpenCreateStory} 
                onOpenMenu={() => setIsMobileMenuOpen(true)}
                searchValue={searchQuery}
                onSearchChange={setSearchQuery}
            />

            <div className="layout">
                <Sidebar 
                    isOpen={isMobileMenuOpen} 
                    onClose={() => setIsMobileMenuOpen(false)} 
                />

                <main className="content">
                    <section className="welcome-section">
                        <h1 className="welcome-title">
                            {t("welcome.title")}
                            <span className="welcome-spark">⚡</span>
                        </h1>

                        <p className="welcome-subtitle">
                            {t("welcome.subtitleBefore")}{" "}
                            <span className="underline-squiggle">
                                {t("welcome.subtitleHighlight")}
                            </span>{" "}
                            {t("welcome.subtitleAfter")}
                        </p>
                    </section>

                    {isLoadingStories && (
                        <div className="library-state" role="status" aria-live="polite">
                            <span className="library-state-spinner" aria-hidden="true" />
                            <p>{t("states.loading")}</p>
                        </div>
                    )}

                    {!isLoadingStories && storiesError && (
                        <div className="library-state library-state-error" role="alert">
                            <h2>{t("states.errorTitle")}</h2>
                            <p>{storiesError}</p>
                            <button type="button" onClick={() => void loadStories()}>
                                {t("states.retry")}
                            </button>
                        </div>
                    )}

                    {!isLoadingStories && !storiesError && stories.length === 0 && (
                        <div className="library-state library-state-empty">
                            <h2>{t("states.emptyTitle")}</h2>
                            <p>{t("states.emptyDescription")}</p>
                            <button type="button" onClick={handleOpenCreateStory}>
                                {t("states.emptyAction")}
                            </button>
                        </div>
                    )}

                    {!isLoadingStories && !storiesError && stories.length > 0 && filteredStories.length === 0 && (
                        <div className="library-state library-state-empty" role="status">
                            <h2>Nenhuma história encontrada</h2>
                            <p>Tente pesquisar por outro título, gênero ou descrição.</p>
                            <button type="button" onClick={() => setSearchQuery("")}>Limpar pesquisa</button>
                        </div>
                    )}

                    {!isLoadingStories && !storiesError && filteredStories.length > 0 && (
                        <Bookshelf
                            shelves={shelves}
                            onCreateStory={handleOpenCreateStory}
                            onEditStory={handleOpenEditStory}
                            onMoveStoryToTrash={handleOpenTrashConfirm}
                        />
                    )}
                </main>
            </div>

            {isCreateModalOpen && (
                <CreateStoryModal
                    isCreating={isCreatingStory}
                    onClose={handleCloseCreateStory}
                    onCreate={handleCreateStory}
                />
            )}

            {editingStory && (
                <EditStoryModal
                    story={editingStory}
                    isSaving={isSavingEdit}
                    onClose={handleCloseEditStory}
                    onSave={handleSaveEdit}
                />
            )}

            {storyToTrash && (
                <div className="modal-backdrop">
                    <div className="story-modal">
                        <div className="modal-header">
                            <div>
                                <h2>Mover para a lixeira</h2>
                            </div>
                        </div>
                        
                        <p style={{ marginTop: '12px', fontSize: '15px', color: 'var(--ink)' }}>
                            Mover "{storyToTrash.title}" para a lixeira?
                        </p>
                        <p style={{ marginTop: '8px', fontSize: '13px', color: 'var(--ink-soft)' }}>
                            Você poderá restaurar esta história durante 7 dias. Depois desse prazo, ela poderá ser excluída definitivamente.
                        </p>

                        <div className="modal-actions" style={{ marginTop: '24px' }}>
                            <button className="secondary-button" type="button" onClick={handleCloseTrashConfirm} disabled={isTrashing}>
                                Cancelar
                            </button>
                            <button className="btn-primary" type="button" onClick={handleConfirmTrash} disabled={isTrashing} style={{ background: '#d93838', borderColor: '#d93838' }}>
                                {isTrashing ? "Movendo..." : "Mover para a lixeira"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

