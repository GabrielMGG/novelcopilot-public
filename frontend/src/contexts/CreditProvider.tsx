import { useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import { getCreditBalance } from '../services/novelAiApi';
import type { CreditBalanceResponse } from '../types/credits';
import { useAuth } from '../hooks/useAuth';
import { CreditContext } from './CreditContext';

export function CreditProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [balance, setBalance] = useState<CreditBalanceResponse | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(user));
  const [error, setError] = useState<string | null>(null);

  const refreshBalance = useCallback(async () => {
    if (!user) {
      setBalance(null);
      setIsLoading(false);
      setError(null);
      return;
    }
    
    setIsLoading(true);
    setError(null);
    try {
      const data = await getCreditBalance();
      setBalance(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Erro ao carregar saldo");
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  useEffect(() => {
    void Promise.resolve().then(refreshBalance);
  }, [refreshBalance]);

  return (
    <CreditContext.Provider value={{ balance, isLoading, error, refreshBalance }}>
      {children}
    </CreditContext.Provider>
  );
}
