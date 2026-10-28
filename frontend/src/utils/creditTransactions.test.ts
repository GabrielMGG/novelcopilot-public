import { describe, expect, it } from "vitest";
import type { CreditTransactionResponse } from "../types/credits";
import {
  formatCreditAmount,
  formatCreditTransactionDate,
  presentCreditTransaction,
} from "./creditTransactions";

function transaction(
  type: string,
  credits: number
): CreditTransactionResponse {
  return {
    date: "2026-07-30T14:09:30",
    type,
    status: "COMPLETED",
    credits,
    reason: null,
  };
}

describe("credit transaction presentation", () => {
  it("preserves fractional credit balances", () => {
    expect(formatCreditAmount(80.9)).toBe("80,9");
    expect(formatCreditAmount(0.1)).toBe("0,1");
  });

  it("shows only captured credits as the real generation cost", () => {
    expect(presentCreditTransaction(transaction("CAPTURE", 2))).toMatchObject({
      action: "Créditos utilizados",
      description: "Custo real da geração concluída",
      amount: "-2",
      tone: "negative",
    });
  });

  it("shows a reservation as temporary instead of a second charge", () => {
    expect(presentCreditTransaction(transaction("RESERVE", 3))).toMatchObject({
      action: "Reserva temporária",
      amount: "3 temporários",
      tone: "neutral",
    });
  });

  it("shows grants as positive balance entries", () => {
    expect(presentCreditTransaction(transaction("GRANT", 100))).toMatchObject({
      action: "Créditos recebidos",
      amount: "+100",
      tone: "positive",
    });
  });

  it("does not convert a missing date into the Unix epoch", () => {
    expect(formatCreditTransactionDate(null)).toBe("Data indisponível");
    expect(formatCreditTransactionDate("invalid")).toBe("Data indisponível");
  });

  it("formats a valid transaction date", () => {
    const formatted = formatCreditTransactionDate("2026-07-30T14:09:30");

    expect(formatted).toContain("2026");
    expect(formatted).not.toContain("1969");
  });
});
