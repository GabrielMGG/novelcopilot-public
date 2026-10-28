// @vitest-environment jsdom

import { beforeEach, describe, expect, it } from "vitest";
import { consumeAuthReturnPath, rememberCurrentProtectedPath } from "./authReturnPath";

describe("authReturnPath", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    window.history.replaceState(null, "", "/");
  });

  it("preserva a pagina protegida para o proximo login", () => {
    window.history.replaceState(null, "", "/stories/35/write?chapter=50#editor");

    rememberCurrentProtectedPath();

    expect(consumeAuthReturnPath()).toBe("/stories/35/write?chapter=50#editor");
    expect(consumeAuthReturnPath()).toBe("/");
  });

  it("nao preserva a propria pagina de autenticacao", () => {
    window.history.replaceState(null, "", "/auth?next=//example.com");

    rememberCurrentProtectedPath();

    expect(consumeAuthReturnPath()).toBe("/");
  });
});
