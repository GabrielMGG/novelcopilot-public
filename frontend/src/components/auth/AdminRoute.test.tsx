import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../../hooks/useAuth";
import { authenticatedFetch } from "../../services/authenticatedFetch";
import { AdminRoute } from "./AdminRoute";

vi.mock("../../hooks/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../services/authenticatedFetch", () => ({
  authenticatedFetch: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedAuthenticatedFetch = vi.mocked(authenticatedFetch);

function renderRoute() {
  render(
    <MemoryRouter initialEntries={["/internal/benchmark"]}>
      <Routes>
        <Route
          path="/internal/benchmark"
          element={
            <AdminRoute>
              <div>benchmark protegido</div>
            </AdminRoute>
          }
        />
        <Route path="/" element={<div>início</div>} />
        <Route path="/auth" element={<div>autenticação</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("AdminRoute", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("redireciona visitante sem sessão para autenticação", async () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      isAuthLoading: false,
    } as ReturnType<typeof useAuth>);

    renderRoute();

    expect(await screen.findByText("autenticação")).toBeInTheDocument();
    expect(mockedAuthenticatedFetch).not.toHaveBeenCalled();
  });

  it("bloqueia usuário que o backend não reconhece como administrador", async () => {
    mockedUseAuth.mockReturnValue({
      user: { id: "ordinary-user" },
      isAuthLoading: false,
    } as ReturnType<typeof useAuth>);
    mockedAuthenticatedFetch.mockResolvedValue({ ok: false, status: 403 } as Response);

    renderRoute();

    expect(await screen.findByText("início")).toBeInTheDocument();
    expect(screen.queryByText("benchmark protegido")).not.toBeInTheDocument();
  });

  it("renderiza o benchmark somente após confirmação do backend", async () => {
    mockedUseAuth.mockReturnValue({
      user: { id: "admin-user" },
      isAuthLoading: false,
    } as ReturnType<typeof useAuth>);
    mockedAuthenticatedFetch.mockResolvedValue({ ok: true, status: 200 } as Response);

    renderRoute();

    expect(await screen.findByText("benchmark protegido")).toBeInTheDocument();
    expect(mockedAuthenticatedFetch).toHaveBeenCalledWith("/api/admin/access");
  });
});
