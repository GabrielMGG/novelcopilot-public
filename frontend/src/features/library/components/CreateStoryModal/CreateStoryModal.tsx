import './CreateStoryModal.css';
import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import type { CreateStoryPayload } from "../../../../types/story";
import { useAuth } from "../../../../hooks/useAuth";
import { StoryCoverField } from "../StoryCoverField/StoryCoverField";
import { IconClose } from "../../../../components/icons/Icons";

type CreateStoryModalProps = {
    isCreating: boolean;
    onClose: () => void;
    onCreate: (payload: CreateStoryPayload, coverFile: File | null) => Promise<void>;
};

type CreateStoryForm = {
    title: string;
    genres: string[];
    tone: string;
    summary: string;
    mainIdea: string;
};

const initialForm: CreateStoryForm = {
    title: "",
    genres: [],
    tone: "",
    summary: "",
    mainIdea: "",
};

export function CreateStoryModal({
    isCreating,
    onClose,
    onCreate,
}: CreateStoryModalProps) {
    const { t } = useTranslation(["common", "library"]);
    const { user } = useAuth();
    const [form, setForm] = useState<CreateStoryForm>(initialForm);
    const [titleError, setTitleError] = useState("");
    const [coverFile, setCoverFile] = useState<File | null>(null);

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
        field: Exclude<keyof CreateStoryForm, "genres">,
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



    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!form.title.trim()) {
            setTitleError(t("library:createModal.titleRequired"));
            return;
        }

        if (!user) {
            return;
        }

        await onCreate({
            title: form.title,
            genre: form.genres.join(", "),
            tone: form.tone,
            summary: form.summary,
            mainIdea: form.mainIdea,
        }, coverFile);
    }

    return (
        <div className="modal-backdrop">
            <form className="story-modal" onSubmit={handleSubmit}>
                <div className="modal-header">
                    <div>
                        <h2>{t("library:createModal.title")}</h2>
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

                <StoryCoverField selectedFile={coverFile} onFileSelect={setCoverFile} />

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
                    <textarea
                        value={form.summary}
                        onChange={(event) => updateField("summary", event.target.value)}
                        placeholder={t("library:createModal.summaryPlaceholder")}
                        rows={3}
                    />
                </label>

                <label className="form-field">
                    <span>{t("library:createModal.mainIdeaLabel")}</span>
                    <textarea
                        value={form.mainIdea}
                        onChange={(event) => updateField("mainIdea", event.target.value)}
                        placeholder={t("library:createModal.mainIdeaPlaceholder")}
                        rows={3}
                    />
                </label>

                <div className="modal-actions">
                    <button className="secondary-button" type="button" onClick={onClose}>
                        {t("actions.cancel")}
                    </button>

                    <button className="btn-primary" type="submit" disabled={isCreating}>
                        {isCreating ? t("actions.creating") : t("actions.createStory")}
                    </button>
                </div>
            </form>
        </div>
    );
}

