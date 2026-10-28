// @vitest-environment jsdom

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getCreditTransactions } from "../../services/novelAiApi";
import { TransactionsModal } from "./TransactionsModal";

vi.mock("../../services/novelAiApi", () => ({
  getCreditTransactions: vi.fn(),
}));

describe("TransactionsModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("recebe foco e fecha com Escape", async () => {
    vi.mocked(getCreditTransactions).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
    });
    const onClose = vi.fn();

    render(<TransactionsModal isOpen onClose={onClose} />);

    expect(screen.getByRole("dialog", { name: "Extrato de Créditos" })).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("button", { name: "Fechar extrato" })).toHaveFocus());

    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("mostra erro e permite tentar carregar novamente", async () => {
    vi.mocked(getCreditTransactions)
      .mockRejectedValueOnce(new Error("indisponível"))
      .mockResolvedValueOnce({ content: [], totalElements: 0, totalPages: 0 });

    render(<TransactionsModal isOpen onClose={vi.fn()} />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível carregar o extrato");
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));

    await waitFor(() => expect(getCreditTransactions).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("Nenhuma transação encontrada.")).toBeInTheDocument();
  });
});
