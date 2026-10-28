/**
 * @vitest-environment jsdom
 */
import { render, fireEvent, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, vi } from 'vitest';
import { ChapterRichEditor } from './ChapterRichEditor';
import { QUICK_AI_ACTION_TYPES } from '../../utils/aiEstimateFlow';

Element.prototype.getClientRects = () => [] as unknown as DOMRectList;
Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
Range.prototype.getBoundingClientRect = () => new DOMRect();

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key })
}));

describe('ChapterRichEditor Undo Real', () => {
  it('mantém somente as três ações rápidas úteis na seleção', () => {
    expect(QUICK_AI_ACTION_TYPES).toEqual([
      'REWRITE_SELECTION',
      'IMPROVE_DIALOGUE',
      'ADD_TENSION',
    ]);
    expect(QUICK_AI_ACTION_TYPES).not.toContain('SUMMARIZE_SELECTION');
  });

  it('preserva a formatação do trecho substituído pela IA', async () => {
    const onContentChange = vi.fn();
    const commonProps = {
      chapterTitle: "Cap 1",
      chapterTitlePlaceholder: "Title",
      onChapterTitleChange: vi.fn(),
      onReplacementApplied: vi.fn(),
      onContentChange,
      onQuickAiAction: vi.fn(),
    };
    const { container, rerender } = render(
      <ChapterRichEditor
        content={'<p style="text-align: center"><span style="font-family: Lora, serif; font-size: 20px"><strong>Texto original.</strong></span></p>'}
        replacement={null}
        {...commonProps}
      />
    );

    rerender(
      <ChapterRichEditor
        content={'<p style="text-align: center"><span style="font-family: Lora, serif; font-size: 20px"><strong>Texto original.</strong></span></p>'}
        replacement={{
          text: '<p>Sugestão aplicada.</p>',
          range: { from: 1, to: 16 },
        }}
        {...commonProps}
      />
    );

    await waitFor(() => {
      expect(container.querySelector('.ProseMirror')).toHaveTextContent(
        'Sugestão aplicada.'
      );
    });

    const paragraph = container.querySelector('.ProseMirror p');
    const styledText = container.querySelector('.ProseMirror span');
    expect(paragraph).toHaveStyle({ textAlign: 'center' });
    expect(styledText).toHaveStyle({
      fontFamily: 'Lora, serif',
      fontSize: '20px',
    });
    expect(styledText?.querySelector('strong')).toHaveTextContent(
      'Sugestão aplicada.'
    );
    expect(container.querySelector('.chapter-editor-stats')).toHaveTextContent(
      '2 editor.stats.words'
    );
    expect(container.querySelector('.chapter-editor-stats')).toHaveTextContent(
      '18 editor.stats.characters'
    );
  });

  it('preserva o alinhamento quando a IA substitui todo o documento', async () => {
    const commonProps = {
      chapterTitle: "Cap 1",
      chapterTitlePlaceholder: "Title",
      onChapterTitleChange: vi.fn(),
      onReplacementApplied: vi.fn(),
      onContentChange: vi.fn(),
      onQuickAiAction: vi.fn(),
    };
    const content = '<p style="text-align: center"><span style="font-family: Lora, serif; font-size: 20px"><strong>Texto original.</strong></span></p>';
    const { container, rerender } = render(
      <ChapterRichEditor content={content} replacement={null} {...commonProps} />
    );

    rerender(
      <ChapterRichEditor
        content={content}
        replacement={{
          text: '<p>Sugestão aplicada.</p>',
          range: { from: 0, to: 17 },
        }}
        {...commonProps}
      />
    );

    await waitFor(() => {
      expect(container.querySelector('.ProseMirror')).toHaveTextContent(
        'Sugestão aplicada.'
      );
    });

    const paragraph = container.querySelector('.ProseMirror p');
    const styledText = container.querySelector('.ProseMirror span');
    expect(paragraph).toHaveStyle({ textAlign: 'center' });
    expect(styledText).toHaveStyle({
      fontFamily: 'Lora, serif',
      fontSize: '20px',
    });
    expect(styledText?.querySelector('strong')).toHaveTextContent(
      'Sugestão aplicada.'
    );
  });

  it('encontra e substitui texto no segundo parágrafo usando posições reais', async () => {
    const { container } = render(
      <ChapterRichEditor
        content="<p>Primeiro alvo.</p><p><strong>Segundo alvo.</strong></p>"
        replacement={null}
        chapterTitle="Cap 1"
        chapterTitlePlaceholder="Title"
        onChapterTitleChange={vi.fn()}
        onReplacementApplied={vi.fn()}
        onContentChange={vi.fn()}
        onQuickAiAction={vi.fn()}
      />
    );

    fireEvent.click(container.querySelector('.rich-editor-search-button')!);
    const fields = container.querySelectorAll<HTMLInputElement>(
      '.search-replace-field input'
    );
    fireEvent.change(fields[0], { target: { value: 'Segundo' } });
    fireEvent.change(fields[1], { target: { value: 'Novo' } });

    const actions = Array.from(
      container.querySelectorAll<HTMLButtonElement>('.search-replace-actions button')
    );
    fireEvent.click(actions[0]);
    fireEvent.click(actions[1]);

    await waitFor(() => {
      expect(container.querySelector('.ProseMirror')).toHaveTextContent(
        'Primeiro alvo.Novo alvo.'
      );
    });
    expect(container.querySelectorAll('.ProseMirror p')[1]).toContainHTML(
      '<strong>Novo alvo.</strong>'
    );
  });

  it('substitui todas as ocorrências sem alterar HTML ou formatação', async () => {
    const { container } = render(
      <ChapterRichEditor
        content={'<p><span style="font-family: Lora, serif">Lora encontrou LORA.</span></p>'}
        replacement={null}
        chapterTitle="Cap 1"
        chapterTitlePlaceholder="Title"
        onChapterTitleChange={vi.fn()}
        onReplacementApplied={vi.fn()}
        onContentChange={vi.fn()}
        onQuickAiAction={vi.fn()}
      />
    );

    fireEvent.click(container.querySelector('.rich-editor-search-button')!);
    const fields = container.querySelectorAll<HTMLInputElement>(
      '.search-replace-field input'
    );
    fireEvent.change(fields[0], { target: { value: 'lora' } });
    fireEvent.change(fields[1], { target: { value: 'Mara' } });

    const actions = container.querySelectorAll<HTMLButtonElement>(
      '.search-replace-actions button'
    );
    fireEvent.click(actions[2]);

    await waitFor(() => {
      expect(container.querySelector('.ProseMirror')).toHaveTextContent(
        'Mara encontrou Mara.'
      );
    });
    expect(container.querySelector('.ProseMirror span')).toHaveStyle({
      fontFamily: 'Lora, serif',
    });
  });

  it('insertContentAt gera uma única etapa de histórico e undo funciona', async () => {
    const onContentChange = vi.fn();
    const { container } = render(
      <ChapterRichEditor
        content="<p>Texto original.</p>"
        replacement={null}
        chapterTitle="Cap 1"
        chapterTitlePlaceholder="Title"
        isGeneratingSummary={false}
        isCreatingNextChapter={false}
        isGeneratingChapterFromBeats={false}
        inlineAiSuggestion=""
        inlineAiRange={null}
        isInlineAiLoading={false}
        isInlineAiAutoEnabled={false}
        onRequestInlineContinuation={vi.fn()}
        onInlineSuggestionAccepted={vi.fn()}
        onInlineSuggestionDismissed={vi.fn()}
        onOpenChapterBeatsModal={vi.fn()}
        onGenerateChapterSummary={vi.fn()}
        onCreateNextChapter={vi.fn()}
        onChapterTitleChange={vi.fn()}
        onReplacementApplied={vi.fn()}
        onContentChange={onContentChange}
        onQuickAiAction={vi.fn()}
      />
    );

    const editorElement = container.querySelector('.ProseMirror');
    expect(editorElement).toBeInTheDocument();

    // To test undo, we need access to the tiptap editor instance.
    // Since we don't have direct access, we can pass a replacement prop which internally calls `editor.chain().focus().insertContentAt(replacement.range, replacement.text).run()`
    
    // First, let's trigger a replacement
    const replacement = {
      text: "<p>Sugestão aplicada.</p>",
      range: { from: 1, to: 17 } // replace entire text
    };

    render(
      <ChapterRichEditor
        content="<p>Texto original.</p>"
        replacement={replacement}
        chapterTitle="Cap 1"
        chapterTitlePlaceholder="Title"
        isGeneratingSummary={false}
        isCreatingNextChapter={false}
        isGeneratingChapterFromBeats={false}
        inlineAiSuggestion=""
        inlineAiRange={null}
        isInlineAiLoading={false}
        isInlineAiAutoEnabled={false}
        onRequestInlineContinuation={vi.fn()}
        onInlineSuggestionAccepted={vi.fn()}
        onInlineSuggestionDismissed={vi.fn()}
        onOpenChapterBeatsModal={vi.fn()}
        onGenerateChapterSummary={vi.fn()}
        onCreateNextChapter={vi.fn()}
        onChapterTitleChange={vi.fn()}
        onReplacementApplied={vi.fn()}
        onContentChange={onContentChange}
        onQuickAiAction={vi.fn()}
      />,
      { container }
    );

    // Wait for effect to apply replacement
    await new Promise(r => setTimeout(r, 50));
    
    expect(editorElement?.textContent).toBe("Sugestão aplicada.");

    // Now trigger undo (Ctrl+Z)
    fireEvent.keyDown(editorElement!, { key: 'z', ctrlKey: true });
    
    // Check if restored
    expect(editorElement?.textContent).toBe("Texto original.");

    // Trigger redo (Ctrl+Y or Ctrl+Shift+Z)
    fireEvent.keyDown(editorElement!, { key: 'y', ctrlKey: true });
    
    expect(editorElement?.textContent).toBe("Sugestão aplicada.");
  });

  it('não informa edição quando recebe conteúdo carregado pelo sistema', async () => {
    const onContentChange = vi.fn();
    const commonProps = {
      replacement: null,
      chapterTitle: "Cap 1",
      chapterTitlePlaceholder: "Title",
      isGeneratingSummary: false,
      isCreatingNextChapter: false,
      isGeneratingChapterFromBeats: false,
      inlineAiSuggestion: "",
      inlineAiRange: null,
      isInlineAiLoading: false,
      isInlineAiAutoEnabled: false,
      onRequestInlineContinuation: vi.fn(),
      onInlineSuggestionAccepted: vi.fn(),
      onInlineSuggestionDismissed: vi.fn(),
      onOpenChapterBeatsModal: vi.fn(),
      onGenerateChapterSummary: vi.fn(),
      onCreateNextChapter: vi.fn(),
      onChapterTitleChange: vi.fn(),
      onReplacementApplied: vi.fn(),
      onContentChange,
      onQuickAiAction: vi.fn(),
    };
    const { container, rerender } = render(
      <ChapterRichEditor content="" {...commonProps} />
    );

    rerender(
      <ChapterRichEditor
        content="<p>Conteúdo carregado do servidor.</p>"
        {...commonProps}
      />
    );

    await waitFor(() => {
      expect(container.querySelector('.ProseMirror')).toHaveTextContent(
        "Conteúdo carregado do servidor."
      );
    });
    expect(onContentChange).not.toHaveBeenCalled();
  });

  it('updates visible word and character counts when chapter content changes', async () => {
    const props = {
      replacement: null,
      chapterTitle: "Cap 1",
      chapterTitlePlaceholder: "Title",
      onChapterTitleChange: vi.fn(),
      onReplacementApplied: vi.fn(),
      onContentChange: vi.fn(),
      onQuickAiAction: vi.fn(),
    };
    const { rerender } = render(
      <ChapterRichEditor content="<p>Olá mundo.</p>" {...props} />
    );

    expect(await screen.findByRole('status')).toHaveTextContent('2 editor.stats.words');
    expect(screen.getByRole('status')).toHaveTextContent('10 editor.stats.characters');

    rerender(
      <ChapterRichEditor content="<p>Um texto novo agora.</p>" {...props} />
    );

    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent('4 editor.stats.words');
      expect(screen.getByRole('status')).toHaveTextContent('20 editor.stats.characters');
    });
  });
});
