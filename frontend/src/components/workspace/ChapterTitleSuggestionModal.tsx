import { useTranslation } from "react-i18next";

type ChapterTitleSuggestionModalProps = {
  options: [string, string, string];
  onApply: (title: string) => void;
  onClose: () => void;
};

export function ChapterTitleSuggestionModal({
  options,
  onApply,
  onClose,
}: ChapterTitleSuggestionModalProps) {
  const { t } = useTranslation("workspace");

  return (
    <div className="workspace-dialog-backdrop" role="presentation" onMouseDown={onClose}>
      <div
        className="workspace-dialog chapter-title-suggestion-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="chapter-title-suggestion-heading"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h2 id="chapter-title-suggestion-heading">{t("editor.chapterTitleAi.title")}</h2>
        <p>{t("editor.chapterTitleAi.description")}</p>

        <div className="chapter-title-suggestion-options">
          {options.map((title) => (
            <button key={title} type="button" onClick={() => onApply(title)}>
              {title}
            </button>
          ))}
        </div>

        <div className="workspace-dialog-actions">
          <button className="workspace-dialog-secondary" type="button" onClick={onClose}>
            {t("editor.chapterTitleAi.cancel")}
          </button>
        </div>
      </div>
    </div>
  );
}
