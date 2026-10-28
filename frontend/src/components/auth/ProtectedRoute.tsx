import { Navigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import { LoadingScreen } from "../ui/LoadingScreen";
import { useLoadingPresentation } from "../../hooks/useLoadingPresentation";

type ProtectedRouteProps = {
  children: React.ReactNode;
};

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { user, isAuthLoading } = useAuth();
  const loadingPresentation = useLoadingPresentation(isAuthLoading);

  if (loadingPresentation.blockContent) {
    return loadingPresentation.showLoading ? <LoadingScreen /> : null;
  }

  if (!user) {
    return <Navigate to="/auth" replace />;
  }

  return children;
}
