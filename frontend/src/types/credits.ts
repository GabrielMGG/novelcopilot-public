export type CreditBalanceResponse = {
  availableCredits: number;
  reservedCredits: number;
  currentQuotaProfile: string | null;
  cycleStart: string | null;
  cycleEnd: string | null;
  updatedAt: string | null;
};

export type AiEstimateResponse = {
  action: string;
  estimatedCredits: number;
  estimatedInputTokens: number;
  maxOutputTokens: number;
  contextTruncated: boolean;
  profile: string;
  canGenerate: boolean;
  availableCredits: number;
  maximumCreditsPerGeneration?: number;
  exceedsPerRequestLimit?: boolean;
};

export type CreditTransactionResponse = {
  date: string | null;
  type: string;
  status: string;
  credits: number;
  reason: string | null;
};

export type CreditTransactionPage = {
  content: CreditTransactionResponse[];
  totalElements: number;
  totalPages: number;
};

