import { supabase } from "../lib/supabaseClient";
import { API_BASE_URL } from "../config/api";
import { rememberCurrentProtectedPath } from "./authReturnPath";

let refreshPromise: Promise<string | null> | null = null;
let signOutPromise: Promise<void> | null = null;

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = supabase.auth.refreshSession()
      .then(({ data, error }) => error ? null : data.session?.access_token ?? null)
      .catch(() => null)
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

async function invalidateExpiredSession() {
  rememberCurrentProtectedPath();

  if (!signOutPromise) {
    signOutPromise = supabase.auth.signOut({ scope: "local" })
      .then(() => undefined)
      .catch(() => undefined)
      .finally(() => {
        signOutPromise = null;
      });
  }

  await signOutPromise;
}

export async function authenticatedFetch(
  input: RequestInfo | URL,
  init: RequestInit = {}
) {
  const {
    data: { session },
  } = await supabase.auth.getSession();

  if (!session?.access_token) {
    throw new Error("Usuário não autenticado.");
  }

  let requestUrl: RequestInfo | URL = input;

  if (typeof input === "string" && input.startsWith("/")) {
    requestUrl = `${API_BASE_URL}${input}`;
  }

  function buildHeaders(accessToken: string) {
    const headers = new Headers(init.headers);
    headers.set("Authorization", `Bearer ${accessToken}`);

    if (init.body && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }

    return headers;
  }

  const firstResponse = await fetch(requestUrl, {
    ...init,
    headers: buildHeaders(session.access_token),
  });

  if (firstResponse.status !== 401) {
    return firstResponse;
  }

  const refreshedAccessToken = await refreshAccessToken();

  if (!refreshedAccessToken) {
    await invalidateExpiredSession();
    return firstResponse;
  }

  const retryResponse = await fetch(requestUrl, {
    ...init,
    headers: buildHeaders(refreshedAccessToken),
  });

  if (retryResponse.status === 401) {
    await invalidateExpiredSession();
  }

  return retryResponse;
}
