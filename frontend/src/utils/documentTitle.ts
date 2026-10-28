import { APP_CONFIG } from "../config/app";

export function setDocumentTitle(pageTitle?: string) {
  document.title = pageTitle
    ? `${pageTitle} | ${APP_CONFIG.name}`
    : APP_CONFIG.defaultDocumentTitle;
}
