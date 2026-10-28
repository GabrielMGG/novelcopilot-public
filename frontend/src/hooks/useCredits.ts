import { useContext } from "react";
import { CreditContext } from "../contexts/CreditContext";

export function useCredits() {
  const context = useContext(CreditContext);
  if (context === undefined) {
    throw new Error("useCredits must be used within a CreditProvider");
  }
  return context;
}
