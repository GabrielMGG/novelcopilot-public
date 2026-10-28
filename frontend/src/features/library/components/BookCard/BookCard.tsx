import './BookCard.css';
import { useState, useRef, useEffect } from "react";
import type { StoryBook } from "../../types/library.types";
import { Link } from "react-router-dom";
import { MoreHorizontal, Edit, Trash2 } from "lucide-react";
import featherLogo from "../../../../assets/novelcopilot-feather-512.png";

type BookCardProps = {
  book: StoryBook;
  onEdit: (book: StoryBook) => void;
  onMoveToTrash: (book: StoryBook) => void;
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

export function BookCard({ book, onEdit, onMoveToTrash }: BookCardProps) {
  const coverClass = getBookCoverClass(book.id);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [imageError, setImageError] = useState(false);
  const [isImageLoaded, setIsImageLoaded] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    setTimeout(() => {
      setIsImageLoaded(false);
      setImageError(false);
    }, 0);
  }, [book.coverDisplayUrl]);

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

  let createdAtFormatted = "Data de criação indisponível";
  if (book.createdAt) {
    try {
      createdAtFormatted = `Criada em ${new Intl.DateTimeFormat("pt-BR").format(new Date(book.createdAt))}`;
    } catch {
      // Ignorar e usar texto padrão
    }
  }

  const hasCoverPath = Boolean(book.coverImagePath);
  const hasCoverDisplayUrl = Boolean(book.coverDisplayUrl);
  const isCoverLoading = hasCoverPath && !hasCoverDisplayUrl;
  const hasCustomCover = hasCoverDisplayUrl && !imageError;

  return (
    <article className={`book-wrapper ${coverClass} ${hasCustomCover ? "has-custom-cover" : ""}`}>
      <div className="book-heading">
        {hasCustomCover && (
          <h3 className="book-external-title" title={book.title}>
            {book.title}
          </h3>
        )}
        <span className="book-creation-date">{createdAtFormatted}</span>
      </div>

      <div className="book-visual">
        <Link className="book" to={`/stories/${book.id}/write`}>
          <span className="book-page-block" />
          <span className="book-page-edge-right" />
          <span className="book-page-edge-bottom" />
          <span className="book-back-cover" />

          <div className="book-front-cover">
            <div className={`book-default-cover ${isCoverLoading ? "book-default-cover--loading" : ""}`}>
              <img
                className="book-default-logo-background"
                src={featherLogo}
                alt=""
              />

              <div className="book-default-title-layer">
                <h3
                  className="book-default-cover-title"
                  title={book.title}
                >
                  {book.title}
                </h3>
              </div>
              
              {isCoverLoading && (
                <span className="book-cover-loading-shimmer" aria-hidden="true" />
              )}
            </div>

            {hasCustomCover && (
              <img
                className={`book-cover-image ${isImageLoaded ? "is-loaded" : ""}`}
                src={book.coverDisplayUrl as string}
                alt={`Capa de ${book.title}`}
                onLoad={() => setIsImageLoaded(true)}
                onError={() => setImageError(true)}
                style={{ position: 'absolute', inset: 0, zIndex: 5 }}
              />
            )}
          </div>

          <span className="book-spine" />
        </Link>

        <button
          type="button"
          className="book-menu"
          aria-label={`Ações da história ${book.title}`}
          onClick={(e) => {
            e.preventDefault();
            setIsMenuOpen(!isMenuOpen);
          }}
          ref={buttonRef}
        >
          <MoreHorizontal size={18} />
        </button>

        {isMenuOpen && (
          <div className="book-actions-menu" ref={menuRef}>
            <button
              className="book-action-item"
              type="button"
              onClick={(e) => {
                e.preventDefault();
                setIsMenuOpen(false);
                onEdit(book);
              }}
            >
              <Edit size={14} />
              Editar
            </button>
            <button
              className="book-action-item book-action-item--danger"
              type="button"
              onClick={(e) => {
                e.preventDefault();
                setIsMenuOpen(false);
                onMoveToTrash(book);
              }}
            >
              <Trash2 size={14} />
              Mover para a lixeira
            </button>
          </div>
        )}
      </div>
    </article>
  );
}

