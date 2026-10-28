import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Topbar } from "../../components/layout/Topbar";
import { IconChevronLeftDouble, IconPeople, IconNote } from "../../components/icons/Icons";
import "./SettingsLayout.css";

export function SettingsLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const tabs = [
    { id: "profile", label: "Perfil", path: "/settings/profile", icon: <IconPeople /> },
    { id: "credits", label: "Créditos e Consumo", path: "/settings/credits", icon: <IconNote /> },
  ];

  return (
    <div className="settings-layout">
      <Topbar />
      <div className="settings-container">
        <aside className="settings-sidebar">
          <div className="settings-sidebar-heading">
            <span>Minha conta</span>
            <strong>Configurações</strong>
          </div>
          <div className="settings-back-link">
            <Link to="/" className="back-btn">
              <IconChevronLeftDouble /> Voltar para Biblioteca
            </Link>
          </div>
          <nav className="settings-nav">
            {tabs.map(tab => (
              <Link
                key={tab.id}
                to={tab.path}
                className={`settings-nav-item ${location.pathname.startsWith(tab.path) ? "is-active" : ""}`}
                aria-current={location.pathname.startsWith(tab.path) ? "page" : undefined}
              >
                <span className="settings-nav-icon">{tab.icon}</span>
                <span className="settings-nav-label">{tab.label}</span>
              </Link>
            ))}
          </nav>
        </aside>
        
        {/* Mobile menu select style */}
        <div className="settings-mobile-nav">
          <Link to="/" className="back-btn mobile-back">
            <IconChevronLeftDouble /> Voltar
          </Link>
          <select 
            value={tabs.find(t => location.pathname.startsWith(t.path))?.path || ""}
            onChange={(e) => navigate(e.target.value)}
            className="settings-mobile-select"
            aria-label="Seção das configurações"
          >
            {tabs.map(tab => (
              <option key={tab.id} value={tab.path}>{tab.label}</option>
            ))}
          </select>
        </div>

        <main className="settings-content">
          <div className="settings-content-inner">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
