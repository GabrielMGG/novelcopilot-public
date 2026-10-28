import { createContext } from "react";
import type { CreditBalanceResponse } from "../types/credits";

export type CreditContextValue = {
  balance: CreditBalanceResponse | null;
  isLoading: boolean;
  error: string | null;
  refreshBalance: () => Promise<void>;
};

export const CreditContext = createContext<CreditContextValue | undefined>(
  undefined,
);
