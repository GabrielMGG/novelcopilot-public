import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";
import {
  IconCollapse,
  IconFeather,
  IconGear,
  IconLibrary,
  IconTrash,
} from "../icons/Icons";

export function Sidebar({ isOpen, onClose }: { isOpen?: boolean; onClose?: () => void }) {
  const { t } = useTranslation(["common", "navigation", "library"]);
  const location = useLocation();

  const isLibraryActive = location.pathname === "/";
  const isTrashActive = location.pathname === "/trash";

  const navItems = [
    { label: t("navigation:library"), icon: <IconLibrary />, active: isLibraryActive, path: "/" },
    { label: "Configurações", icon: <IconGear />, active: location.pathname.startsWith("/settings"), path: "/settings/profile" },
  ];

  return (
    <>
      <div 
        className={`sidebar-backdrop ${isOpen ? 'is-open' : ''}`} 
        onClick={onClose} 
        aria-hidden="true"
      />
      <aside className={`sidebar ${isOpen ? 'is-open' : ''}`} aria-expanded={isOpen}>
      <nav className="nav">
        {navItems.map((item) => (
          <Link
            key={item.label}
            to={item.path}
            className={`nav-item${item.active ? " is-active" : ""}`}
            onClick={onClose}
          >
            <span className="nav-icon">{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        ))}
      </nav>

      <div className="sidebar-bottom">

        <Link
          to="/trash"
          className={`nav-item${isTrashActive ? " is-active" : ""}`}
          onClick={onClose}
        >
          <span className="nav-icon">
            <IconTrash />
          </span>
          <span>{t("navigation:trash")}</span>
        </Link>

        <div className="sidebar-quote">
          <IconFeather />
          <p>{t("library:quote")}</p>
        </div>

        <button
          className="collapse-btn"
          aria-label={t("actions.collapseMenu")}
          type="button"
        >
          <IconCollapse />
        </button>
      </div>
      </aside>
    </>
  );
}
