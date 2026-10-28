import { APP_CONFIG } from "../../../config/app";

export const commonPtBR = {
  app: {
    name: APP_CONFIG.name,
  },

  actions: {
    newStory: "Nova História",
    search: "Buscar",
    notifications: "Notificações",
    moreOptions: "Mais opções",
    collapseMenu: "Recolher menu",
    cancel: "Cancelar",
    createStory: "Criar história",
    creating: "Criando...",
  },

  states: {
    loadingStories: "Carregando histórias...",
    loadStoriesError: "Não foi possível carregar as histórias.",
  },
  notFound: {
  kicker: "Página não encontrada",
  title: "Essa página saiu do mapa.",
  description:
    "Ela pode não existir, ter sido removida ou não estar disponível para a sua conta.",
  backToLibrary: "Voltar para a biblioteca",
},
  topbar: {
    userMenu: {
      open: "Abrir menu do usuário",
      account: "Conta",
      settings: "Configurações da conta",
      logout: "Sair",
    },
  }
};