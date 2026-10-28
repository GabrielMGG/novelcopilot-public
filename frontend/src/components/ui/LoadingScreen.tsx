import featherLogo from "../../assets/novelcopilot-feather-512.png";
import { APP_CONFIG } from "../../config/app";
import "./LoadingScreen.css";

type LoadingScreenProps = {
  title?: string;
  description?: string;
};

export function LoadingScreen({
  title = "Preparando seu espaço de escrita",
  description = "Carregando suas histórias com segurança.",
}: LoadingScreenProps) {
  return (
    <main className="loading-screen">
      <div className="loading-card">
        <div className="loading-logo-shell">
          <img src={featherLogo} alt="" className="loading-logo" />
        </div>

        <div className="loading-text">
          <strong>{APP_CONFIG.name}</strong>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>

        <div className="loading-progress" aria-hidden="true">
          <span />
        </div>
      </div>
    </main>
  );
}
