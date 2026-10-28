function normalizeApiBaseUrl(configuredUrl?: string): string {
  return configuredUrl?.trim().replace(/\/+$/, "") ?? "";
}

function isLocalApiUrl(url: string): boolean {
  try {
    const hostname = new URL(url).hostname;
    return hostname === "localhost"
      || hostname === "127.0.0.1"
      || hostname === "::1"
      || hostname === "[::1]";
  } catch {
    return false;
  }
}

export function resolveApiBaseUrl(
  configuredUrl: string | undefined,
  isDevelopment: boolean,
  developmentApiUrl: string,
): string {
  const normalizedUrl = normalizeApiBaseUrl(configuredUrl);

  if (normalizedUrl && (isDevelopment || !isLocalApiUrl(normalizedUrl))) {
    return normalizedUrl;
  }

  return isDevelopment ? developmentApiUrl : "";
}

export const API_BASE_URL = resolveApiBaseUrl(
  import.meta.env.VITE_API_BASE_URL,
  import.meta.env.DEV,
  import.meta.env.DEV ? "http://localhost:8080" : "",
);
