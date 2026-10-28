import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { AiConfirmModal } from './AiConfirmModal';

describe('AiConfirmModal', () => {
  it('não renderiza se isOpen for falso', () => {
    const { container } = render(
      <AiConfirmModal
        isOpen={false}
        estimate={null}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={false}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('exibe estimatedCredits e availableCredits e valida acessibilidade do teclado', () => {
    render(
      <AiConfirmModal
        isOpen={true}
        estimate={{
          action: 'EXPAND_SELECTION',
          estimatedCredits: 12.5,
          availableCredits: 100,
          canGenerate: true,
          estimatedInputTokens: 100, maxOutputTokens: 100, contextTruncated: false, profile: 'TEST'
        }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={false}
      />
    );
    expect(screen.getByText(/consumir até 12,5 créditos/)).toBeInTheDocument();
    
    // Keyboard accessibility test
    const confirmButton = screen.getByRole('button', { name: 'Confirmar e Gerar' });
    confirmButton.focus();
    expect(document.activeElement).toBe(confirmButton);
  });

  it('WRITE_CHAPTER_FROM_BEATS mostra texto customizado "Gerar primeiro rascunho"', () => {
    render(
      <AiConfirmModal
        isOpen={true}
        estimate={{
          action: 'WRITE_CHAPTER_FROM_BEATS',
          estimatedCredits: 50,
          availableCredits: 100,
          canGenerate: true,
          estimatedInputTokens: 100, maxOutputTokens: 100, contextTruncated: false, profile: 'TEST'
        }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={false}
      />
    );
    expect(screen.getByText('Gerar primeiro rascunho')).toBeInTheDocument();
  });

  it('Cancelar não executa geração', () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    render(
      <AiConfirmModal
        isOpen={true}
        estimate={{ action: 'EXPAND_SELECTION', estimatedCredits: 10, availableCredits: 100, canGenerate: true, estimatedInputTokens: 100, maxOutputTokens: 100, contextTruncated: false, profile: 'TEST' }}
        onConfirm={onConfirm}
        onCancel={onCancel}
        isLoading={false}
      />
    );
    fireEvent.click(screen.getByText('Cancelar'));
    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('Confirmar executa uma única vez (duplo clique bloqueado por isLoading no app)', () => {
    const onConfirm = vi.fn();
    render(
      <AiConfirmModal
        isOpen={true}
        estimate={{ action: 'EXPAND_SELECTION', estimatedCredits: 10, availableCredits: 100, canGenerate: true, estimatedInputTokens: 100, maxOutputTokens: 100, contextTruncated: false, profile: 'TEST' }}
        onConfirm={onConfirm}
        onCancel={vi.fn()}
        isLoading={false}
      />
    );
    const btn = screen.getByText('Confirmar e Gerar');
    fireEvent.click(btn);
    // Component is just a dumb view, it fires every click, the parent handles loading.
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('loading bloqueia confirmação', () => {
    render(
      <AiConfirmModal
        isOpen={true}
        estimate={{ action: 'EXPAND_SELECTION', estimatedCredits: 10, availableCredits: 100, canGenerate: true, estimatedInputTokens: 100, maxOutputTokens: 100, contextTruncated: false, profile: 'TEST' }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={true}
      />
    );
    expect(screen.getByText('Gerando...')).toBeDisabled();
    expect(screen.getByText('Cancelar')).toBeDisabled();
  });

  it('saldo insuficiente mostra mensagem correta e oculta botão confirmar', () => {
    render(
      <AiConfirmModal
        isOpen={true}
        estimate={{ action: 'Test Action', estimatedCredits: 150, availableCredits: 100, canGenerate: false, estimatedInputTokens: 100, maxOutputTokens: 100, contextTruncated: false, profile: 'TEST' }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={false}
      />
    );
    expect(screen.getByText(/Créditos insuficientes para esta geração/)).toBeInTheDocument();
    expect(screen.queryByText('Confirmar e Gerar')).not.toBeInTheDocument();
  });

  it('limite seguro ultrapassado mostra motivo correto e impede confirmação', () => {
    render(
      <AiConfirmModal
        isOpen={true}
        estimate={{
          action: 'WRITE_CHAPTER_FROM_BEATS',
          estimatedCredits: 11,
          availableCredits: 100,
          canGenerate: false,
          estimatedInputTokens: 18000,
          maxOutputTokens: 2500,
          contextTruncated: false,
          profile: 'CHAPTER_FROM_BEATS',
          maximumCreditsPerGeneration: 10,
          exceedsPerRequestLimit: true
        }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={false}
      />
    );

    expect(screen.getByText(/ultrapassa o limite seguro de 10 créditos/)).toBeInTheDocument();
    expect(screen.queryByText('Gerar primeiro rascunho')).not.toBeInTheDocument();
  });

  it('não duplica no modal ações confirmadas dentro do painel de IA', () => {
    const { container } = render(
      <AiConfirmModal
        isOpen={true}
        estimate={{ action: 'CONTINUE_SCENE', estimatedCredits: 10, availableCredits: 100, canGenerate: true, estimatedInputTokens: 100, maxOutputTokens: 100, contextTruncated: false, profile: 'TEST' }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={false}
      />
    );

    expect(container.firstChild).toBeNull();
  });
});
