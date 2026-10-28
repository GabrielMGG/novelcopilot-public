import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import { authenticatedFetch } from "../../services/authenticatedFetch";
import { LoadingScreen } from "../ui/LoadingScreen";
import { useLoadingPresentation } from "../../hooks/useLoadingPresentation";

type AdminRouteProps = {
  children: React.ReactNode;
};

type AccessState = "checking" | "allowed" | "denied";

export function AdminRoute({ children }: AdminRouteProps) {
  const { user, isAuthLoading } = useAuth();
  const [access, setAccess] = useState<{
    userId: string | null;
    state: AccessState;
  }>({ userId: null, state: "checking" });
  const accessState = user && access.userId === user.id ? access.state : "checking";
  const loadingPresentation = useLoadingPresentation(
    isAuthLoading || Boolean(user && accessState === "checking"),
  );

  useEffect(() => {
    if (!user) {
      return;
    }

    let active = true;
    const userId = user.id;

    authenticatedFetch("/api/admin/access")
      .then((response) => {
        if (active) {
          setAccess({
            userId,
            state: response.ok ? "allowed" : "denied",
          });
        }
      })
      .catch(() => {
        if (active) {
          setAccess({ userId, state: "denied" });
        }
      });

    return () => {
      active = false;
    };
  }, [user]);

  if (loadingPresentation.blockContent) {
    return loadingPresentation.showLoading ? <LoadingScreen /> : null;
  }

  if (!user) {
    return <Navigate to="/auth" replace />;
  }

  if (accessState === "denied") {
    return <Navigate to="/" replace />;
  }

  return children;
}
