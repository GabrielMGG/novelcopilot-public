import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "../components/auth/ProtectedRoute";
import { AdminRoute } from "../components/auth/AdminRoute";
import { DelayedLoadingScreen } from "../components/ui/DelayedLoadingScreen";

const LibraryPage = lazy(() => import("../features/library/pages/LibraryPage").then((module) => ({ default: module.LibraryPage })));
const TrashPage = lazy(() => import("../pages/TrashPage").then((module) => ({ default: module.TrashPage })));
const StoryWorkspacePage = lazy(() => import("../pages/StoryWorkspacePage").then((module) => ({ default: module.StoryWorkspacePage })));
const AuthPage = lazy(() => import("../pages/AuthPage").then((module) => ({ default: module.AuthPage })));
const BenchmarkPage = lazy(() => import("../features/internal-benchmark/BenchmarkPage"));
const NotFoundPage = lazy(() => import("../pages/NotFoundPage").then((module) => ({ default: module.NotFoundPage })));
const SettingsLayout = lazy(() => import("../pages/settings/SettingsLayout").then((module) => ({ default: module.SettingsLayout })));
const ProfileSettingsPage = lazy(() => import("../pages/settings/ProfileSettingsPage").then((module) => ({ default: module.ProfileSettingsPage })));
const CreditsSettingsPage = lazy(() => import("../pages/settings/CreditsSettingsPage").then((module) => ({ default: module.CreditsSettingsPage })));
const PreferencesSettingsPage = lazy(() => import("../pages/settings/PreferencesSettingsPage").then((module) => ({ default: module.PreferencesSettingsPage })));

export function AppRoutes() {
  return (
    <Suspense fallback={<DelayedLoadingScreen title="Abrindo esta página" description="Carregando somente o que você precisa." />}>
      <Routes>
      {/* Rotas Públicas */}
      <Route path="/auth" element={<AuthPage />} />

      {/* Rotas Protegidas - Biblioteca */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <LibraryPage />
          </ProtectedRoute>
        }
      />

      {/* Rotas Protegidas - Lixeira */}
      <Route
        path="/trash"
        element={
          <ProtectedRoute>
            <TrashPage />
          </ProtectedRoute>
        }
      />

      {/* Rotas Protegidas - Workspace */}
      <Route
        path="/stories/:storyId/write"
        element={
          <ProtectedRoute>
            <StoryWorkspacePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stories/:storyId/characters"
        element={
          <ProtectedRoute>
            <StoryWorkspacePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stories/:storyId/world"
        element={
          <ProtectedRoute>
            <StoryWorkspacePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stories/:storyId/notes"
        element={
          <ProtectedRoute>
            <StoryWorkspacePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stories/:storyId/references"
        element={
          <ProtectedRoute>
            <StoryWorkspacePage />
          </ProtectedRoute>
        }
      />

      <Route
          path="/internal/benchmark"
          element={
            <AdminRoute>
              <BenchmarkPage />
            </AdminRoute>
          }
        />
      <Route
          path="/internal/benchmark/runs/:runId"
          element={
            <AdminRoute>
              <BenchmarkPage />
            </AdminRoute>
          }
        />

      {/* Rotas Protegidas - Configurações */}
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <SettingsLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<ProfileSettingsPage />} />
        <Route path="profile" element={<ProfileSettingsPage />} />
        <Route path="credits" element={<CreditsSettingsPage />} />
        <Route path="preferences" element={<PreferencesSettingsPage />} />
      </Route>

      {/* Rotas de Sistema */}
      <Route path="/not-found" element={<NotFoundPage />} />
      <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
