import { useEffect, useRef, useState } from "react";
import { getCreditTransactions } from "../../services/novelAiApi";
import type { CreditTransactionResponse } from "../../types/credits";
import {
  creditTransactionColor,
  formatCreditTransactionDate,
  presentCreditTransaction,
} from "../../utils/creditTransactions";
import "./TransactionsModal.css";

type TransactionsModalProps = {
  isOpen: boolean;
  onClose: () => void;
};

export function TransactionsModal({ isOpen, onClose }: TransactionsModalProps) {
  const [transactions, setTransactions] = useState<CreditTransactionResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    previousFocusRef.current = document.activeElement as HTMLElement | null;
    const focusTimer = window.setTimeout(() => closeButtonRef.current?.focus(), 0);

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      window.clearTimeout(focusTimer);
      document.removeEventListener("keydown", handleKeyDown);
      previousFocusRef.current?.focus();
    };
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen) return;

    let isCancelled = false;
    
    async function loadTransactions() {
      setIsLoading(true);
      setError("");
      try {
        const data = await getCreditTransactions(page, 15);
        if (isCancelled) return;
        setTransactions(data.content);
        setHasMore(page < data.totalPages - 1);
      } catch (err: unknown) {
        if (isCancelled) return;
        const detail = err instanceof Error ? err.message : "";
        setError("Não foi possível carregar o extrato. " + detail);
      } finally {
        if (!isCancelled) setIsLoading(false);
      }
    }

    loadTransactions();

    return () => {
      isCancelled = true;
    };
  }, [isOpen, page, reloadKey]);

  if (!isOpen) return null;

  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="modal transactions-modal" role="dialog" aria-modal="true" aria-labelledby="transactions-modal-title">
        <div className="modal-header">
          <h2 id="transactions-modal-title">Extrato de Créditos</h2>
          <button ref={closeButtonRef} type="button" className="modal-close" onClick={onClose} aria-label="Fechar extrato">
            ✕
          </button>
        </div>
        <div className="modal-content" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          {isLoading && <p role="status">Carregando...</p>}
          {error && (
            <div role="alert" className="transactions-error">
              <p>{error}</p>
              <button type="button" className="btn btn-secondary" onClick={() => setReloadKey((current) => current + 1)}>
                Tentar novamente
              </button>
            </div>
          )}
          
          {!isLoading && !error && transactions.length === 0 && (
            <p>Nenhuma transação encontrada.</p>
          )}

          {!isLoading && !error && transactions.length > 0 && (
            <div className="transactions-table-wrap" tabIndex={0} aria-label="Extrato com rolagem horizontal">
            <table className="transactions-table">
              <thead>
                <tr>
                  <th style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>Data</th>
                  <th style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>Descrição</th>
                  <th style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>Ação</th>
                  <th style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>Status</th>
                  <th style={{ padding: '8px', borderBottom: '1px solid var(--border-color)', textAlign: 'right' }}>Créditos</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx, idx) => {
                  const presentation = presentCreditTransaction(tx);
                  return (
                    <tr key={`${tx.date}-${tx.type}-${idx}`}>
                      <td style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>
                        {formatCreditTransactionDate(tx.date)}
                      </td>
                      <td style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>
                        {presentation.description}
                      </td>
                      <td style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>
                        {presentation.action}
                      </td>
                      <td style={{ padding: '8px', borderBottom: '1px solid var(--border-color)' }}>
                        {tx.status === 'COMPLETED' ? 'Concluído' : tx.status === 'RELEASED' ? 'Liberado' : tx.status === 'FAILED' ? 'Falhou' : tx.status}
                      </td>
                      <td style={{ padding: '8px', borderBottom: '1px solid var(--border-color)', textAlign: 'right' }}>
                        <strong style={{ color: creditTransactionColor(presentation.tone) }}>
                          {presentation.amount}
                        </strong>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            </div>
          )}
        </div>
        <div className="modal-footer transactions-modal-footer">
          <div>
            <button 
              type="button" 
              className="btn btn-secondary" 
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0 || isLoading}
              style={{ marginRight: '8px' }}
            >
              Anterior
            </button>
            <button 
              type="button" 
              className="btn btn-secondary" 
              onClick={() => setPage(p => p + 1)}
              disabled={!hasMore || isLoading}
            >
              Próxima
            </button>
          </div>
          <button type="button" className="btn btn-primary" onClick={onClose}>
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}
