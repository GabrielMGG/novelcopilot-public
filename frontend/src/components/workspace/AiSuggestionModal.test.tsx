/**
 * @vitest-environment jsdom
 */
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, vi } from 'vitest';
import { AiSuggestionModal } from './AiSuggestionModal';
import type { AiSuggestionsResult } from '../../types/ai';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key })
}));

describe('AiSuggestionModal', () => {
  it('renderiza prosa', () => {
    const result = { format: 'prose' as const, content: 'Texto em prosa.' };
    render(
      <AiSuggestionModal
        result={result}
        canReplaceSelection={true}
        onClose={vi.fn()}
        onCopy={vi.fn()}
        onInsertAtEnd={vi.fn()}
        onReplaceSelection={vi.fn()}
      />
    );
    expect(screen.getByText('Texto em prosa.')).toBeInTheDocument();
  });

  it('exibe três cartões para sugestões', () => {
    const result: AiSuggestionsResult = {
      format: 'suggestions' as const,
      options: [
        { type: 'safe' as const, title: 'T1', summary: 'S1', snippet: 'Snip1' },
        { type: 'unexpected' as const, title: 'T2', summary: 'S2', snippet: 'Snip2' },
        { type: 'intense' as const, title: 'T3', summary: 'S3', snippet: 'Snip3' }
      ]
    };
    render(
      <AiSuggestionModal
        result={result}
        canReplaceSelection={true}
        onClose={vi.fn()}
        onCopy={vi.fn()}
        onInsertAtEnd={vi.fn()}
        onReplaceSelection={vi.fn()}
      />
    );
    
    expect(screen.getByText('T1')).toBeInTheDocument();
    expect(screen.getByText('safe')).toBeInTheDocument();
    expect(screen.getByText('Snip1')).toBeInTheDocument();
    expect(screen.getByText('T2')).toBeInTheDocument();
    expect(screen.getByText('T3')).toBeInTheDocument();
  });

  it('usar trecho chama handler correto', () => {
    const onInsertAtEnd = vi.fn();
    const result: AiSuggestionsResult = {
      format: 'suggestions' as const,
      options: [
        { type: 'safe' as const, title: 'T1', summary: 'S1', snippet: 'Snip1' },
        { type: 'unexpected' as const, title: 'T2', summary: 'S2', snippet: 'Snip2' },
        { type: 'intense' as const, title: 'T3', summary: 'S3', snippet: 'Snip3' }
      ]
    };
    render(
      <AiSuggestionModal
        result={result}
        canReplaceSelection={true}
        onClose={vi.fn()}
        onCopy={vi.fn()}
        onInsertAtEnd={onInsertAtEnd}
        onReplaceSelection={vi.fn()}
      />
    );
    
    const btns = screen.getAllByText('Usar trecho (Inserir ao final)');
    fireEvent.click(btns[0]);
    expect(onInsertAtEnd).toHaveBeenCalledWith('Snip1');
  });
});
