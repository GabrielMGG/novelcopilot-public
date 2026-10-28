import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { IconPlus, IconSearch, IconSparkle } from "../icons/Icons";
import { useAuth } from "../../hooks/useAuth";
import { NotificationsMenu } from "./NotificationsMenu";
import { UserMenu } from "./UserMenu";
import "./Topbar.css";

type TopbarProps = {
  onCreateStory?: () => void;
  onOpenMenu?: () => void;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
};

export function Topbar({ onCreateStory, onOpenMenu, searchValue, onSearchChange }: TopbarProps) {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const hasSearch = onSearchChange !== undefined;

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsMobileMenuOpen(false);
        setIsSearchOpen(false);
      }
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

  useEffect(() => {
    document.body.style.overflow = isMobileMenuOpen ? "hidden" : "";
    return () => { document.body.style.overflow = ""; };
  }, [isMobileMenuOpen]);

  useEffect(() => {
    if (isSearchOpen) searchInputRef.current?.focus();
  }, [isSearchOpen]);

  function openMobileMenu() {
    if (onOpenMenu) onOpenMenu();
    else setIsMobileMenuOpen(true);
  }

  return (
    <>
      <header className="topbar">
        <div className="topbar-left">
          <button type="button" className="topbar__hamburger" aria-label="Abrir menu" onClick={openMobileMenu}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20" aria-hidden="true">
              <path d="M3 6h18M3 12h18M3 18h18" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
          <span className="brand-mark brand-mark--desktop" aria-hidden="true">W<IconSparkle /></span>
        </div>

        <div className="topbar-center">
          <Link className="topbar-brand-link" to="/"><span className="brand-name">NovelCopilot</span></Link>
          <svg className="brand-flourish brand-flourish--desktop" viewBox="0 0 96 14" fill="none" aria-hidden="true">
            <path d="M2 7c10-6 20 6 30 0s20-6 30 0 20 6 30 0" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
          </svg>
        </div>

        <div className="topbar-right">
          {hasSearch && isSearchOpen && (
            <label className="topbar__search">
              <span className="sr-only">Pesquisar histórias</span>
              <IconSearch />
              <input
                ref={searchInputRef}
                type="search"
                value={searchValue ?? ""}
                placeholder="Pesquisar histórias..."
                onChange={(event) => onSearchChange(event.target.value)}
              />
            </label>
          )}
          {onCreateStory && (
            <button className="btn-primary btn-primary--desktop" type="button" onClick={onCreateStory}>
              <IconPlus /> Nova história
            </button>
          )}
          {hasSearch && !isSearchOpen && (
            <button className="icon-btn topbar__search-trigger" aria-label="Pesquisar histórias" type="button" onClick={() => setIsSearchOpen(true)}>
              <IconSearch />
            </button>
          )}
          <NotificationsMenu />
          <UserMenu />
        </div>
      </header>

      {!onOpenMenu && isMobileMenuOpen && <div className="topbar-drawer-backdrop" aria-hidden="true" onClick={() => setIsMobileMenuOpen(false)} />}
      {!onOpenMenu && (
        <div className={`topbar-drawer${isMobileMenuOpen ? " is-open" : ""}`} role="dialog" aria-modal="true" aria-label="Menu de navegação">
          <div className="topbar-drawer__header">
            <span className="topbar-drawer__brand">NovelCopilot</span>
            <button type="button" className="topbar-drawer__close" aria-label="Fechar menu" onClick={() => setIsMobileMenuOpen(false)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20" aria-hidden="true"><path d="M6 18L18 6M6 6l12 12" strokeLinecap="round" strokeLinejoin="round" /></svg>
            </button>
          </div>
          <nav className="topbar-drawer__nav">
            <button type="button" className="topbar-drawer__item" onClick={() => { navigate("/"); setIsMobileMenuOpen(false); }}>Biblioteca</button>
            <button type="button" className="topbar-drawer__item" onClick={() => { navigate("/trash"); setIsMobileMenuOpen(false); }}>Lixeira</button>
          </nav>
          <div className="topbar-drawer__divider" />
          <nav className="topbar-drawer__footer">
            <button type="button" className="topbar-drawer__item topbar-drawer__item--danger" onClick={() => { setIsMobileMenuOpen(false); logout(); }}>Sair</button>
          </nav>
        </div>
      )}
    </>
  );
}
