import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  createWorldNote,
  deleteWorldNote,
  listWorldNotesByStory,
  updateWorldNote,
} from "../../services/worldNoteService";
import {
  worldNoteCategoryOptions,
  worldNoteImportanceOptions,
  worldNoteStatusOptions,
} from "../../constants/worldNoteOptions";
import type {
  ApiWorldNote,
  CreateWorldNotePayload,
  WorldNoteCategory,
  WorldNoteImportance,
  WorldNoteStatus,
} from "../../types/worldNote";
import { WorldNoteCategoryIcon } from "./WorldNoteCategoryIcon";
import "./WorldWorkspace.css";

type WorldWorkspaceProps = {
  storyId: number;
};

const emptyWorldNoteForm: CreateWorldNotePayload = {
  title: "",
  category: "OTHER",
  importance: "MEDIUM",
  status: "CANON",
  tags: "",
  content: "",
};

function worldNoteToForm(worldNote: ApiWorldNote): CreateWorldNotePayload {
  return {
    title: worldNote.title ?? "",
    category: worldNote.category ?? "OTHER",
    importance: worldNote.importance ?? "MEDIUM",
    status: worldNote.status ?? "CANON",
    tags: worldNote.tags ?? "",
    content: worldNote.content ?? "",
  };
}

function areWorldNoteFormsEqual(
  firstForm: CreateWorldNotePayload,
  secondForm: CreateWorldNotePayload
) {
  return JSON.stringify(firstForm) === JSON.stringify(secondForm);
}

export function WorldWorkspace({ storyId }: WorldWorkspaceProps) {
  const { t } = useTranslation("world");

  const [worldNotes, setWorldNotes] = useState<ApiWorldNote[]>([]);
  const [selectedWorldNoteId, setSelectedWorldNoteId] = useState<number | null>(
    null
  );
  const [form, setForm] = useState<CreateWorldNotePayload>(emptyWorldNoteForm);
  const [savedForm, setSavedForm] =
    useState<CreateWorldNotePayload>(emptyWorldNoteForm);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [searchText, setSearchText] = useState("");
  const [categoryFilter, setCategoryFilter] = useState<WorldNoteCategory>("");
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);

  const selectedWorldNote = useMemo(
    () => worldNotes.find((worldNote) => worldNote.id === selectedWorldNoteId),
    [worldNotes, selectedWorldNoteId]
  );

  const hasUnsavedChanges = !areWorldNoteFormsEqual(form, savedForm);

  const filteredWorldNotes = useMemo(() => {
    const normalizedSearchText = searchText.trim().toLowerCase();

    return worldNotes.filter((worldNote) => {
      const matchesCategory =
        !categoryFilter || worldNote.category === categoryFilter;

      const searchableContent = [
        worldNote.title,
        worldNote.category,
        worldNote.importance,
        worldNote.status,
        worldNote.tags,
        worldNote.content,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      const matchesSearch =
        !normalizedSearchText ||
        searchableContent.includes(normalizedSearchText);

      return matchesCategory && matchesSearch;
    });
  }, [worldNotes, searchText, categoryFilter]);

  useEffect(() => {
    async function loadWorldNotes() {
      try {
        setIsLoading(true);
        setMessage("");

        const worldNotesFromApi = await listWorldNotesByStory(storyId);
        setWorldNotes(worldNotesFromApi);

        if (worldNotesFromApi.length > 0) {
          const firstWorldNote = worldNotesFromApi[0];
          const firstForm = worldNoteToForm(firstWorldNote);

          setSelectedWorldNoteId(firstWorldNote.id);
          setForm(firstForm);
          setSavedForm(firstForm);
        } else {
          setSelectedWorldNoteId(null);
          setForm(emptyWorldNoteForm);
          setSavedForm(emptyWorldNoteForm);
        }
      } catch {
        setMessage(t("loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadWorldNotes();
  }, [storyId, t]);

  function getCategoryLabel(category: WorldNoteCategory) {
    const option = worldNoteCategoryOptions.find(
      (currentOption) => currentOption.value === category
    );

    return option ? t(option.labelKey) : t("category.other");
  }

  function getImportanceLabel(importance: WorldNoteImportance) {
    const option = worldNoteImportanceOptions.find(
      (currentOption) => currentOption.value === importance
    );

    return option ? t(option.labelKey) : t("importance.medium");
  }

  function getStatusLabel(status: WorldNoteStatus) {
    const option = worldNoteStatusOptions.find(
      (currentOption) => currentOption.value === status
    );

    return option ? t(option.labelKey) : t("status.canon");
  }

  function updateField(field: keyof CreateWorldNotePayload, value: string) {
    setForm((currentForm) => ({
      ...currentForm,
      [field]: value,
    }));
  }

  function handleSelectWorldNote(worldNote: ApiWorldNote) {
    const selectedForm = worldNoteToForm(worldNote);

    setSelectedWorldNoteId(worldNote.id);
    setForm(selectedForm);
    setSavedForm(selectedForm);
    setMessage("");
  }

  function handleNewWorldNote() {
    setSelectedWorldNoteId(null);
    setForm(emptyWorldNoteForm);
    setSavedForm(emptyWorldNoteForm);
    setMessage("");
  }

  async function handleSaveWorldNote() {
    if (!form.title.trim()) {
      setMessage(t("titleRequired"));
      return;
    }

    try {
      setIsSaving(true);
      setMessage("");

      if (selectedWorldNote) {
        const updatedWorldNote = await updateWorldNote(selectedWorldNote.id, form);
        const updatedForm = worldNoteToForm(updatedWorldNote);

        setWorldNotes((currentWorldNotes) =>
          currentWorldNotes.map((worldNote) =>
            worldNote.id === updatedWorldNote.id ? updatedWorldNote : worldNote
          )
        );
        setSelectedWorldNoteId(updatedWorldNote.id);
        setForm(updatedForm);
        setSavedForm(updatedForm);
        setMessage(t("updated"));
        return;
      }

      const createdWorldNote = await createWorldNote(storyId, form);
      const createdForm = worldNoteToForm(createdWorldNote);

      setWorldNotes((currentWorldNotes) => [
        ...currentWorldNotes,
        createdWorldNote,
      ]);
      setSelectedWorldNoteId(createdWorldNote.id);
      setForm(createdForm);
      setSavedForm(createdForm);
      setMessage(t("created"));
    } catch {
      setMessage(t("saveError"));
    } finally {
      setIsSaving(false);
    }
  }

  function handleRequestDeleteWorldNote() {
    if (!selectedWorldNote) {
      return;
    }

    setIsDeleteConfirmOpen(true);
  }

  async function handleDeleteWorldNote() {
    if (!selectedWorldNote) {
      return;
    }

    try {
      setIsSaving(true);
      setIsDeleteConfirmOpen(false);
      setMessage("");

      await deleteWorldNote(selectedWorldNote.id);

      const remainingWorldNotes = worldNotes.filter(
        (worldNote) => worldNote.id !== selectedWorldNote.id
      );

      setWorldNotes(remainingWorldNotes);

      if (remainingWorldNotes.length > 0) {
        const nextWorldNote = remainingWorldNotes[0];
        const nextForm = worldNoteToForm(nextWorldNote);

        setSelectedWorldNoteId(nextWorldNote.id);
        setForm(nextForm);
        setSavedForm(nextForm);
      } else {
        setSelectedWorldNoteId(null);
        setForm(emptyWorldNoteForm);
        setSavedForm(emptyWorldNoteForm);
      }

      setMessage(t("deleted"));
    } catch {
      setMessage(t("deleteError"));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="world-workspace">
      <aside className="world-sidebar">
        <div className="world-list-header">
          <div className="world-list-header-top">
            <h2>{t("title")}</h2>
            <button type="button" className="world-new-button" onClick={handleNewWorldNote}>
              {t("new")}
            </button>
          </div>
          <p className="world-list-count">{t("count", { count: worldNotes.length })}</p>
        </div>

        <div className="world-sidebar-tools">
          <input
            className="world-search-input"
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
            placeholder={t("searchPlaceholder")}
          />

          <select
            className="world-category-filter"
            value={categoryFilter}
            onChange={(event) =>
              setCategoryFilter(event.target.value as WorldNoteCategory)
            }
          >
            <option value="">{t("filterAllCategories")}</option>

            {worldNoteCategoryOptions
              .filter((option) => option.value)
              .map((option) => (
                <option key={option.value} value={option.value}>
                  {t(option.labelKey)}
                </option>
              ))}
          </select>
        </div>

        {isLoading && <p className="world-state">{t("loading")}</p>}

        {!isLoading && worldNotes.length === 0 && (
          <p className="world-state">{t("empty")}</p>
        )}

        <div className="world-entry-list">
          {filteredWorldNotes.map((worldNote) => (
            <button
              key={worldNote.id}
              className={`world-entry-item ${
                worldNote.id === selectedWorldNoteId ? "is-active" : ""
              }`}
              type="button"
              onClick={() => handleSelectWorldNote(worldNote)}
            >
              <span className="world-entry-icon">
                <WorldNoteCategoryIcon category={worldNote.category} />
              </span>

              <span className="world-entry-info">
                <strong>{worldNote.title}</strong>
                <small>
                  {getCategoryLabel(worldNote.category)} •{" "}
                  {getImportanceLabel(worldNote.importance)} •{" "}
                  {getStatusLabel(worldNote.status)}
                </small>
              </span>
            </button>
          ))}
        </div>
      </aside>

      <div className="world-editor-panel">
        <div className="world-editor-header">
          <div>
            <p>{t("formEyebrow")}</p>
            <h2>{selectedWorldNote ? selectedWorldNote.title : t("newEntry")}</h2>

            {hasUnsavedChanges && (
              <span className="world-unsaved-badge">
                {t("unsavedChanges")}
              </span>
            )}
          </div>

          <div className="world-editor-actions">
            {selectedWorldNote && (
              <button
                className="world-danger-button"
                type="button"
                onClick={handleRequestDeleteWorldNote}
                disabled={isSaving}
              >
                {t("delete")}
              </button>
            )}

            <button
              className="world-save-button"
              type="button"
              onClick={handleSaveWorldNote}
              disabled={isSaving || !hasUnsavedChanges}
            >
              {isSaving ? t("saving") : t("save")}
            </button>
          </div>
        </div>

        {message && <p className="world-message">{message}</p>}

        <div className="world-content-form world-content-form--title">
          <label className="world-field">
            <span>{t("fields.title")}</span>
            <input
              value={form.title}
              onChange={(event) => updateField("title", event.target.value)}
              placeholder={t("placeholders.title")}
            />
          </label>
        </div>

        <div className="world-metadata-bar">
          <label className="world-field">
            <span>{t("fields.category")}</span>
            <select
              value={form.category}
              onChange={(event) =>
                updateField("category", event.target.value as WorldNoteCategory)
              }
            >
              {worldNoteCategoryOptions.map((option) => (
                <option key={option.value || "empty"} value={option.value}>
                  {t(option.labelKey)}
                </option>
              ))}
            </select>
          </label>

          <label className="world-field">
            <span>{t("fields.importance")}</span>
            <select
              value={form.importance}
              onChange={(event) =>
                updateField(
                  "importance",
                  event.target.value as WorldNoteImportance
                )
              }
            >
              {worldNoteImportanceOptions.map((option) => (
                <option key={option.value || "empty"} value={option.value}>
                  {t(option.labelKey)}
                </option>
              ))}
            </select>
          </label>

          <label className="world-field">
            <span>{t("fields.status")}</span>
            <select
              value={form.status}
              onChange={(event) =>
                updateField("status", event.target.value as WorldNoteStatus)
              }
            >
              {worldNoteStatusOptions.map((option) => (
                <option key={option.value || "empty"} value={option.value}>
                  {t(option.labelKey)}
                </option>
              ))}
            </select>
          </label>

          <label className="world-field world-tags-field">
            <span>{t("fields.tags")}</span>
            <input
              value={form.tags}
              onChange={(event) => updateField("tags", event.target.value)}
              placeholder={t("placeholders.tags")}
            />
          </label>
        </div>

        <div className="world-content-form">
          <label className="world-field">
            <span>{t("fields.content")}</span>
            <textarea
              value={form.content}
              onChange={(event) => updateField("content", event.target.value)}
              placeholder={t("placeholders.content")}
            />
          </label>
        </div>
      </div>

      {isDeleteConfirmOpen && selectedWorldNote && (
        <div className="world-dialog-backdrop">
          <div className="world-dialog">
            <h2>{t("deleteConfirm.title")}</h2>

            <p>
              {t("deleteConfirm.message", {
                title: selectedWorldNote.title,
              })}
            </p>

            <div className="world-dialog-actions">
              <button
                className="world-dialog-secondary"
                type="button"
                onClick={() => setIsDeleteConfirmOpen(false)}
              >
                {t("deleteConfirm.cancel")}
              </button>

              <button
                className="world-dialog-danger"
                type="button"
                onClick={handleDeleteWorldNote}
                disabled={isSaving}
              >
                {isSaving ? t("saving") : t("deleteConfirm.confirm")}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
