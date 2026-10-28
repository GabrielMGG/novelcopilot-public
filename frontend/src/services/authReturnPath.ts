const AUTH_RETURN_PATH_KEY = "novelcopilot:auth-return-path";

function isSafeInternalPath(path: string) {
  return path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/auth");
}

export function rememberCurrentProtectedPath() {
  if (typeof window === "undefined") return;

  const path = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  if (isSafeInternalPath(path)) {
    window.sessionStorage.setItem(AUTH_RETURN_PATH_KEY, path);
  }
}

export function consumeAuthReturnPath() {
  if (typeof window === "undefined") return "/";

  const path = window.sessionStorage.getItem(AUTH_RETURN_PATH_KEY);
  window.sessionStorage.removeItem(AUTH_RETURN_PATH_KEY);
  return path && isSafeInternalPath(path) ? path : "/";
}
