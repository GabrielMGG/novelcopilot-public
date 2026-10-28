import type { CreditTransactionResponse } from "../types/credits";

export type CreditTransactionTone = "positive" | "negative" | "neutral";

export type CreditTransactionPresentation = {
  action: string;
  description: string;
  amount: string;
  tone: CreditTransactionTone;
};

export function formatCreditAmount(credits: number) {
  return new Intl.NumberFormat("pt-BR", {
    maximumFractionDigits: 4,
  }).format(Math.abs(credits));
}

export function presentCreditTransaction(
  transaction: CreditTransactionResponse
): CreditTransactionPresentation {
  const credits = formatCreditAmount(transaction.credits);

  switch (transaction.type) {
    case "GRANT":
      return {
        action: "Créditos recebidos",
        description: transaction.reason || "Créditos adicionados ao saldo",
        amount: `+${credits}`,
        tone: "positive",
      };
    case "RESERVE":
      return {
        action: "Reserva temporária",
        description:
          "Estimativa protegida durante a geração; não é o custo definitivo",
        amount: `${credits} temporários`,
        tone: "neutral",
      };
    case "CAPTURE":
      return {
        action: "Créditos utilizados",
        description: "Custo real da geração concluída",
        amount: `-${credits}`,
        tone: "negative",
      };
    case "RELEASE":
      return {
        action: "Reserva liberada",
        description: "Créditos reservados devolvidos ao saldo",
        amount: `${credits} devolvidos`,
        tone: "positive",
      };
    case "REFUND":
      return {
        action: "Estorno",
        description: transaction.reason || "Créditos devolvidos ao saldo",
        amount: `+${credits}`,
        tone: "positive",
      };
    case "EXPIRATION":
      return {
        action: "Créditos expirados",
        description: transaction.reason || "Créditos removidos ao fim do prazo",
        amount: `-${credits}`,
        tone: "negative",
      };
    case "ADJUSTMENT":
      return {
        action: "Ajuste de créditos",
        description: transaction.reason || "Ajuste administrativo do saldo",
        amount: transaction.credits >= 0 ? `+${credits}` : `-${credits}`,
        tone: transaction.credits >= 0 ? "positive" : "negative",
      };
    default:
      return {
        action: transaction.type,
        description: transaction.reason || "-",
        amount: credits,
        tone: "neutral",
      };
  }
}

export function formatCreditTransactionDate(date: string | null) {
  if (!date) {
    return "Data indisponível";
  }

  const parsedDate = new Date(date);
  if (Number.isNaN(parsedDate.getTime())) {
    return "Data indisponível";
  }

  return parsedDate.toLocaleString("pt-BR");
}

export function creditTransactionColor(tone: CreditTransactionTone) {
  switch (tone) {
    case "positive":
      return "var(--color-success)";
    case "negative":
      return "var(--color-danger)";
    default:
      return "var(--color-text-muted)";
  }
}
