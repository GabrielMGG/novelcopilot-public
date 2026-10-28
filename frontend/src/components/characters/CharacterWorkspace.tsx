import { useEffect, useMemo, useState } from "react";
import {
    createCharacter,
    deleteCharacter,
    listCharactersByStory,
    updateCharacter,
} from "../../services/characterService";
import type {
    ApiCharacter,
    CharacterImportance,
    CharacterNarrativeRole,
    CreateCharacterPayload,
} from "../../types/character";
import "./CharacterWorkspace.css";
import { useTranslation } from "react-i18next";


type CharacterWorkspaceProps = {
    storyId: number;
};
type CharacterMainViewMode = "sheet" | "gallery";

type CharacterFormTab = "general" | "psychology" | "arc";

const emptyCharacterForm: CreateCharacterPayload = {
    name: "",
    role: "",
    importance: "",
    narrativeRole: "",
    description: "",
    appearance: "",
    personality: "",
    goal: "",
    backstory: "",
    conflict: "",
    relationships: "",
    notes: "",
    imageUrl: "",
    imagePrompt: "",
};

const importanceOptions: {
    value: CharacterImportance;
    labelKey: string;
}[] = [
        { value: "", labelKey: "importance.empty" },
        { value: "MAIN", labelKey: "importance.main" },
        { value: "SUPPORTING", labelKey: "importance.supporting" },
        { value: "MINOR", labelKey: "importance.minor" },
    ];

const narrativeRoleOptions: {
    value: CharacterNarrativeRole;
    labelKey: string;
}[] = [
        { value: "", labelKey: "narrativeRole.empty" },
        { value: "PROTAGONIST", labelKey: "narrativeRole.protagonist" },
        { value: "ANTAGONIST", labelKey: "narrativeRole.antagonist" },
        { value: "VILLAIN", labelKey: "narrativeRole.villain" },
        { value: "LOVE_INTEREST", labelKey: "narrativeRole.loveInterest" },
        { value: "MENTOR", labelKey: "narrativeRole.mentor" },
        { value: "ALLY", labelKey: "narrativeRole.ally" },
        { value: "RIVAL", labelKey: "narrativeRole.rival" },
        { value: "OTHER", labelKey: "narrativeRole.other" },
    ];

function characterToForm(character: ApiCharacter): CreateCharacterPayload {
    return {
        name: character.name ?? "",
        role: character.role ?? "",
        importance: character.importance ?? "",
        narrativeRole: character.narrativeRole ?? "",
        description: character.description ?? "",
        appearance: character.appearance ?? "",
        personality: character.personality ?? "",
        goal: character.goal ?? "",
        backstory: character.backstory ?? "",
        conflict: character.conflict ?? "",
        relationships: character.relationships ?? "",
        notes: character.notes ?? "",
        imageUrl: character.imageUrl ?? "",
        imagePrompt: character.imagePrompt ?? "",
    };
}

function areCharacterFormsEqual(
    firstForm: CreateCharacterPayload,
    secondForm: CreateCharacterPayload
) {
    return JSON.stringify(firstForm) === JSON.stringify(secondForm);
}

export function CharacterWorkspace({ storyId }: CharacterWorkspaceProps) {
    const [characters, setCharacters] = useState<ApiCharacter[]>([]);
    const [selectedCharacterId, setSelectedCharacterId] = useState<number | null>(
        null
    );
    const [form, setForm] = useState<CreateCharacterPayload>(emptyCharacterForm);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [message, setMessage] = useState("");

    const selectedCharacter = useMemo(
        () => characters.find((character) => character.id === selectedCharacterId),
        [characters, selectedCharacterId]
    );


    const [characterSearchText, setCharacterSearchText] = useState("");
    const [characterMainViewMode, setCharacterMainViewMode] =
        useState<CharacterMainViewMode>("sheet");

    const [activeCharacterFormTab, setActiveCharacterFormTab] =
        useState<CharacterFormTab>("general");
    const filteredCharacters = useMemo(() => {
        const normalizedSearchText = characterSearchText.trim().toLowerCase();

        if (!normalizedSearchText) {
            return characters;
        }

        return characters.filter((character) => {
            const searchableContent = [
                character.name,
                character.role,
                character.importance,
                character.narrativeRole,
                character.description,
            ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();

            return searchableContent.includes(normalizedSearchText);
        });
    }, [characters, characterSearchText]);

    const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
    const [savedForm, setSavedForm] =
        useState<CreateCharacterPayload>(emptyCharacterForm);

    const hasUnsavedChanges = !areCharacterFormsEqual(form, savedForm);

    const { t } = useTranslation("characters");

    function getImportanceLabel(importance: CharacterImportance) {
        const labels = {
            "": t("importance.empty"),
            MAIN: t("importance.main"),
            SUPPORTING: t("importance.supporting"),
            MINOR: t("importance.minor"),
        };

        return labels[importance];
    }

    function getNarrativeRoleLabel(narrativeRole: CharacterNarrativeRole) {
        const labels = {
            "": t("narrativeRole.empty"),
            PROTAGONIST: t("narrativeRole.protagonist"),
            ANTAGONIST: t("narrativeRole.antagonist"),
            VILLAIN: t("narrativeRole.villain"),
            LOVE_INTEREST: t("narrativeRole.loveInterest"),
            MENTOR: t("narrativeRole.mentor"),
            ALLY: t("narrativeRole.ally"),
            RIVAL: t("narrativeRole.rival"),
            OTHER: t("narrativeRole.other"),
        };

        return labels[narrativeRole];
    }



    useEffect(() => {
        async function loadCharacters() {
            try {
                setIsLoading(true);
                setMessage("");

                const charactersFromApi = await listCharactersByStory(storyId);
                setCharacters(charactersFromApi);

                if (charactersFromApi.length > 0) {
                    setSelectedCharacterId(charactersFromApi[0].id);
                    const firstCharacterForm = characterToForm(charactersFromApi[0]);

                    setForm(firstCharacterForm);
                    setSavedForm(firstCharacterForm);
                } else {
                    setSelectedCharacterId(null);
                    setForm(emptyCharacterForm);
                    setSavedForm(emptyCharacterForm);
                }
            } catch {
                setMessage(t("loadError"));
            } finally {
                setIsLoading(false);
            }
        }

        loadCharacters();
    }, [storyId, t]);

    function updateField(field: keyof CreateCharacterPayload, value: string) {
        setForm((currentForm) => ({
            ...currentForm,
            [field]: value,
        }));
    }

    function handleSelectCharacter(character: ApiCharacter) {
        setSelectedCharacterId(character.id);
        const selectedForm = characterToForm(character);

        setForm(selectedForm);
        setSavedForm(selectedForm);
        setActiveCharacterFormTab("general");
        setMessage("");
    }

    function handleNewCharacter() {
        setSelectedCharacterId(null);
        setForm(emptyCharacterForm);
        setSavedForm(emptyCharacterForm);
        setActiveCharacterFormTab("general");
        setMessage("");
    }

    async function handleSaveCharacter() {
        if (!form.name.trim()) {
            setMessage(t("nameRequired"));
            return;
        }

        try {
            setIsSaving(true);
            setMessage("");

            if (selectedCharacter) {
                const updatedCharacter = await updateCharacter(selectedCharacter.id, form);

                setCharacters((currentCharacters) =>
                    currentCharacters.map((character) =>
                        character.id === updatedCharacter.id ? updatedCharacter : character
                    )
                );

                setSelectedCharacterId(updatedCharacter.id);
                const updatedForm = characterToForm(updatedCharacter);

                setForm(updatedForm);
                setSavedForm(updatedForm);
                setMessage(t("updated"));
                return;
            }

            const createdCharacter = await createCharacter(storyId, form);

            setCharacters((currentCharacters) => [...currentCharacters, createdCharacter]);
            setSelectedCharacterId(createdCharacter.id);
            const createdForm = characterToForm(createdCharacter);

            setForm(createdForm);
            setSavedForm(createdForm);
            setMessage(t("created"));
        } catch {
            setMessage(t("saveError"));
        } finally {
            setIsSaving(false);
        }
    }

    function handleRequestDeleteCharacter() {
        if (!selectedCharacter) {
            return;
        }

        setIsDeleteConfirmOpen(true);
    }

    async function handleDeleteCharacter() {
        if (!selectedCharacter) {
            return;
        }

        try {
            setIsSaving(true);
            setIsDeleteConfirmOpen(false);

            await deleteCharacter(selectedCharacter.id);

            const remainingCharacters = characters.filter(
                (character) => character.id !== selectedCharacter.id
            );

            setCharacters(remainingCharacters);

            if (remainingCharacters.length > 0) {
                setSelectedCharacterId(remainingCharacters[0].id);
                const nextForm = characterToForm(remainingCharacters[0]);

                setForm(nextForm);
                setSavedForm(nextForm);
            } else {
                setSelectedCharacterId(null);
                setForm(emptyCharacterForm);
                setSavedForm(emptyCharacterForm);
            }

            setMessage(t("deleted"));
        } catch {
            setMessage(t("deleteError"));
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <section className="character-workspace">
            <aside className="character-list-panel">
                <div className="character-list-header">
                    <div className="character-list-header-top">
                        <h2>{t("title")}</h2>
                        <button type="button" className="character-new-button" onClick={handleNewCharacter}>
                            {t("new")}
                        </button>
                    </div>
                    <p className="character-list-count">
                        {t("count", { count: characters.length })}
                    </p>
                </div>
                <div className="character-sidebar-tools">
                    <input
                        className="character-search-input"
                        value={characterSearchText}
                        onChange={(event) => setCharacterSearchText(event.target.value)}
                        placeholder={t("search.placeholder")}
                    />

                    <div className="character-view-toggle">
                        <button
                            className={characterMainViewMode === "sheet" ? "is-active" : ""}
                            type="button"
                            onClick={() => setCharacterMainViewMode("sheet")}
                        >
                            {t("viewMode.sheet")}
                        </button>

                        <button
                            className={characterMainViewMode === "gallery" ? "is-active" : ""}
                            type="button"
                            onClick={() => setCharacterMainViewMode("gallery")}
                        >
                            {t("viewMode.gallery")}
                        </button>
                    </div>
                </div>

                {isLoading && <p className="character-state">{t("loading")}</p>}

                {!isLoading && characters.length === 0 && (
                    <p className="character-state">{t("empty")}</p>
                )}

                <div className="character-list">
                    {filteredCharacters.map((character) => (
                        <button
                            key={character.id}
                            className={`character-list-item ${character.id === selectedCharacterId ? "is-active" : ""
                                }`}
                            type="button"
                            onClick={() => {
                                handleSelectCharacter(character);
                                setCharacterMainViewMode("sheet");
                            }}
                        >
                            <strong>{character.name}</strong>

                            <span>
                                {character.narrativeRole
                                    ? getNarrativeRoleLabel(character.narrativeRole)
                                    : character.role || t("noRole")}
                            </span>

                            {character.importance && (
                                <small>{getImportanceLabel(character.importance)}</small>
                            )}
                        </button>
                    ))}
                </div>
            </aside>

            <div className="character-form-panel">
                {characterMainViewMode === "sheet" && (
                    <>
                        <div className="character-form-header">
                            <div>
                                <p>{t("formEyebrow")}</p>


                                {hasUnsavedChanges && (
                                    <span className="character-unsaved-badge">
                                        {t("unsavedChanges")}
                                    </span>
                                )}
                            </div>

                            <div className="character-form-actions">
                                {selectedCharacter && (
                                    <button
                                        className="character-danger-button"
                                        type="button"
                                        onClick={handleRequestDeleteCharacter}
                                        disabled={isSaving}
                                    >
                                        {t("delete")}
                                    </button>
                                )}

                                <button
                                    className="character-save-button"
                                    type="button"
                                    onClick={handleSaveCharacter}
                                    disabled={isSaving || !hasUnsavedChanges}
                                >
                                    {isSaving ? t("saving") : t("save")}
                                </button>
                            </div>
                        </div>

                        {message && <p className="character-message">{message}</p>}

                        <div className="character-detail-layout">
                            <aside className="character-media-card">
                                <div className="character-image-preview">
                                    {form.imageUrl ? (
                                        <img src={form.imageUrl} alt={form.name || t("newCharacter")} />
                                    ) : (
                                        <span>{form.name ? form.name.slice(0, 1).toUpperCase() : "?"}</span>
                                    )}
                                </div>

                                <label className="character-field">
                                    <span>{t("fields.imageUrl")}</span>
                                    <input
                                        value={form.imageUrl}
                                        onChange={(event) => updateField("imageUrl", event.target.value)}
                                        placeholder={t("placeholders.imageUrl")}
                                    />
                                </label>

                                <label className="character-field">
                                    <span>{t("fields.imagePrompt")}</span>
                                    <textarea
                                        value={form.imagePrompt}
                                        onChange={(event) => updateField("imagePrompt", event.target.value)}
                                        rows={4}
                                    />
                                </label>

                                <label className="character-field">
                                    <span>{t("fields.importance")}</span>
                                    <select
                                        value={form.importance}
                                        onChange={(event) =>
                                            updateField("importance", event.target.value as CharacterImportance)
                                        }
                                    >
                                        {importanceOptions.map((option) => (
                                            <option key={option.value || "empty"} value={option.value}>
                                                {t(option.labelKey)}
                                            </option>
                                        ))}
                                    </select>
                                </label>

                                <label className="character-field">
                                    <span>{t("fields.narrativeRole")}</span>
                                    <select
                                        value={form.narrativeRole}
                                        onChange={(event) =>
                                            updateField(
                                                "narrativeRole",
                                                event.target.value as CharacterNarrativeRole
                                            )
                                        }
                                    >
                                        {narrativeRoleOptions.map((option) => (
                                            <option key={option.value || "empty"} value={option.value}>
                                                {t(option.labelKey)}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                            </aside>

                            <div className="character-detail-content">
                                <div className="character-form-tabs">
                                    <button
                                        className={activeCharacterFormTab === "general" ? "is-active" : ""}
                                        type="button"
                                        onClick={() => setActiveCharacterFormTab("general")}
                                    >
                                        {t("tabs.general")}
                                    </button>

                                    <button
                                        className={activeCharacterFormTab === "psychology" ? "is-active" : ""}
                                        type="button"
                                        onClick={() => setActiveCharacterFormTab("psychology")}
                                    >
                                        {t("tabs.psychology")}
                                    </button>

                                    <button
                                        className={activeCharacterFormTab === "arc" ? "is-active" : ""}
                                        type="button"
                                        onClick={() => setActiveCharacterFormTab("arc")}
                                    >
                                        {t("tabs.arc")}
                                    </button>
                                </div>

                                {activeCharacterFormTab === "general" && (
                                    <>
                                        <div className="character-form-grid">
                                            <label className="character-field">
                                                <span>{t("fields.name")}</span>
                                                <input
                                                    value={form.name}
                                                    onChange={(event) => updateField("name", event.target.value)}
                                                    placeholder={t("placeholders.name")}
                                                />
                                            </label>

                                            <label className="character-field">
                                                <span>{t("fields.role")}</span>
                                                <input
                                                    value={form.role}
                                                    onChange={(event) => updateField("role", event.target.value)}
                                                    placeholder={t("placeholders.role")}
                                                />
                                            </label>
                                        </div>

                                        <div className="character-form-sections">
                                            <label className="character-field">
                                                <span>{t("fields.description")}</span>
                                                <textarea
                                                    value={form.description}
                                                    onChange={(event) => updateField("description", event.target.value)}
                                                    rows={4}
                                                />
                                            </label>

                                            <label className="character-field">
                                                <span>{t("fields.appearance")}</span>
                                                <textarea
                                                    value={form.appearance}
                                                    onChange={(event) => updateField("appearance", event.target.value)}
                                                    rows={4}
                                                />
                                            </label>
                                        </div>
                                    </>
                                )}
                                {activeCharacterFormTab === "psychology" && (
                                    <div className="character-form-sections">
                                        <label className="character-field">
                                            <span>{t("fields.personality")}</span>
                                            <textarea
                                                value={form.personality}
                                                onChange={(event) => updateField("personality", event.target.value)}
                                                rows={4}
                                            />
                                        </label>

                                        <label className="character-field">
                                            <span>{t("fields.goal")}</span>
                                            <textarea
                                                value={form.goal}
                                                onChange={(event) => updateField("goal", event.target.value)}
                                                rows={4}
                                            />
                                        </label>

                                        <label className="character-field">
                                            <span>{t("fields.conflict")}</span>
                                            <textarea
                                                value={form.conflict}
                                                onChange={(event) => updateField("conflict", event.target.value)}
                                                rows={4}
                                            />
                                        </label>
                                    </div>
                                )}
                                {activeCharacterFormTab === "arc" && (
                                    <div className="character-form-sections">
                                        <label className="character-field">
                                            <span>{t("fields.backstory")}</span>
                                            <textarea
                                                value={form.backstory}
                                                onChange={(event) => updateField("backstory", event.target.value)}
                                                rows={4}
                                            />
                                        </label>

                                        <label className="character-field">
                                            <span>{t("fields.relationships")}</span>
                                            <textarea
                                                value={form.relationships}
                                                onChange={(event) => updateField("relationships", event.target.value)}
                                                rows={4}
                                            />
                                        </label>

                                        <label className="character-field">
                                            <span>{t("fields.notes")}</span>
                                            <textarea
                                                value={form.notes}
                                                onChange={(event) => updateField("notes", event.target.value)}
                                                rows={4}
                                            />
                                        </label>
                                    </div>
                                )}
                            </div>
                        </div>

                    </>
                )}

                {characterMainViewMode === "gallery" && (
                    <div className="character-main-gallery">
                        {filteredCharacters.map((character) => (
                            <button
                                key={character.id}
                                className="character-main-gallery-card"
                                type="button"
                                onClick={() => {
                                    handleSelectCharacter(character);
                                    setCharacterMainViewMode("sheet");
                                }}
                            >
                                <div className="character-main-gallery-image">
                                    {character.imageUrl ? (
                                        <img src={character.imageUrl} alt={character.name} />
                                    ) : (
                                        <span>{character.name.slice(0, 1).toUpperCase()}</span>
                                    )}
                                </div>

                                <div>
                                    <strong>{character.name}</strong>
                                    <span>
                                        {character.narrativeRole
                                            ? getNarrativeRoleLabel(character.narrativeRole)
                                            : character.role || t("noRole")}
                                    </span>
                                </div>
                            </button>
                        ))}
                    </div>
                )}
            </div>
            {isDeleteConfirmOpen && selectedCharacter && (
                <div className="character-dialog-backdrop">
                    <div className="character-dialog">
                        <h2>{t("deleteConfirm.title")}</h2>

                        <p>
                            {t("deleteConfirm.message", {
                                name: selectedCharacter.name,
                            })}
                        </p>

                        <div className="character-dialog-actions">
                            <button
                                className="character-dialog-secondary"
                                type="button"
                                onClick={() => setIsDeleteConfirmOpen(false)}
                            >
                                {t("deleteConfirm.cancel")}
                            </button>

                            <button
                                className="character-dialog-danger"
                                type="button"
                                onClick={handleDeleteCharacter}
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
