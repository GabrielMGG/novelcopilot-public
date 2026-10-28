import './EditStoryModal.css';
import './EditStoryModal.css';
import { useState, useEffect, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import type { ApiStory, UpdateStoryPayload } from "../../../../types/story";
import { useAuth } from "../../../../hooks/useAuth";
import { StoryCoverField } from "../StoryCoverField/StoryCoverField";
import { createStoryCoverSignedUrl } from "../../../../services/storage/storyCoverStorage";
import { IconClose } from "../../../../components/icons/Icons";

type EditStoryModalProps = {
  story: ApiStory;
  isSaving: boolean;
  onClose: () => void;
  onSave: (
    storyId: number,
    payload: UpdateStoryPayload,
    coverFile: File | null,
    markedForRemoval: boolean,
    oldCoverPath: string | null
  ) => Promise<void>;
};

type EditStoryForm = {
  title: string;
  genres: string[];
  tone: string;
  summary: string;
  mainIdea: string;
  status: string;
};

export function EditStoryModal({
  story,
  isSaving,
  onClose,
  onSave,
}: EditStoryModalProps) {
  const { t } = useTranslation(["common", "library"]);
  const { user } = useAuth();

  const [form, setForm] = useState<EditStoryForm>(() => ({
    title: story.title || "",
    genres: story.genre ? story.genre.split(",").map((g) => g.trim()).filter(Boolean) : [],
    tone: story.tone || "",
    summary: story.summary || "",
    mainIdea: story.mainIdea || "",
    status: story.status || "draft",
  }));

  const [titleError, setTitleError] = useState("");
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [markedForRemoval, setMarkedForRemoval] = useState(false);
  const [coverError, setCoverError] = useState("");
  const [currentCoverUrl, setCurrentCoverUrl] = useState<string | null>(null);

  // Load signed URL for existing cover
  useEffect(() => {
    if (!story.coverImagePath) return;

    let cancelled = false;

    async function loadSignedUrl() {
      try {
        const url = await createStoryCoverSignedUrl(story.coverImagePath as string);
        if (!cancelled) {
          setCurrentCoverUrl(url);
        }
      } catch {
        // Silently fail - will show default cover
      }
    }

    loadSignedUrl();

    return () => {
      cancelled = true;
    };
  }, [story.coverImagePath]);

  const genreOptions = [
    t("library:createModal.genres.fantasy"),
    t("library:createModal.genres.sciFi"),
    t("library:createModal.genres.romance"),
    t("library:createModal.genres.mystery"),
    t("library:createModal.genres.horror"),
    t("library:createModal.genres.thriller"),
    t("library:createModal.genres.adventure"),
    t("library:createModal.genres.drama"),
    t("library:createModal.genres.historical"),
  ];

  function updateField(
    field: keyof EditStoryForm,
    value: string
  ) {
    setForm((currentForm) => ({
      ...currentForm,
      [field]: value,
    }));

    if (field === "title" && value.trim()) {
      setTitleError("");
    }
  }

  function toggleGenre(genre: string) {
    setForm((currentForm) => {
      const genreAlreadySelected = currentForm.genres.includes(genre);

      return {
        ...currentForm,
        genres: genreAlreadySelected
          ? currentForm.genres.filter((currentGenre) => currentGenre !== genre)
          : [...currentForm.genres, genre],
      };
    });
  }

  function handleRemoveCurrentCover() {
    setMarkedForRemoval((prev) => !prev);
    setCoverFile(null);
  }

  function handleFileSelect(file: File | null) {
    setCoverFile(file);
    if (file) {
      setMarkedForRemoval(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!form.title.trim()) {
      setTitleError(t("library:createModal.titleRequired"));
      return;
    }

    if (!user) {
      return;
    }

    setCoverError("");

    // Determine what coverImageUrl to send
    // If there's a new file, LibraryPage handles upload + update
    // If marked for removal, send null
    // Otherwise, preserve the existing path
    let coverImagePathForPayload = story.coverImagePath;
    if (markedForRemoval && !coverFile) {
      coverImagePathForPayload = null;
    }

    await onSave(
      story.id,
      {
        title: form.title,
        genre: form.genres.join(", "),
        tone: form.tone,
        summary: form.summary,
        mainIdea: form.mainIdea,
        status: form.status,
        coverImagePath: coverImagePathForPayload,
      },
      coverFile,
      markedForRemoval,
      story.coverImagePath
    );
  }

  return (
    <div className="modal-backdrop">
      <form className="story-modal" onSubmit={handleSubmit}>
        <div className="modal-header">
          <div>
            <h2>Editar História</h2>
          </div>

          <button
            className="modal-close icon-button"
            type="button"
            aria-label={t("library:createModal.close")}
            onClick={onClose}
          >
        <IconClose />
      </button>
        </div>

        <label className="form-field">
          <span>
            {t("library:createModal.titleLabel")}{" "}
            <strong className="required-mark">*</strong>
          </span>

          <input
            value={form.title}
            onChange={(event) => updateField("title", event.target.value)}
            placeholder={t("library:createModal.titlePlaceholder")}
            aria-invalid={Boolean(titleError)}
          />

          {titleError && <small className="field-error">{titleError}</small>}
        </label>

        <StoryCoverField
          currentCoverUrl={currentCoverUrl}
          selectedFile={coverFile}
          onFileSelect={handleFileSelect}
          markedForRemoval={markedForRemoval}
          onRemoveCurrentCover={handleRemoveCurrentCover}
          error={coverError}
          disabled={isSaving}
        />

        <label className="form-field">
          <span>Status</span>
          <select
            value={form.status}
            onChange={(event) => updateField("status", event.target.value)}
          >
            <option value="draft">Rascunho</option>
            <option value="in_progress">Em andamento</option>
            <option value="completed">Concluída</option>
          </select>
        </label>

        <div className="form-field">
          <span>{t("library:createModal.genreLabel")}</span>

          <div className="genre-chip-list">
            {genreOptions.map((genre) => {
              const isSelected = form.genres.includes(genre);

              return (
                <button
                  key={genre}
                  className={`genre-chip${isSelected ? " is-selected" : ""}`}
                  type="button"
                  onClick={() => toggleGenre(genre)}
                >
                  {genre}
                </button>
              );
            })}
          </div>
        </div>

        <label className="form-field">
          <span>{t("library:createModal.toneLabel")}</span>

          <input
            value={form.tone}
            onChange={(event) => updateField("tone", event.target.value)}
            placeholder={t("library:createModal.tonePlaceholder")}
          />
        </label>

        <label className="form-field">
          <span>{t("library:createModal.summaryLabel")}</span>
          <p className="field-help">Descrição breve da obra, usada na biblioteca e como contexto geral para a IA.</p>
          <textarea
            value={form.summary}
            onChange={(event) => updateField("summary", event.target.value)}
            placeholder={t("library:createModal.summaryPlaceholder")}
            rows={3}
          />
        </label>

        <label className="form-field">
          <span>{t("library:createModal.mainIdeaLabel")}</span>
          <p className="field-help">O conflito, tema ou direção principal que deve orientar a história e as sugestões da IA.</p>
          <textarea
            value={form.mainIdea}
            onChange={(event) => updateField("mainIdea", event.target.value)}
            placeholder={t("library:createModal.mainIdeaPlaceholder")}
            rows={3}
          />
        </label>

        <div className="modal-actions">
          <button className="secondary-button" type="button" onClick={onClose} disabled={isSaving}>
            {t("actions.cancel")}
          </button>

          <button className="btn-primary" type="submit" disabled={isSaving}>
            {isSaving ? "Salvando..." : "Salvar alterações"}
          </button>
        </div>
      </form>
    </div>
  );
}




