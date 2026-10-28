import type { AiEstimateResponse } from "../../types/credits";
import { formatCreditAmount } from "../../utils/creditTransactions";
import "./AiConfirmModal.css";

type AiConfirmModalProps = {
  isOpen: boolean;
  estimate: AiEstimateResponse | null;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading: boolean;
};

export function AiConfirmModal({
  isOpen,
  estimate,
  onConfirm,
  onCancel,
  isLoading
}: AiConfirmModalProps) {
  const panelActions = ["CONTINUE_SCENE", "REWRITE_SELECTION", "REVIEW_CHAPTER", "PLAN_SCENE"];
  
  if (!isOpen || !estimate || panelActions.includes(estimate.action)) return null;

  return (
    <div className="modal-backdrop">
      <div className="modal" role="dialog" aria-modal="true">
        <div className="modal-header">
          <h2>Confirmar Geração</h2>
          <button type="button" className="modal-close" onClick={onCancel}>
            ✕
          </button>
        </div>
        <div className="modal-content">
          <p>
            {estimate.exceedsPerRequestLimit
              ? `Esta geração ultrapassa o limite seguro de ${formatCreditAmount(estimate.maximumCreditsPerGeneration ?? 0)} créditos. Reduza o tamanho ou o contexto.`
              : estimate.canGenerate
              ? `Esta geração pode consumir até ${formatCreditAmount(estimate.estimatedCredits)} créditos.`
              : `Créditos insuficientes para esta geração. (Estimado: ${formatCreditAmount(estimate.estimatedCredits)}, Disponível: ${formatCreditAmount(estimate.availableCredits)})`
            }
          </p>
          {estimate.action === "WRITE_CHAPTER_FROM_BEATS" && estimate.canGenerate && (
            <p style={{ marginTop: "12px", fontSize: "0.9rem", color: "var(--text-muted)" }}>
              Deseja prosseguir e gerar o primeiro rascunho?
            </p>
          )}
        </div>
        <div className="modal-footer">
          <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={isLoading}>
            Cancelar
          </button>
          {estimate.canGenerate && (
            <button type="button" className="btn btn-primary" onClick={onConfirm} disabled={isLoading}>
              {isLoading 
                ? "Gerando..." 
                : estimate.action === "WRITE_CHAPTER_FROM_BEATS" 
                  ? "Gerar primeiro rascunho" 
                  : "Confirmar e Gerar"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
