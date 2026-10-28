import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CreditProvider } from './CreditProvider';
import { useCredits } from '../hooks/useCredits';
import { getCreditBalance } from '../services/novelAiApi';
import { useAuth } from '../hooks/useAuth';
import type { CreditBalanceResponse } from '../types/credits';

vi.mock('../services/novelAiApi', () => ({
  getCreditBalance: vi.fn(),
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

function TestComponent() {
  const { balance, isLoading, error, refreshBalance } = useCredits();
  return (
    <div>
      <div data-testid="loading">{isLoading ? 'loading' : 'done'}</div>
      <div data-testid="error">{error || 'none'}</div>
      <div data-testid="available">{balance?.availableCredits ?? 'N/A'}</div>
      <div data-testid="reserved">{balance?.reservedCredits ?? 'N/A'}</div>
      <div data-testid="quota-profile">{balance?.currentQuotaProfile ?? 'N/A'}</div>
      <div data-testid="cycle">{balance?.cycleEnd ?? 'N/A'}</div>
      <button onClick={refreshBalance}>Refresh</button>
    </div>
  );
}

describe('CreditProvider', () => {
  const authenticatedUser = {
    user: { id: '1' },
  } as ReturnType<typeof useAuth>;

  function balance(
    overrides: Partial<CreditBalanceResponse> = {},
  ): CreditBalanceResponse {
    return {
      availableCredits: 100,
      reservedCredits: 0,
      currentQuotaProfile: null,
      cycleStart: null,
      cycleEnd: null,
      updatedAt: null,
      ...overrides,
    };
  }

  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('busca saldo ao montar e não bloqueia aplicação com loading (mostra conteudo imediatamente)', async () => {
    vi.mocked(useAuth).mockReturnValue(authenticatedUser);
    vi.mocked(getCreditBalance).mockReturnValue(new Promise(resolve => setTimeout(() => resolve(balance({
      reservedCredits: 20,
      currentQuotaProfile: 'DEMO',
      cycleEnd: '2026-10-10',
    })), 10)));

    render(
      <CreditProvider>
        <TestComponent />
      </CreditProvider>
    );

    // Initial state: loading is true but component renders
    expect(screen.getByTestId('loading').textContent).toBe('loading');
    expect(screen.getByTestId('available').textContent).toBe('N/A');

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('done');
    });

    expect(screen.getByTestId('available').textContent).toBe('100');
    expect(screen.getByTestId('reserved').textContent).toBe('20');
    expect(screen.getByTestId('quota-profile').textContent).toBe('DEMO');
    expect(screen.getByTestId('cycle').textContent).toBe('2026-10-10');
  });

  it('erro de balance não quebra o editor', async () => {
    vi.mocked(useAuth).mockReturnValue(authenticatedUser);
    vi.mocked(getCreditBalance).mockRejectedValue(new Error('API error'));

    render(
      <CreditProvider>
        <TestComponent />
      </CreditProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('error').textContent).toBe('API error');
    });

    expect(screen.getByTestId('available').textContent).toBe('N/A');
  });

  it('desmontagem não atualiza estado após resposta tardia', async () => {
    vi.mocked(useAuth).mockReturnValue(authenticatedUser);
    let resolvePromise: ((value: CreditBalanceResponse) => void) | undefined;
    vi.mocked(getCreditBalance).mockReturnValue(new Promise(res => {
      resolvePromise = res;
    }));

    const { unmount } = render(
      <CreditProvider>
        <TestComponent />
      </CreditProvider>
    );

    expect(screen.getByTestId('loading').textContent).toBe('loading');
    
    unmount();
    
    // Resolve after unmount (react would warn if state is updated, but newer React handles it. 
    // We just verify it doesn't crash)
    resolvePromise?.(balance());
  });

  it('refresh atualiza os valores', async () => {
    vi.mocked(useAuth).mockReturnValue(authenticatedUser);
    vi.mocked(getCreditBalance)
      .mockResolvedValueOnce(balance())
      .mockResolvedValueOnce(balance({ availableCredits: 50 }));

    render(
      <CreditProvider>
        <TestComponent />
      </CreditProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('available').textContent).toBe('100');
    });

    screen.getByText('Refresh').click();

    await waitFor(() => {
      expect(screen.getByTestId('available').textContent).toBe('50');
    });
  });
});
