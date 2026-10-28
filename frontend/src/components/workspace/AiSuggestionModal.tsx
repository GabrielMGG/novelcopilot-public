import { useTranslation } from "react-i18next";
import { IconClose } from "../icons/Icons";
import type { AiProseResult, AiSuggestionsResult } from "../../types/ai";
import "./AiSuggestionModal.css";

type AiSuggestionModalProps = {
  result: AiProseResult | AiSuggestionsResult;
  canReplaceSelection: boolean;
  onReplaceEditor?: () => void;
  onClose: () => void;
  onCopy: (text: string) => void;
  onInsertAtEnd: (text: string) => void;
  onReplaceSelection: (text: string) => void;
  isFirstDraft?: boolean;
};

export function AiSuggestionModal({
  result,
  canReplaceSelection,
  onReplaceEditor,
  onClose,
  onCopy,
  onInsertAtEnd,
  onReplaceSelection,
  isFirstDraft = false,
}: AiSuggestionModalProps) {
  const { t } = useTranslation("workspace");

  const title = isFirstDraft
    ? "Primeiro rascunho"
    : result.format === "suggestions"
    ? "Opções de Continuação"
    : t("ai.suggestion.modalTitle");

  return (
    <div className="ai-suggestion-backdrop">
      <section className="ai-suggestion-modal" role="dialog" aria-modal="true" aria-labelledby="ai-modal-title">
        <header className="ai-suggestion-header">
          <h2 id="ai-modal-title">{title}</h2>

          <button
            className="ai-suggestion-close icon-button"
            type="button"
            onClick={onClose}
            aria-label={t("ai.suggestion.discard")}
          >
            <IconClose />
          </button>
        </header>

        <div className="ai-suggestion-content">
          {result.format === "prose" ? (
            <div className="ai-prose-result">
              {result.content.split('\n').map((para, i) => (
                para ? <p key={i}>{para}</p> : <br key={i} />
              ))}
            </div>
          ) : (
            <div className="ai-suggestions-grid">
              {result.options.map((opt, i) => (
                <div key={i} className="ai-suggestion-card">
                  <h3>{opt.title} <span className="ai-suggestion-type-badge">{opt.type}</span></h3>
                  <p className="ai-suggestion-summary"><strong>Resumo:</strong> {opt.summary}</p>
                  <div className="ai-suggestion-snippet">
                    <strong>Trecho:</strong>
                    <p>{opt.snippet}</p>
                  </div>
                  <div className="ai-suggestion-card-actions">
                    <button type="button" onClick={() => onInsertAtEnd(opt.snippet)}>
                      Usar trecho (Inserir ao final)
                    </button>
                    <button type="button" onClick={() => onCopy(opt.snippet)}>
                      Copiar
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <footer className="ai-suggestion-actions">
          {result.format === "prose" && (
            <>
              <button type="button" onClick={() => onCopy(result.content)}>
                {t("ai.suggestion.copy")}
              </button>

              <button type="button" onClick={() => onInsertAtEnd(result.content)}>
                {t("ai.suggestion.insertAtEnd")}
              </button>

              <button
                type="button"
                onClick={() => onReplaceSelection(result.content)}
                disabled={!canReplaceSelection}
              >
                {t("ai.suggestion.replaceSelection")}
              </button>

              {onReplaceEditor && (
                <button type="button" onClick={onReplaceEditor}>
                  {t("ai.suggestion.replaceEditor")}
                </button>
              )}
            </>
          )}

          <button type="button" onClick={onClose}>
            {t("ai.suggestion.discard")}
          </button>
        </footer>
      </section>
    </div>
  );
}
