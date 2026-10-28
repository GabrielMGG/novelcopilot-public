import { fireEvent, render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { NotificationsMenu } from "./NotificationsMenu";
import { UserMenu } from "./UserMenu";

const menuAuthState = vi.hoisted(() => ({
  user: { email: "writer@example.com", user_metadata: { full_name: "Writer" } },
  logout: vi.fn(),
}));

vi.mock("../../hooks/useAuth", () => ({ useAuth: () => menuAuthState }));

describe("topbar menus", () => {
  beforeEach(() => {
    menuAuthState.user = { email: "writer@example.com", user_metadata: { full_name: "Writer" } };
  });

  it("shows a useful empty notifications state", () => {
    render(<NotificationsMenu />);
    fireEvent.click(screen.getByRole("button", { name: "Notificações" }));
    expect(screen.getByText("Nenhuma notificação no momento.")).toBeInTheDocument();
  });

  it("uses the compact standard user menu", () => {
    render(<BrowserRouter><UserMenu /></BrowserRouter>);
    fireEvent.click(screen.getByRole("button", { name: "Menu do usuário" }));
    expect(screen.getByText("Configurações")).toBeInTheDocument();
    expect(screen.getByText("Sair")).toBeInTheDocument();
    expect(screen.queryByText(/Créditos/i)).not.toBeInTheDocument();
    expect(screen.queryByText("Biblioteca")).not.toBeInTheDocument();
  });

  it("reflects a name received from an updated authentication session", () => {
    const { rerender } = render(<BrowserRouter><UserMenu /></BrowserRouter>);
    fireEvent.click(screen.getByRole("button", { name: "Menu do usuário" }));
    expect(screen.getByText("Writer")).toBeInTheDocument();

    menuAuthState.user = { email: "writer@example.com", user_metadata: { full_name: "Updated Writer" } };
    rerender(<BrowserRouter><UserMenu /></BrowserRouter>);

    expect(screen.getByText("Updated Writer")).toBeInTheDocument();
    expect(screen.queryByText("Writer")).not.toBeInTheDocument();
  });
});
