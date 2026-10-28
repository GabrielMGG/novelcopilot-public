import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Settings } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";

type UserMenuProps = {
  variant?: "topbar" | "workspace";
};

export function UserMenu({ variant = "topbar" }: UserMenuProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const prefix = variant === "workspace" ? "ws-topbar" : "topbar";

  const metadata = user?.user_metadata;
  const displayName = (metadata?.full_name as string | undefined) ??
    (metadata?.name as string | undefined);
  const avatarLetter = (displayName?.[0] ?? user?.email?.[0] ?? "U").toUpperCase();
  const avatarUrl = (metadata?.avatar_url as string | undefined) ??
    (metadata?.picture as string | undefined) ?? null;

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) setIsOpen(false);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setIsOpen(false);
    }
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  function goTo(path: string) {
    setIsOpen(false);
    navigate(path);
  }

  return (
    <div className={`${prefix}__profile`} ref={menuRef}>
      <button
        type="button"
        className={`${prefix}__avatar-btn`}
        aria-label="Menu do usuário"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((current) => !current)}
      >
        {avatarUrl ? (
          <img src={avatarUrl} alt="" className={`${prefix}__avatar ${prefix}__avatar--img`} />
        ) : (
          <span className={`${prefix}__avatar`}>{avatarLetter}</span>
        )}
      </button>

      {isOpen && (
        <div className={`${prefix}__profile-menu`} role="menu">
          <div className={`${prefix}__profile-header`}>
            <strong>{displayName || "Conta"}</strong>
            <span>{user?.email}</span>
          </div>
          <button type="button" className={`${prefix}__profile-item`} onClick={() => goTo("/settings")}>
            <Settings width={16} height={16} /> Configurações
          </button>
          <button
            type="button"
            className={`${prefix}__profile-item ${prefix}__profile-item--danger`}
            onClick={() => { setIsOpen(false); logout(); }}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16" aria-hidden="true">
              <path d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            Sair
          </button>
        </div>
      )}
    </div>
  );
}
