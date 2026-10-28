import { useEffect, useState } from "react";
import { Sidebar } from "../components/layout/Sidebar";
import { Topbar } from "../components/layout/Topbar";
import { TrashStoryCard } from "../components/trash/TrashStoryCard";
import { TrashEmptyState } from "../components/trash/TrashEmptyState";
import { PermanentDeleteModal } from "../components/trash/PermanentDeleteModal";
import {
  listTrashStories,
  restoreStory,
  permanentlyDeleteStory,
} from "../services/storyService";
import type { ApiStory } from "../types/story";

export function TrashPage() {
  const [trashStories, setTrashStories] = useState<ApiStory[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  
  const [restoringStoryId, setRestoringStoryId] = useState<number | null>(null);
  const [selectedStoryForPermanentDelete, setSelectedStoryForPermanentDelete] = useState<ApiStory | null>(null);
  const [isPermanentlyDeleting, setIsPermanentlyDeleting] = useState(false);

  useEffect(() => {
    async function loadTrash() {
      try {
        setIsLoading(true);
        setError("");
        const stories = await listTrashStories();
        setTrashStories(stories);
      } catch {
        setError("Não foi possível carregar a lixeira.");
      } finally {
        setIsLoading(false);
      }
    }
    loadTrash();
  }, []);

  async function handleRestore(story: ApiStory) {
    if (restoringStoryId) return;
    try {
      setRestoringStoryId(story.id);
      await restoreStory(story.id);
      setTrashStories((currentStories) =>
        currentStories.filter((s) => s.id !== story.id)
      );
    } catch {
      alert("Não foi possível restaurar a história.");
    } finally {
      setRestoringStoryId(null);
    }
  }

  function handleOpenDeleteModal(story: ApiStory) {
    setSelectedStoryForPermanentDelete(story);
  }

  function handleCloseDeleteModal() {
    if (!isPermanentlyDeleting) {
      setSelectedStoryForPermanentDelete(null);
    }
  }

  useEffect(() => {
    function handleGlobalKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && selectedStoryForPermanentDelete && !isPermanentlyDeleting) {
        setSelectedStoryForPermanentDelete(null);
      }
    }
    document.addEventListener("keydown", handleGlobalKeyDown);
    return () => {
      document.removeEventListener("keydown", handleGlobalKeyDown);
    };
  }, [selectedStoryForPermanentDelete, isPermanentlyDeleting]);

  async function handleConfirmDelete() {
    if (!selectedStoryForPermanentDelete) return;
    try {
      setIsPermanentlyDeleting(true);
      await permanentlyDeleteStory(selectedStoryForPermanentDelete.id);
      setTrashStories((currentStories) =>
        currentStories.filter((s) => s.id !== selectedStoryForPermanentDelete.id)
      );
      setSelectedStoryForPermanentDelete(null);
    } catch {
      alert("Não foi possível excluir definitivamente a história.");
    } finally {
      setIsPermanentlyDeleting(false);
    }
  }

  return (
    <div className="app">
      <Topbar 
          onCreateStory={() => {}} 
          onOpenMenu={() => setIsMobileMenuOpen(true)}
      />

      <div className="layout">
        <Sidebar 
            isOpen={isMobileMenuOpen} 
            onClose={() => setIsMobileMenuOpen(false)} 
        />

        <main className="content trash-page">
          <div className="trash-header">
            <h2>Lixeira</h2>
            <p>
              Histórias movidas para a lixeira ficam disponíveis por 7 dias antes da exclusão definitiva.
            </p>
            {!isLoading && !error && trashStories.length > 0 && (
              <span className="trash-count">
                {trashStories.length} {trashStories.length === 1 ? "história" : "histórias"}
              </span>
            )}
          </div>

          {isLoading && <p className="state-message">Carregando lixeira...</p>}
          
          {error && <p className="state-message state-message--error">{error}</p>}

          {!isLoading && !error && trashStories.length === 0 && (
            <TrashEmptyState />
          )}

          {!isLoading && !error && trashStories.length > 0 && (
            <div className="trash-list">
              {trashStories.map((story) => (
                <TrashStoryCard
                  key={story.id}
                  story={story}
                  isRestoring={restoringStoryId === story.id}
                  onRestore={handleRestore}
                  onRequestPermanentDelete={handleOpenDeleteModal}
                />
              ))}
            </div>
          )}
        </main>
      </div>

      {selectedStoryForPermanentDelete && (
        <PermanentDeleteModal
          storyName={selectedStoryForPermanentDelete.title}
          isDeleting={isPermanentlyDeleting}
          onClose={handleCloseDeleteModal}
          onConfirm={handleConfirmDelete}
        />
      )}
    </div>
  );
}
