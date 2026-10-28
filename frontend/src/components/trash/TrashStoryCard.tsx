import type { ApiStory } from "../../types/story";
import { MoreHorizontal, RotateCcw, Trash2 } from "lucide-react";
import { useState, useRef, useEffect } from "react";

type TrashStoryCardProps = {
  story: ApiStory;
  isRestoring: boolean;
  onRestore: (story: ApiStory) => void;
  onRequestPermanentDelete: (story: ApiStory) => void;
};

const bookColors = [
  "book-cover-brown",
  "book-cover-green",
  "book-cover-blue",
  "book-cover-purple",
  "book-cover-red",
  "book-cover-gold",
];

function getBookCoverClass(id: number) {
  return bookColors[id % bookColors.length];
}

function getRemainingTrashDays(deletedAt: string | null): number {
  if (!deletedAt) return 0;
  
  const deletedDate = new Date(deletedAt);
  const now = new Date();
  
  const diffTime = Math.abs(now.getTime() - deletedDate.getTime());
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
  
  const remaining = 7 - diffDays;
  if (remaining < 0) return 0;
  if (remaining > 7) return 7;
  return remaining;
}

function getRemainingDaysText(days: number): string {
  if (days === 7) return "Exclusão definitiva em 7 dias";
  if (days === 1) return "Exclusão definitiva amanhã";
  if (days === 0) return "Prazo de restauração encerrado";
  return `Exclusão definitiva em ${days} dias`;
}

export function TrashStoryCard({
  story,
  isRestoring,
  onRestore,
  onRequestPermanentDelete,
}: TrashStoryCardProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        isMenuOpen &&
        menuRef.current &&
        !menuRef.current.contains(event.target as Node) &&
        buttonRef.current &&
        !buttonRef.current.contains(event.target as Node)
      ) {
        setIsMenuOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (isMenuOpen && event.key === "Escape") {
        setIsMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isMenuOpen]);

  const coverClass = getBookCoverClass(story.id);
  const remainingDays = getRemainingTrashDays(story.deletedAt);
  const remainingDaysText = getRemainingDaysText(remainingDays);
  
  const deletedDateStr = story.deletedAt 
    ? new Date(story.deletedAt).toLocaleDateString()
    : "Desconhecido";

  return (
    <article className="trash-story-card">
      <div className={`trash-story-cover ${coverClass}`}>
        <span className="trash-story-cover-inner">{story.title}</span>
      </div>

      <div className="trash-story-content">
        <h3>{story.title}</h3>
        <div className="trash-story-meta">
          <span>{story.genre || "Sem gênero"}</span>
          <span className="dot-separator">•</span>
          <span>Movido em: {deletedDateStr}</span>
        </div>
        <p className="trash-story-countdown">{remainingDaysText}</p>
      </div>

      <div className="trash-story-actions">
        <button
          type="button"
          className="trash-restore-button"
          onClick={() => onRestore(story)}
          disabled={isRestoring || remainingDays === 0}
        >
          <RotateCcw size={16} />
          {isRestoring ? "Restaurando..." : "Restaurar"}
        </button>

        <div style={{ position: "relative" }}>
          <button
            type="button"
            className="book-menu trash-menu-button"
            aria-label={`Opções da lixeira para ${story.title}`}
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            ref={buttonRef}
          >
            <MoreHorizontal size={18} />
          </button>

          {isMenuOpen && (
            <div className="book-actions-menu" ref={menuRef}>
              <button
                className="book-action-item book-action-item--danger trash-delete-button"
                type="button"
                onClick={() => {
                  setIsMenuOpen(false);
                  onRequestPermanentDelete(story);
                }}
              >
                <Trash2 size={14} />
                Excluir definitivamente
              </button>
            </div>
          )}
        </div>
      </div>
    </article>
  );
}
