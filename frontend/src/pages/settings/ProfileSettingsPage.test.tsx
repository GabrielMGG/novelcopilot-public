// @vitest-environment jsdom

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { updateProfileName } from "../../services/authService";
import { ProfileSettingsPage } from "./ProfileSettingsPage";

const authState = vi.hoisted(() => ({
  user: {
    email: "writer@example.com",
    user_metadata: { full_name: "Nome Atual" },
    app_metadata: { provider: "email" },
  },
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => authState,
}));

vi.mock("../../services/authService", () => ({
  updateProfileName: vi.fn(),
}));

describe("ProfileSettingsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.user = {
      email: "writer@example.com",
      user_metadata: { full_name: "Nome Atual" },
      app_metadata: { provider: "email" },
    };
  });

  it("shows the current account and keeps email read-only", () => {
    render(<ProfileSettingsPage />);
    expect(screen.getByDisplayValue("Nome Atual")).toBeInTheDocument();
    expect(screen.getByDisplayValue("writer@example.com")).toHaveAttribute("readonly");
    expect(screen.getByText("Acesso com E-mail e senha")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Salvar alterações" })).toBeDisabled();
  });

  it("validates the name before calling Supabase", () => {
    render(<ProfileSettingsPage />);
    fireEvent.change(screen.getByLabelText("Nome"), { target: { value: "A" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
    expect(screen.getByRole("alert")).toHaveTextContent("pelo menos 2 caracteres");
    expect(updateProfileName).not.toHaveBeenCalled();
  });

  it("updates a valid name and confirms success", async () => {
    vi.mocked(updateProfileName).mockResolvedValue({} as never);
    render(<ProfileSettingsPage />);
    fireEvent.change(screen.getByLabelText("Nome"), { target: { value: "  Novo Nome  " } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
    await waitFor(() => expect(updateProfileName).toHaveBeenCalledWith("Novo Nome"));
    expect(await screen.findByRole("status")).toHaveTextContent("Nome atualizado com sucesso");
  });

  it("shows a safe error when Supabase rejects the update", async () => {
    vi.mocked(updateProfileName).mockRejectedValue(new Error("offline"));
    render(<ProfileSettingsPage />);
    fireEvent.change(screen.getByLabelText("Nome"), { target: { value: "Outro Nome" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível atualizar seu nome");
  });
});
