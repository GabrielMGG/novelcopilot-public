import { APP_CONFIG } from "../../../config/app";

export const commonEn = {
  app: {
    name: APP_CONFIG.name,
  },

  actions: {
    newStory: "New Story",
    search: "Search",
    notifications: "Notifications",
    moreOptions: "More options",
    collapseMenu: "Collapse menu",
    cancel: "Cancel",
    createStory: "Create story",
    creating: "Creating...",
  },

  states: {
    loadingStories: "Loading stories...",
    loadStoriesError: "Could not load stories.",
  },
  notFound: {
    kicker: "Page not found",
    title: "This page slipped off the map.",
    description:
      "It may not exist, have been removed, or not be available to your account.",
    backToLibrary: "Back to library",
  },
  topbar: {
    userMenu: {
      open: "Open user menu",
      account: "Account",
      settings: "Account settings",
      logout: "Sign out",
    },
  }
};