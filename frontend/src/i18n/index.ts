import i18n from "i18next";
import { initReactI18next } from "react-i18next";

import { commonPtBR } from "./locales/pt-BR/common";
import { navigationPtBR } from "./locales/pt-BR/navigation";
import { libraryPtBR } from "./locales/pt-BR/library";
import { workspacePtBR } from "./locales/pt-BR/workspace";
import { charactersPtBR } from "./locales/pt-BR/characters";
import { worldPtBR } from "./locales/pt-BR/world";



import { commonEn } from "./locales/en/common";
import { navigationEn } from "./locales/en/navigation";
import { libraryEn } from "./locales/en/library";
import { workspaceEn } from "./locales/en/workspace";
import { charactersEn } from "./locales/en/characters";
import { worldEn } from "./locales/en/world";
import { authPtBR } from "./locales/pt-BR/auth";
import { authEn } from "./locales/en/auth";

const resources = {
  "pt-BR": {
    common: commonPtBR,
    navigation: navigationPtBR,
    library: libraryPtBR,
    workspace: workspacePtBR,
    characters: charactersPtBR,
    world: worldPtBR,
    auth: authPtBR,
  },
  en: {
    common: commonEn,
    navigation: navigationEn,
    library: libraryEn,
    workspace: workspaceEn,
    characters: charactersEn,
    world: worldEn,
    auth: authEn,
  },
};

i18n.use(initReactI18next).init({
  resources,
  lng: "pt-BR",
  fallbackLng: "pt-BR",

  defaultNS: "common",

  interpolation: {
    escapeValue: false,
  },
});



export default i18n;