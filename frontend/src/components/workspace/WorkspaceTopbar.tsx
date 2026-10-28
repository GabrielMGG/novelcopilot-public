import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { UserMenu } from "../layout/UserMenu";
import { useAuth } from "../../hooks/useAuth";
import { useCredits } from "../../hooks/useCredits";
import { formatCreditAmount } from "../../utils/creditTransactions";
import "./WorkspaceTopbar.css";

type WorkspaceTab = "writing" | "characters" | "world" | "notes" | "references";
type WorkspaceTopbarProps = { storyId: string | number; activeTab: WorkspaceTab; isFocusMode?: boolean };

const TABS: { key: WorkspaceTab; label: string; path: string }[] = [
  { key: "writing", label: "Escrita", path: "write" },
  { key: "characters", label: "Personagens", path: "characters" },
  { key: "world", label: "Mundo", path: "world" },
  { key: "notes", label: "Notas", path: "notes" },
  { key: "references", label: "Referências", path: "references" },
];

export function WorkspaceTopbar({ storyId, activeTab, isFocusMode = false }: WorkspaceTopbarProps) {
  const { logout } = useAuth();
  const { balance, isLoading } = useCredits();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setIsMobileMenuOpen(false);
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

  useEffect(() => {
    document.body.style.overflow = isMobileMenuOpen ? "hidden" : "";
    return () => { document.body.style.overflow = ""; };
  }, [isMobileMenuOpen]);

  if (isFocusMode) return null;

  function handleNavigate(path: string) {
    setIsMobileMenuOpen(false);
    navigate(path);
  }

  return (
    <>
      <header className="ws-topbar">
        <div className="ws-topbar__side ws-topbar__side--left">
          <button type="button" className="ws-topbar__hamburger" aria-label="Abrir menu" onClick={() => setIsMobileMenuOpen(true)}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20" aria-hidden="true"><path d="M3 6h18M3 12h18M3 18h18" strokeLinecap="round" strokeLinejoin="round" /></svg>
          </button>
          <Link className="ws-topbar__brand ws-topbar__brand--desktop" to="/">NovelCopilot</Link>
        </div>
        <div className="ws-topbar__center">
          <Link className="ws-topbar__brand ws-topbar__brand--mobile" to="/">NovelCopilot</Link>
          <nav className="ws-topbar__tabs" aria-label="Áreas da história">
            {TABS.map((tab) => (
              <Link key={tab.key} to={`/stories/${storyId}/${tab.path}`} className={`ws-topbar__tab${activeTab === tab.key ? " is-active" : ""}`}>
                {tab.label}
              </Link>
            ))}
          </nav>
        </div>
        <div className="ws-topbar__side ws-topbar__side--right">
          {balance && (
            <div className="ws-topbar__credits">
              {isLoading ? <span className="ws-topbar__credits-value">Atualizando saldo...</span> : (
                <>
                  <span className="ws-topbar__credits-value">Créditos: {formatCreditAmount(balance.availableCredits)}</span>
                  {balance.reservedCredits > 0 && <span className="ws-topbar__credits-reserved">({formatCreditAmount(balance.reservedCredits)} reservados)</span>}
                </>
              )}
            </div>
          )}
          <UserMenu variant="workspace" />
        </div>
      </header>

      {isMobileMenuOpen && <div className="ws-drawer-backdrop" aria-hidden="true" onClick={() => setIsMobileMenuOpen(false)} />}
      <div className={`ws-drawer${isMobileMenuOpen ? " is-open" : ""}`} role="dialog" aria-modal="true" aria-label="Menu de navegação">
        <div className="ws-drawer__header">
          <span className="ws-drawer__brand">NovelCopilot</span>
          <button type="button" className="ws-drawer__close" aria-label="Fechar menu" onClick={() => setIsMobileMenuOpen(false)}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20" aria-hidden="true"><path d="M6 18L18 6M6 6l12 12" strokeLinecap="round" strokeLinejoin="round" /></svg>
          </button>
        </div>
        <nav className="ws-drawer__nav" aria-label="Navegação principal">
          {TABS.map((tab) => (
            <button key={tab.key} type="button" className={`ws-drawer__nav-item${activeTab === tab.key ? " is-active" : ""}`} onClick={() => handleNavigate(`/stories/${storyId}/${tab.path}`)}>{tab.label}</button>
          ))}
        </nav>
        <div className="ws-drawer__divider" />
        <nav className="ws-drawer__footer-nav" aria-label="Navegação secundária">
          <button type="button" className="ws-drawer__nav-item" onClick={() => handleNavigate("/")}>Biblioteca</button>
          <button type="button" className="ws-drawer__nav-item" onClick={() => handleNavigate("/trash")}>Lixeira</button>
          <button type="button" className="ws-drawer__nav-item ws-drawer__nav-item--danger" onClick={() => { setIsMobileMenuOpen(false); logout(); }}>Sair</button>
        </nav>
      </div>
    </>
  );
}
