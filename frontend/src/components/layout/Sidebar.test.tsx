import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { Sidebar } from "./Sidebar";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "../../contexts/AuthProvider";
import type { ReactNode } from "react";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key })
}));

// Mock AuthProvider so it doesn't throw or fetch
vi.mock("../../contexts/AuthProvider", () => {
  return {
    AuthProvider: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    useAuth: () => ({
      user: { id: "1", email: "test@test.com" },
      logout: vi.fn(),
    }),
  };
});

describe("Sidebar", () => {
  it("renders the public navigation without commercial links", () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <Sidebar isOpen={true} />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByText("Configurações")).toBeInTheDocument();
    expect(screen.queryByText("Planos e Créditos")).not.toBeInTheDocument();
    expect(screen.getByText("navigation:library")).toBeInTheDocument();
  });
});
