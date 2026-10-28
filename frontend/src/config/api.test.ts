import { describe, expect, it } from "vitest";
import { resolveApiBaseUrl } from "./api";

describe("resolveApiBaseUrl", () => {
  const developmentApiUrl = "http://localhost:8080";

  it("uses the configured API and removes trailing slashes", () => {
    expect(resolveApiBaseUrl(
      "https://api.novelcopilot.test///",
      false,
      developmentApiUrl,
    ))
      .toBe("https://api.novelcopilot.test");
  });

  it("uses the local backend when developing without configuration", () => {
    expect(resolveApiBaseUrl(undefined, true, developmentApiUrl))
      .toBe("http://localhost:8080");
  });

  it("uses the same origin in production without configuration", () => {
    expect(resolveApiBaseUrl(undefined, false, developmentApiUrl)).toBe("");
  });

  it.each([
    "http://localhost:8080",
    "http://127.0.0.1:8080",
    "http://[::1]:8080",
  ])("never publishes a loopback backend in production: %s", (url) => {
    expect(resolveApiBaseUrl(url, false, developmentApiUrl)).toBe("");
  });
});
