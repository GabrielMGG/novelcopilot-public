import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { BrowserRouter } from "react-router-dom";
import { Topbar } from "./Topbar";
import { Sidebar } from "./Sidebar";
import { CreditProvider } from "../../contexts/CreditProvider";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key })
}));

// Mock AuthProvider
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({
    user: { id: "1", email: "test@test.com", user_metadata: { full_name: "Test User" } },
    logout: vi.fn(),
  }),
}));

// Mock NovelAiApi so it doesn't really fetch
vi.mock("../../services/novelAiApi", () => ({
  getCreditBalance: vi.fn().mockResolvedValue({
    availableCredits: 100,
    reservedCredits: 0,
    currentQuotaProfile: "DEMO",
    cycleEnd: "2026-12-31"
  }),
  getCreditTransactions: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 })
}));

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // Deprecated
    removeListener: vi.fn(), // Deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

describe("Layout Integration", () => {
  it("renders the general layout without duplicating credits in the topbar", () => {
    render(
      <BrowserRouter>
        <CreditProvider>
          <div>
            <Topbar />
            <Sidebar isOpen={true} />
          </div>
        </CreditProvider>
      </BrowserRouter>
    );

    expect(screen.getByText("Configurações")).toBeInTheDocument();
    expect(screen.queryByText("Planos e Créditos")).not.toBeInTheDocument();
    
    expect(screen.queryByText("Créditos: 100")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Notificações" })).toBeInTheDocument();
  });
});
