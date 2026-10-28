import { useCallback, useEffect, useState } from "react";
import { useCredits } from "../../hooks/useCredits";
import { getCreditTransactions } from "../../services/novelAiApi";
import type { CreditTransactionResponse } from "../../types/credits";
import {
  creditTransactionColor,
  formatCreditAmount,
  formatCreditTransactionDate,
  presentCreditTransaction,
} from "../../utils/creditTransactions";
import "./CreditsSettings.css";

const CREDIT_HISTORY_PAGE_SIZE = 10;

export function CreditsSettingsPage() {
  const { balance } = useCredits();
  const [history, setHistory] = useState<CreditTransactionResponse[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [historyError, setHistoryError] = useState("");
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);

  const loadHistory = useCallback(async () => {
    setLoadingHistory(true);
    setHistoryError("");
    try {
      const response = await getCreditTransactions(historyPage, CREDIT_HISTORY_PAGE_SIZE);
      setHistory(response.content || []);
      setHistoryTotalPages(response.totalPages || 0);
    } catch {
      setHistoryError("Não foi possível carregar o histórico de créditos.");
    } finally {
      setLoadingHistory(false);
    }
  }, [historyPage]);

  useEffect(() => {
    void Promise.resolve().then(loadHistory);
  }, [loadHistory]);

  const visibleHistory = history.filter(
    transaction => transaction.type !== "RESERVE" && transaction.type !== "RELEASE",
  );

  return (
    <div>
      <h2 className="settings-section-title">Créditos e Consumo</h2>
      <p className="settings-section-desc">Créditos são usados quando a IA gera uma resposta válida.</p>

      {/* Cards */}
      <div className="credits-card-grid">
        <div className="credits-card">
          <h4>Disponíveis</h4>
          <strong className="credits-value">{formatCreditAmount(balance?.availableCredits ?? 0)}</strong>
        </div>
        <div className="credits-card">
          <h4>Renovação</h4>
          <strong className="credits-value credits-value--date">
            {balance?.cycleEnd ? new Date(balance.cycleEnd).toLocaleDateString() : "N/A"}
          </strong>
        </div>
      </div>

      <h3 className="settings-subsection-title">Como o consumo é calculado</h3>
      <div className="credits-info-card credits-cost-explanation">
        <p>Antes de cada geração, o NovelCopilot calcula uma estimativa usando:</p>
        <ul className="credits-consumption-list">
          <li>o tamanho do texto e do contexto enviado;</li>
          <li>a ação solicitada e o modelo utilizado;</li>
          <li>o limite máximo de resposta daquela geração.</li>
        </ul>
        <p className="credits-cost-explanation__note">Você confirma a estimativa antes da chamada. O histórico abaixo mostra somente movimentações definitivas do seu saldo.</p>
      </div>

      <h3 className="settings-subsection-title">Histórico completo</h3>
      {loadingHistory ? (
        <p className="credits-state" role="status">Carregando extrato...</p>
      ) : historyError ? (
        <div className="credits-state" role="alert">
          <p>{historyError}</p>
          <button type="button" onClick={() => void loadHistory()}>Tentar novamente</button>
        </div>
      ) : visibleHistory.length === 0 ? (
        <p className="credits-empty-text">Você ainda não possui movimentações de créditos.</p>
      ) : (
        <div className="credits-table-wrap" tabIndex={0} aria-label="Histórico de créditos com rolagem horizontal">
          <table className="credits-table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Ação</th>
                <th>Descrição</th>
                <th>Créditos</th>
              </tr>
            </thead>
            <tbody>
              {visibleHistory.map((tx, index) => {
                const presentation = presentCreditTransaction(tx);
                return (
                  <tr key={`${tx.date}-${tx.type}-${index}`}>
                    <td data-label="Data">{formatCreditTransactionDate(tx.date)}</td>
                    <td data-label="Ação">{presentation.action}</td>
                    <td data-label="Descrição">{presentation.description}</td>
                    <td
                      data-label="Créditos"
                      style={{
                        fontWeight: "bold",
                        color: creditTransactionColor(presentation.tone),
                      }}
                    >
                      {presentation.amount}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {!loadingHistory && !historyError && historyTotalPages > 1 ? (
        <nav className="credits-pagination" aria-label="Páginas do histórico de créditos">
          <button
            type="button"
            onClick={() => setHistoryPage(current => Math.max(0, current - 1))}
            disabled={historyPage === 0}
          >
            Anterior
          </button>
          <span aria-live="polite">Página {historyPage + 1} de {historyTotalPages}</span>
          <button
            type="button"
            onClick={() => setHistoryPage(current => current + 1)}
            disabled={historyPage >= historyTotalPages - 1}
          >
            Próxima
          </button>
        </nav>
      ) : null}
    </div>
  );
}
