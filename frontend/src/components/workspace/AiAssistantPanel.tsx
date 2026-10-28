import { useEffect, useState } from "react";
import type { AiActionType, AiGenerateResult, AiHistoryItem, AiHistoryPage, AiReviewResult, SuggestContinuationOption } from "../../types/ai";
import type { AiEstimateResponse } from "../../types/credits";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { IconChevronLeftDouble, IconChevronRightDouble, IconMessageSparkle } from "../icons/Icons";
import { useCredits } from "../../hooks/useCredits";
import { getChapterAiHistory } from "../../services/aiHistoryService";
import { parseReviewResult } from "../../services/aiService";
import { formatCreditAmount } from "../../utils/creditTransactions";
import "./AiAssistantPanel.css";

function formatChapterReviewAsText(review: AiReviewResult) {
  const findings = review.findings.map((item) =>
    `${item.severity.toUpperCase()} — ${item.issue}\nEvidência: ${item.evidence}\nImpacto: ${item.impact}\nSugestão: ${item.suggestion}`
  ).join("\n\n");
  return [review.summary, findings, ...review.strengths, ...review.limitations].filter(Boolean).join("\n\n").trim();
}

type AiAssistantAction = {
  title: string;
  description: string;
  actionType: AiActionType;
};

type AiAssistantPanelProps = {
  isOpen: boolean;
  isLoading: boolean;
  instructionText: string;
  resultText: string;
  errorMessage: string;
  actions: AiAssistantAction[];
  onToggleOpen: () => void;
  onInstructionChange: (value: string) => void;
  onGenerate: (actionType: AiActionType) => void;
  onOpenSuggestion: () => void;
  isMobileSheetOpen?: boolean;
  onOpenMobileSheet?: () => void;
  onCloseMobileSheet?: () => void;
  aiEstimate?: AiEstimateResponse | null;
  onConfirmEstimate?: () => void;
  storyId: number;
  chapterId: number | null;
  onOpenHistoryResult: (result: AiGenerateResult) => void;
};

export function AiAssistantPanel({
  isOpen,
  isLoading,
  instructionText,
  resultText,
  errorMessage,
  actions,
  onToggleOpen,
  onInstructionChange,
  onGenerate,
  onOpenSuggestion,
  isMobileSheetOpen = false,
  onOpenMobileSheet,
  onCloseMobileSheet,
  aiEstimate,
  onConfirmEstimate,
  storyId,
  chapterId,
  onOpenHistoryResult,
}: AiAssistantPanelProps) {
  const { t } = useTranslation("workspace");
  const { balance, isLoading: isLoadingCredits } = useCredits();
  const [activeView, setActiveView] = useState<"create" | "history">("create");
  const [historyPage, setHistoryPage] = useState(0);
  const [historyReload, setHistoryReload] = useState(0);
  const [copiedHistoryId, setCopiedHistoryId] = useState<number | null>(null);
  const [historyState, setHistoryState] = useState<{
    key: string;
    data: AiHistoryPage | null;
    error: string;
  }>({ key: "", data: null, error: "" });

  const historyKey = `${storyId}:${chapterId ?? "none"}:${historyPage}:${historyReload}`;
  const historyLoadErrorMessage = t("ai.history.error");

  useEffect(() => {
    if (activeView !== "history" || !chapterId) {
      return;
    }

    const controller = new AbortController();
    getChapterAiHistory(storyId, chapterId, historyPage, 10, controller.signal)
      .then((data) => setHistoryState({ key: historyKey, data, error: "" }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setHistoryState({
            key: historyKey,
            data: null,
            error: error instanceof Error && error.message
              ? error.message
              : historyLoadErrorMessage,
          });
        }
      });

    return () => controller.abort();
  }, [activeView, chapterId, historyKey, historyLoadErrorMessage, historyPage, storyId]);

  const currentHistory = historyState.key === historyKey ? historyState.data : null;
  const historyError = historyState.key === historyKey ? historyState.error : "";

  function historyItemToResult(item: AiHistoryItem): AiGenerateResult | null {
    if (item.outputType === "PROSE" && item.content) {
      return { format: "prose", content: item.content };
    }

    const options = item.structuredResult?.options;
    if (item.action === "REVIEW_CHAPTER" && item.structuredResult) {
      return parseReviewResult(item.structuredResult);
    }
    if (Array.isArray(options) && options.length === 3) {
      if (options.every((option) => typeof option === "string" && option.trim() !== "")) {
        return {
          format: "title-suggestions",
          options: options as [string, string, string],
        };
      }
      return {
        format: "suggestions",
        options: options as [SuggestContinuationOption, SuggestContinuationOption, SuggestContinuationOption],
      };
    }

    return null;
  }

  async function copyHistoryItem(item: AiHistoryItem) {
    const result = historyItemToResult(item);
    const text = result?.format === "prose"
      ? result.content
      : result?.format === "review"
        ? formatChapterReviewAsText(result)
      : result?.format === "title-suggestions"
        ? result.options.join("\n")
        : result?.options.map((option) => `${option.title}\n${option.snippet}`).join("\n\n") ?? "";
    if (!text) return;
    await navigator.clipboard.writeText(text);
    setCopiedHistoryId(item.id);
    window.setTimeout(() => setCopiedHistoryId(null), 1800);
  }

  const renderCreditsStatus = () => {
    if (isLoadingCredits) return "Carregando créditos...";
    if (isLoading) return "Atualizando saldo...";
    if (!balance) return "Créditos indisponíveis";
    if (balance.availableCredits <= 0) return "Você precisa de mais créditos para esta ação.";
    
    return (
      <>
        {formatCreditAmount(balance.availableCredits)} disponíveis
        {balance.reservedCredits > 0 && ` · ${formatCreditAmount(balance.reservedCredits)} reservados`}
      </>
    );
  };

  const panelContent = (
    <div className="panel-content">
      <div className="ai-panel-title">
        <h2>{t("ai.title")}</h2>
        <p>{t("ai.subtitle")}</p>
      </div>

      <div className="ai-panel-tabs" role="tablist" aria-label={t("ai.history.tabsLabel")}>
        <button
          type="button"
          role="tab"
          aria-selected={activeView === "create"}
          className={activeView === "create" ? "is-active" : ""}
          onClick={() => setActiveView("create")}
        >
          {t("ai.history.createTab")}
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeView === "history"}
          className={activeView === "history" ? "is-active" : ""}
          onClick={() => {
            setHistoryPage(0);
            setActiveView("history");
          }}
        >
          {t("ai.history.historyTab")}
        </button>
      </div>

      {activeView === "create" ? (
        <>

      <div className="ai-action-list">
        {actions.map((action) => (
          <button
            className="ai-action-card"
            type="button"
            key={action.actionType}
            onClick={() => {
                onInstructionChange(action.description);
                onGenerate(action.actionType);
            }}
            disabled={isLoading}
          >
            <strong>{action.title}</strong>
            <span>{action.description}</span>
          </button>
        ))}
      </div>

      <div className="ai-chat-card">
        <h3>{t("ai.instructionTitle")}</h3>

        <textarea
          value={instructionText}
          onChange={(event) => onInstructionChange(event.target.value)}
          placeholder={t("ai.instructionPlaceholder")}
        />

        <div style={{ fontSize: "0.75rem", marginBottom: "0.75rem", background: "var(--color-bg-surface-hover)", padding: "0.5rem", borderRadius: "4px", color: "var(--color-text-muted)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "4px" }}>
            <strong style={{ color: "var(--color-text-main)" }}>{renderCreditsStatus()}</strong>
            <Link to="/settings/credits" style={{ color: "var(--color-primary)", textDecoration: "none" }}>Ver consumo</Link>
          </div>
          {aiEstimate ? (
            <div style={{ marginTop: "4px", color: aiEstimate.canGenerate ? "var(--color-success)" : "var(--color-danger)" }}>
              {aiEstimate.exceedsPerRequestLimit
                ? <>Esta geração ultrapassa o limite seguro de <strong>{aiEstimate.maximumCreditsPerGeneration?.toFixed(2)} créditos</strong>. Reduza o tamanho ou o contexto.</>
                : <>Esta geração pode consumir até <strong>{aiEstimate.estimatedCredits.toFixed(2)} créditos</strong>.</>}
            </div>
          ) : (
            <div style={{ marginTop: "4px" }}>
              O custo varia conforme o tamanho do texto e o contexto da sua história. Antes de cada geração, você verá uma estimativa calculada para aquela ação.
            </div>
          )}
        </div>

        {!aiEstimate ? (
          <button
            type="button"
            onClick={() => onGenerate("CONTINUE_SCENE")}
            disabled={isLoading}
          >
            {isLoading ? t("ai.generating") : "Calcular estimativa"}
          </button>
        ) : (
          <button
            type="button"
            className="btn-primary"
            onClick={onConfirmEstimate}
            disabled={isLoading || !aiEstimate.canGenerate}
          >
            {isLoading ? t("ai.generating") : "Gerar"}
          </button>
        )}
      </div>

      <div className={`ai-result-preview ${!resultText ? "is-empty" : ""}`}>
        {errorMessage && <p className="ai-result-error">{errorMessage}</p>}

        {!errorMessage && !resultText && (
          <p className="ai-result-placeholder">{t("ai.resultPlaceholder")}</p>
        )}

        {resultText && (
          <div className="ai-result-text">
            <strong>{t("ai.suggestion.previewTitle")}</strong>
            <p>{resultText}</p>

            <button
              className="ai-result-open-button"
              type="button"
              onClick={onOpenSuggestion}
            >
              {t("ai.suggestion.viewSuggestion")}
            </button>
          </div>
        )}
      </div>
        </>
      ) : (
        <div className="ai-history-panel">
          {!chapterId && <p className="ai-history-state">{t("ai.history.noChapter")}</p>}
          {chapterId && !currentHistory && !historyError && (
            <p className="ai-history-state">{t("ai.history.loading")}</p>
          )}
          {historyError && (
            <div className="ai-history-state is-error">
              <p>{historyError}</p>
              <button type="button" onClick={() => setHistoryReload((value) => value + 1)}>
                {t("ai.history.retry")}
              </button>
            </div>
          )}
          {currentHistory?.content.length === 0 && (
            <p className="ai-history-state">{t("ai.history.empty")}</p>
          )}
          {currentHistory && currentHistory.content.length > 0 && (
            <>
              <div className="ai-history-list">
                {currentHistory.content.map((item) => {
                  const result = historyItemToResult(item);
                  const preview = result?.format === "prose"
                    ? result.content
                    : result?.format === "review"
                      ? result.summary
                    : result?.format === "title-suggestions"
                      ? result.options.join(" · ")
                      : result?.options.map((option) => option.title).join(" · ") ?? "";
                  return (
                    <article className="ai-history-card" key={item.id}>
                      <header>
                        <strong>{t(`ai.history.actions.${item.action}`, { defaultValue: item.action })}</strong>
                        <time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString()}</time>
                      </header>
                      <p>{preview}</p>
                      <div className="ai-history-actions">
                        <button type="button" disabled={!result} onClick={() => result && onOpenHistoryResult(result)}>
                          {t("ai.history.open")}
                        </button>
                        <button type="button" disabled={!result} onClick={() => void copyHistoryItem(item)}>
                          {copiedHistoryId === item.id ? t("ai.history.copied") : t("ai.history.copy")}
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
              <div className="ai-history-pagination">
                <button
                  type="button"
                  disabled={currentHistory.first}
                  onClick={() => setHistoryPage((page) => Math.max(0, page - 1))}
                >
                  {t("ai.history.previous")}
                </button>
                <span>{t("ai.history.page", { current: currentHistory.number + 1, total: currentHistory.totalPages })}</span>
                <button
                  type="button"
                  disabled={currentHistory.last}
                  onClick={() => setHistoryPage((page) => page + 1)}
                >
                  {t("ai.history.next")}
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );

  return (
    <>
      {/* Desktop Panel */}
      <aside className={`ai-tools-panel ${!isOpen ? "is-collapsed" : ""}`}>
        <button
          className="panel-collapse ai-collapse"
          type="button"
          onClick={onToggleOpen}
          title={t("ai.collapse")}
          aria-label={t("ai.collapse")}
        >
          {isOpen ? <IconChevronRightDouble /> : <IconChevronLeftDouble />}
        </button>

        {isOpen && panelContent}
      </aside>

      {/* Mobile Floating Trigger */}
      {onOpenMobileSheet && (
        <button
          type="button"
          className="mobile-ai-trigger"
          onClick={onOpenMobileSheet}
          aria-label="Abrir assistente"
          title="Abrir assistente"
        >
          <IconMessageSparkle />
        </button>
      )}

      {/* Mobile Bottom Sheet */}
      <div className={`mobile-ai-sheet ${isMobileSheetOpen ? "is-open" : ""}`} role="dialog" aria-modal="true">
        <div className="mobile-ai-sheet-header">
          <h2>{t("ai.title")}</h2>
          {onCloseMobileSheet && (
            <button
              type="button"
              className="mobile-ai-sheet-close"
              onClick={onCloseMobileSheet}
              aria-label="Fechar assistente"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M6 18L18 6M6 6l12 12" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          )}
        </div>
        {isMobileSheetOpen && panelContent}
      </div>
    </>
  );
}
