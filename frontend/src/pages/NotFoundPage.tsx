import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

export function NotFoundPage() {
  const { t } = useTranslation("common");

  return (
    <main className="not-found-page">
      <div className="not-found-card">
        <p className="not-found-kicker">{t("notFound.kicker")}</p>
        <h1>{t("notFound.title")}</h1>
        <p>{t("notFound.description")}</p>

        <Link className="btn-primary" to="/">
          {t("notFound.backToLibrary")}
        </Link>
      </div>
    </main>
  );
}
