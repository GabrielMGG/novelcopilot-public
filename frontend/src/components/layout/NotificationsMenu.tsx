import { useEffect, useRef, useState } from "react";
import { IconBell } from "../icons/Icons";

export function NotificationsMenu() {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

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

  return (
    <div className="topbar__notifications" ref={menuRef}>
      <button
        className="icon-btn icon-btn--desktop"
        aria-label="Notificações"
        aria-expanded={isOpen}
        type="button"
        onClick={() => setIsOpen((current) => !current)}
      >
        <IconBell />
      </button>
      {isOpen && (
        <div className="topbar__notifications-menu" role="status">
          <strong>Notificações</strong>
          <p>Nenhuma notificação no momento.</p>
        </div>
      )}
    </div>
  );
}
