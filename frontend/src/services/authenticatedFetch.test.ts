import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { getSessionMock, refreshSessionMock, signOutMock } = vi.hoisted(() => ({
  getSessionMock: vi.fn(),
  refreshSessionMock: vi.fn(),
  signOutMock: vi.fn(),
}));

vi.mock("../lib/supabaseClient", () => ({
  supabase: {
    auth: {
      getSession: getSessionMock,
      refreshSession: refreshSessionMock,
      signOut: signOutMock,
    },
  },
}));

import { authenticatedFetch } from "./authenticatedFetch";

describe("authenticatedFetch", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
    getSessionMock.mockResolvedValue({
      data: { session: { access_token: "expired-token" } },
    });
    refreshSessionMock.mockReset();
    signOutMock.mockReset();
    signOutMock.mockResolvedValue({ error: null });
    fetchMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("envia a primeira requisicao com o token da sessao atual", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    const response = await authenticatedFetch("/chapters/50", {
      method: "DELETE",
    });

    expect(response.status).toBe(204);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer expired-token");
    expect(refreshSessionMock).not.toHaveBeenCalled();
  });

  it("renova a sessao e repete uma exclusao que recebeu 401", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    refreshSessionMock.mockResolvedValue({
      data: { session: { access_token: "renewed-token" } },
      error: null,
    });

    const response = await authenticatedFetch("/chapters/50", {
      method: "DELETE",
    });

    expect(response.status).toBe(204);
    expect(refreshSessionMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const retryHeaders = fetchMock.mock.calls[1][1].headers as Headers;
    expect(retryHeaders.get("Authorization")).toBe("Bearer renewed-token");
  });

  it("nao entra em loop quando a repeticao tambem recebe 401", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));
    refreshSessionMock.mockResolvedValue({
      data: { session: { access_token: "renewed-token" } },
      error: null,
    });

    const response = await authenticatedFetch("/chapters/50", {
      method: "DELETE",
    });

    expect(response.status).toBe(401);
    expect(refreshSessionMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(signOutMock).toHaveBeenCalledWith({ scope: "local" });
  });

  it("devolve o 401 original quando a renovacao falha", async () => {
    const unauthorizedResponse = new Response(null, { status: 401 });
    fetchMock.mockResolvedValue(unauthorizedResponse);
    refreshSessionMock.mockResolvedValue({
      data: { session: null },
      error: new Error("refresh failed"),
    });

    const response = await authenticatedFetch("/chapters/50", {
      method: "DELETE",
    });

    expect(response).toBe(unauthorizedResponse);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(signOutMock).toHaveBeenCalledWith({ scope: "local" });
  });

  it("nao renova a sessao para outros codigos de erro", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 403 }));

    const response = await authenticatedFetch("/chapters/50", {
      method: "DELETE",
    });

    expect(response.status).toBe(403);
    expect(refreshSessionMock).not.toHaveBeenCalled();
    expect(signOutMock).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("recusa a requisicao quando nao existe sessao autenticada", async () => {
    getSessionMock.mockResolvedValue({ data: { session: null } });

    await expect(authenticatedFetch("/chapters/50", { method: "DELETE" }))
      .rejects.toThrow("Usuário não autenticado.");
    expect(fetchMock).not.toHaveBeenCalled();
    expect(refreshSessionMock).not.toHaveBeenCalled();
    expect(signOutMock).not.toHaveBeenCalled();
  });

  it("compartilha uma unica renovacao entre requisicoes simultaneas", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValue(new Response(null, { status: 200 }));
    refreshSessionMock.mockResolvedValue({
      data: { session: { access_token: "renewed-token" } },
      error: null,
    });

    const responses = await Promise.all([
      authenticatedFetch("/stories"),
      authenticatedFetch("/api/credits/balance"),
    ]);

    expect(responses.map((response) => response.status)).toEqual([200, 200]);
    expect(refreshSessionMock).toHaveBeenCalledTimes(1);
    expect(signOutMock).not.toHaveBeenCalled();
  });
});
