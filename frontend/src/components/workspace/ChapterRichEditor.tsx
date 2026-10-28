import { useEffect, useRef, useState } from "react";
import { EditorContent, useEditor, useEditorState } from "@tiptap/react";
import { BubbleMenu } from "@tiptap/react/menus";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import TextAlign from "@tiptap/extension-text-align";
import "./ChapterRichEditor.css";
import { useTranslation } from "react-i18next";
import { TextStyle, FontFamily, FontSize } from "@tiptap/extension-text-style";
import { Extension, type Editor, type JSONContent } from "@tiptap/core";
import { Plugin, PluginKey } from "@tiptap/pm/state";
import { Decoration, DecorationSet } from "@tiptap/pm/view";
import { findEditorTextMatches } from "../../utils/editorSearch";
import { countEditorText } from "../../utils/editorTextStats";
import { QUICK_AI_ACTION_TYPES, type QuickAiAction } from "../../utils/aiEstimateFlow";
import { 
  IconUndo, 
  IconRedo, 
  IconAlignLeft, 
  IconAlignCenter, 
  IconAlignRight, 
  IconAlignJustify,
  IconSearch,
  IconSparkle
} from "../icons/Icons";



export type EditorSelectionRange = {
  from: number;
  to: number;
};

export type EditorReplacement = {
  text: string;
  range: EditorSelectionRange;
};

type ChapterRichEditorProps = {
  content: string;
  replacement: EditorReplacement | null;
  chapterTitle: string;
  chapterTitlePlaceholder: string;
  showToolbar?: boolean;
  isGeneratingSummary?: boolean;
  isCreatingNextChapter?: boolean;
  isGeneratingChapterFromBeats?: boolean;
  isGeneratingChapterTitle?: boolean;
  inlineAiSuggestion?: string;
  inlineAiRange?: EditorSelectionRange | null;
  isInlineAiLoading?: boolean;
  isInlineAiAutoEnabled?: boolean;
  onRequestInlineContinuation?: (payload: {
    currentText: string;
    cursorRange: EditorSelectionRange;
  }) => void;
  onInlineSuggestionAccepted?: () => void;
  onInlineSuggestionDismissed?: () => void;
  onOpenChapterBeatsModal?: () => void;
  onChapterTitleChange: (title: string) => void;
  onReplacementApplied: () => void;
  onContentChange: (content: string) => void;
  onGenerateChapterSummary?: () => void;
  onCreateNextChapter?: () => void;
  onGenerateChapterTitle?: () => void;
  onQuickAiAction: (
    actionType: QuickAiAction,
    selectedText: string,
    selectionRange: EditorSelectionRange
  ) => void;
};

type InlineAiGhostState = {
  text: string;
  pos: number | null;
  isLoading: boolean;
};

const inlineAiGhostPluginKey = new PluginKey<InlineAiGhostState>(
  "inlineAiGhost"
);

function extractReplacementParagraphs(content: string): string[] {
  if (!content.includes("<")) {
    return content.split(/\r?\n+/);
  }

  const container = document.createElement("div");
  container.innerHTML = content;
  const paragraphTexts = Array.from(container.querySelectorAll("p")).map(
    (paragraph) => paragraph.textContent ?? ""
  );

  return paragraphTexts.length > 0
    ? paragraphTexts
    : [container.textContent ?? ""];
}

function buildFormattedReplacement(
  editor: Editor,
  replacement: EditorReplacement
): JSONContent | JSONContent[] | string {
  const { from, to } = replacement.range;
  const paragraphs = extractReplacementParagraphs(replacement.text);
  let marks: NonNullable<JSONContent["marks"]> = [];
  let paragraphAttrs: Record<string, unknown> = {};

  editor.state.doc.nodesBetween(from, to, (node) => {
    if (Object.keys(paragraphAttrs).length === 0 && node.isTextblock) {
      paragraphAttrs = { ...node.attrs };
    }

    if (marks.length === 0 && node.isText) {
      marks = node.marks.map((mark) => mark.toJSON());
    }
  });

  if (marks.length === 0) {
    marks = editor.state.doc
      .resolve(from)
      .marks()
      .map((mark) => mark.toJSON());
  }

  const replacesWholeDocument = from === 0 && to === editor.state.doc.content.size;

  if (paragraphs.length === 1 && !replacesWholeDocument) {
    if (!paragraphs[0]) {
      return "";
    }

    return {
      type: "text",
      text: paragraphs[0],
      marks,
    };
  }

  return paragraphs.map((text) => ({
    type: "paragraph",
    attrs: paragraphAttrs,
    content: text
      ? [
          {
            type: "text",
            text,
            marks,
          },
        ]
      : undefined,
  }));
}

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    inlineAiGhost: {
      setInlineAiGhost: (
        text: string,
        pos: number,
        isLoading?: boolean
      ) => ReturnType;
      clearInlineAiGhost: () => ReturnType;
    };
  }
}

const InlineAiGhostExtension = Extension.create({
  name: "inlineAiGhost",

  addCommands() {
    return {
      setInlineAiGhost:
        (text: string, pos: number, isLoading = false) =>
          ({ tr, dispatch }) => {
            dispatch?.(
              tr.setMeta(inlineAiGhostPluginKey, {
                text,
                pos,
                isLoading,
              })
            );

            return true;
          },
      clearInlineAiGhost:
        () =>
          ({ tr, dispatch }) => {
            dispatch?.(
              tr.setMeta(inlineAiGhostPluginKey, {
                text: "",
                pos: null,
                isLoading: false,
              })
            );

            return true;
          },
    };
  },

  addProseMirrorPlugins() {
    return [
      new Plugin<InlineAiGhostState>({
        key: inlineAiGhostPluginKey,

        state: {
          init() {
            return {
              text: "",
              pos: null,
              isLoading: false,
            };
          },

          apply(transaction, value) {
            const meta = transaction.getMeta(inlineAiGhostPluginKey);

            if (meta) {
              return meta;
            }

            if (value.pos !== null && transaction.docChanged) {
              return {
                ...value,
                pos: transaction.mapping.map(value.pos),
              };
            }

            return value;
          },
        },

        props: {
          decorations(state) {
            const ghost = inlineAiGhostPluginKey.getState(state);

            if (!ghost?.text || ghost.pos === null) {
              return null;
            }

            const widget = Decoration.widget(
              ghost.pos,
              () => {
                const span = document.createElement("span");
                span.className = ghost.isLoading
                  ? "rich-editor-inline-ai-ghost is-loading"
                  : "rich-editor-inline-ai-ghost";
                span.textContent = ghost.text;
                return span;
              },
              {
                side: 1,
              }
            );

            return DecorationSet.create(state.doc, [widget]);
          },
        },
      }),
    ];
  },
});

export function ChapterRichEditor({
  content,
  replacement,
  chapterTitle,
  chapterTitlePlaceholder,
  showToolbar = true,
  isGeneratingSummary = false,
  isCreatingNextChapter = false,
  isGeneratingChapterFromBeats = false,
  isGeneratingChapterTitle = false,
  inlineAiSuggestion = "",
  inlineAiRange = null,
  isInlineAiLoading = false,
  isInlineAiAutoEnabled = false,
  onRequestInlineContinuation,
  onInlineSuggestionAccepted,
  onInlineSuggestionDismissed,
  onOpenChapterBeatsModal,
  onCreateNextChapter,
  onGenerateChapterTitle,
  onChapterTitleChange,
  onReplacementApplied,
  onContentChange,
  onGenerateChapterSummary,
  onQuickAiAction,
}: ChapterRichEditorProps) {
  const { t } = useTranslation("workspace");

  const quickAiActions = QUICK_AI_ACTION_TYPES.map((actionType) => ({
    actionType,
    label: t(
      actionType === "REWRITE_SELECTION"
        ? "selectionActions.rewrite"
        : actionType === "IMPROVE_DIALOGUE"
          ? "selectionActions.dialogue"
          : "selectionActions.tension"
    ),
    icon: actionType === "REWRITE_SELECTION" ? "✨" : undefined,
  }));

  const [isSearchReplaceOpen, setIsSearchReplaceOpen] = useState(false);
  const [searchText, setSearchText] = useState("");
  const [replaceText, setReplaceText] = useState("");
  const [searchMessage, setSearchMessage] = useState("");
  const inlineAiAutoTimerRef = useRef<number | null>(null);
  const onRequestInlineContinuationRef = useRef(onRequestInlineContinuation);
  const onInlineSuggestionDismissedRef = useRef(onInlineSuggestionDismissed);

  const editor = useEditor({
    extensions: [
      StarterKit,
      TextStyle,
      FontFamily,
      FontSize,
      TextAlign.configure({
        types: ["heading", "paragraph"],
      }),
      InlineAiGhostExtension,
      Placeholder.configure({
        placeholder: t("editor.placeholder"),
      }),
    ],
    content,
    onUpdate({ editor }) {
      const nextContent = editor.getHTML();

      if (nextContent !== content) {
        onContentChange(nextContent);
      }
    },
  });

  const textStats = useEditorState({
    editor,
    selector: ({ editor: currentEditor }) => countEditorText(currentEditor?.getText() ?? ""),
  }) ?? { words: 0, characters: 0 };

  useEffect(() => {
    onRequestInlineContinuationRef.current = onRequestInlineContinuation;
  }, [onRequestInlineContinuation]);

  useEffect(() => {
    onInlineSuggestionDismissedRef.current = onInlineSuggestionDismissed;
  }, [onInlineSuggestionDismissed]);

  // getInlineAiContext is moved inside the useEffects that need it

  useEffect(() => {
    if (!editor) {
      return;
    }

    function clearInlineAiAutoTimer() {
      if (inlineAiAutoTimerRef.current) {
        window.clearTimeout(inlineAiAutoTimerRef.current);
        inlineAiAutoTimerRef.current = null;
      }
    }

    function handleEditorUpdate() {
      if (inlineAiSuggestion) {
        editor.commands.clearInlineAiGhost();
        onInlineSuggestionDismissedRef.current?.();
      }
    }
    editor.on("update", handleEditorUpdate);

    return () => {
      clearInlineAiAutoTimer();
      editor.off("update", handleEditorUpdate);
    };
  }, [
    editor,
    isInlineAiAutoEnabled,
    isInlineAiLoading,
    inlineAiSuggestion,
  ]);

  useEffect(() => {
    if (!editor || !replacement) {
      return;
    }

    editor
      .chain()
      .focus()
      .insertContentAt(
        {
          from: replacement.range.from,
          to: replacement.range.to,
        },
        buildFormattedReplacement(editor, replacement)
      )
      .run();

    onReplacementApplied();
  }, [editor, replacement, onReplacementApplied]);

  useEffect(() => {
    if (!editor) {
      return;
    }

    if (isInlineAiLoading && inlineAiRange) {
      editor.commands.setInlineAiGhost(
        t("editor.inlineAi.loading"),
        inlineAiRange.to,
        true
      );
      return;
    }

    if (!inlineAiSuggestion || !inlineAiRange) {
      editor.commands.clearInlineAiGhost();
      return;
    }

    const suggestion = inlineAiSuggestion.replace(/\s+/g, " ").trim();

    editor.commands.setInlineAiGhost(suggestion, inlineAiRange.to, false);
  }, [editor, isInlineAiLoading, inlineAiSuggestion, inlineAiRange, t]);

  useEffect(() => {
    if (!editor) {
      return;
    }

    if (editor.getHTML() !== content) {
      editor.commands.setContent(content || "", {
        emitUpdate: false,
      });
    }
  }, [content, editor]);


  function getSelectedText() {
    const { from, to } = editor.state.selection;
    return editor.state.doc.textBetween(from, to, " ");
  }

  function getSelectionRange(): EditorSelectionRange {
    const { from, to } = editor.state.selection;

    return { from, to };
  }

  function changeFontFamily(fontFamily: string) {
    if (!fontFamily) {
      editor.chain().focus().unsetFontFamily().run();
      return;
    }

    editor.chain().focus().setFontFamily(fontFamily).run();
  }

  function changeFontSize(fontSize: string) {
    if (!fontSize) {
      editor.chain().focus().unsetFontSize().run();
      return;
    }

    editor.chain().focus().setFontSize(`${fontSize}px`).run();
  }

  function findTextInEditor() {
    if (!searchText.trim()) {
      setSearchMessage("");
      return;
    }

    const matches = findEditorTextMatches(editor.state.doc, searchText);

    if (matches.length === 0) {
      setSearchMessage(t("editor.search.notFound"));
      return;
    }

    const { from: selectionFrom, to: selectionTo } = editor.state.selection;
    const currentMatch = matches.some(
      (match) => match.from === selectionFrom && match.to === selectionTo
    );
    const nextMatch = matches.find(
      (match) => match.from >= (currentMatch ? selectionTo : selectionFrom)
    ) ?? matches[0];

    editor.chain().focus().setTextSelection(nextMatch).run();
    setSearchMessage(t("editor.search.found"));
  }

  function replaceSelectedText() {
    const { from, to } = editor.state.selection;

    if (from === to) {
      findTextInEditor();
      return;
    }

    editor.view.dispatch(editor.state.tr.insertText(replaceText, from, to));
    editor.commands.focus();

    setSearchMessage(t("editor.search.replaced"));
  }

  function replaceAllText() {
    if (!searchText.trim()) {
      setSearchMessage("");
      return;
    }

    const matches = findEditorTextMatches(editor.state.doc, searchText);

    if (matches.length === 0) {
      setSearchMessage(t("editor.search.notFound"));
      return;
    }

    const transaction = matches
      .slice()
      .reverse()
      .reduce(
        (currentTransaction, match) =>
          currentTransaction.insertText(replaceText, match.from, match.to),
        editor.state.tr
      );

    editor.view.dispatch(transaction);
    editor.commands.focus();
    setSearchMessage(t("editor.search.replacedAll"));
  }

  useEffect(() => {
    if (!editor) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (!editor || !editor.isFocused) {
        return;
      }

      if (event.ctrlKey && event.code === "Space") {
        event.preventDefault();

        const { from, to } = editor.state.selection;

        let currentText = "";
        if (editor) {
          currentText = editor.state.doc.textBetween(0, to, "\n").trim().slice(-2200);
        }

        onRequestInlineContinuation?.({
          currentText,
          cursorRange: { from, to },
        });

        return;
      }

      if (
        event.key === "Tab" &&
        inlineAiSuggestion &&
        inlineAiRange &&
        !isInlineAiLoading
      ) {
        event.preventDefault();

        const suggestion = inlineAiSuggestion.replace(/\s+/g, " ").trim();

        editor.chain().focus().insertContentAt(inlineAiRange.to, suggestion).run();

        editor.commands.clearInlineAiGhost();
        onInlineSuggestionAccepted?.();
        return;
      }

      if (event.key === "Escape" && inlineAiSuggestion) {
        event.preventDefault();

        editor.commands.clearInlineAiGhost();
        onInlineSuggestionDismissed?.();
      }
    }

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [
    editor,
    inlineAiSuggestion,
    inlineAiRange,
    isInlineAiLoading,
    onRequestInlineContinuation,
    onInlineSuggestionAccepted,
    onInlineSuggestionDismissed,
  ]);

  if (!editor) return null;

  return (
    <div className="chapter-editor-shell">

      <BubbleMenu
        editor={editor}
        shouldShow={({ editor }) => !editor.state.selection.empty}
      >

        <div className="selection-ai-toolbar">
          {/* Controles de formatação básicos */}
          <div className="selection-format-group">
            <button
              className={`selection-format-action ${editor.isActive("bold") ? "is-active" : ""}`}
              type="button"
              onClick={() => editor.chain().focus().toggleBold().run()}
            >
              B
            </button>
            <button
              className={`selection-format-action ${editor.isActive("italic") ? "is-active" : ""}`}
              type="button"
              onClick={() => editor.chain().focus().toggleItalic().run()}
            >
              I
            </button>
          </div>

          <div className="selection-divider"></div>

          {/* Controles de IA */}
          <div className="selection-ai-group">
            {quickAiActions.map((action) => (
              <button
                key={action.actionType}
                className={`selection-ai-action ${action.icon ? "has-icon" : ""}`}
                type="button"
                onClick={() =>
                  onQuickAiAction(action.actionType, getSelectedText(), getSelectionRange())
                }
              >
                {action.icon && <span>{action.icon}</span>}
                {action.label}
              </button>
            ))}
          </div>
        </div>
      </BubbleMenu>
      {showToolbar && (
        <div className="rich-editor-toolbar">
          <div className="rich-editor-toolbar-group">
            <button
              className="rich-editor-toolbar-button"
              type="button"
              onClick={() => editor.chain().focus().undo().run()}
              title={t("editor.toolbar.undo")}
              aria-label={t("editor.toolbar.undo")}
            >
              <IconUndo />
            </button>

            <button
              className="rich-editor-toolbar-button"
              type="button"
              onClick={() => editor.chain().focus().redo().run()}
              title={t("editor.toolbar.redo")}
              aria-label={t("editor.toolbar.redo")}
            >
              <IconRedo />
            </button>
          </div>

          <div className="rich-editor-toolbar-group">
            <select
              className="rich-editor-select"
              title={t("editor.toolbar.font")}
              defaultValue=""
              onChange={(event) => changeFontFamily(event.target.value)}
            >
              <option value="">Fonte</option>
              <option value="Inter, sans-serif">Inter</option>
              <option value="Lora, serif">Lora</option>
              <option value='"Playfair Display", serif'>Playfair Display</option>
            </select>

            <select
              className="rich-editor-select"
              title={t("editor.toolbar.size")}
              defaultValue=""
              onChange={(event) => changeFontSize(event.target.value)}
            >
              <option value="">Tamanho</option>
              <option value="16">16</option>
              <option value="18">18</option>
              <option value="20">20</option>
              <option value="22">22</option>
              <option value="24">24</option>
            </select>
          </div>

          <div className="rich-editor-toolbar-group">
            <button
              className={`rich-editor-toolbar-button ${editor.isActive("bold") ? "is-active" : ""
                }`}
              type="button"
              onClick={() => editor.chain().focus().toggleBold().run()}
              title={t("editor.toolbar.bold")}
            >
              B
            </button>

            <button
              className={`rich-editor-toolbar-button ${editor.isActive("italic") ? "is-active" : ""
                }`}
              type="button"
              onClick={() => editor.chain().focus().toggleItalic().run()}
              title={t("editor.toolbar.italic")}
            >
              I
            </button>

            <button
              className={`rich-editor-toolbar-button ${editor.isActive("underline") ? "is-active" : ""
                }`}
              type="button"
              onClick={() => editor.chain().focus().toggleUnderline().run()}
              title={t("editor.toolbar.underline")}
            >
              U
            </button>
          </div>

          <div className="rich-editor-toolbar-group">
            <button
              className={`rich-editor-toolbar-button ${editor.isActive({ textAlign: "left" }) ? "is-active" : ""
                }`}
              type="button"
              onClick={() => editor.chain().focus().setTextAlign("left").run()}
              title={t("editor.toolbar.alignLeft")}
              aria-label={t("editor.toolbar.alignLeft")}
            >
              <IconAlignLeft />
            </button>

            <button
              className={`rich-editor-toolbar-button ${editor.isActive({ textAlign: "center" }) ? "is-active" : ""
                }`}
              type="button"
              onClick={() => editor.chain().focus().setTextAlign("center").run()}
              title={t("editor.toolbar.alignCenter")}
              aria-label={t("editor.toolbar.alignCenter")}
            >
              <IconAlignCenter />
            </button>

            <button
              className={`rich-editor-toolbar-button ${editor.isActive({ textAlign: "right" }) ? "is-active" : ""
                }`}
              type="button"
              onClick={() => editor.chain().focus().setTextAlign("right").run()}
              title={t("editor.toolbar.alignRight")}
              aria-label={t("editor.toolbar.alignRight")}
            >
              <IconAlignRight />
            </button>

            <button
              className={`rich-editor-toolbar-button ${editor.isActive({ textAlign: "justify" }) ? "is-active" : ""
                }`}
              type="button"
              onClick={() => editor.chain().focus().setTextAlign("justify").run()}
              title={t("editor.toolbar.alignJustify")}
              aria-label={t("editor.toolbar.alignJustify")}
            >
              <IconAlignJustify />
            </button>
          </div>

          <div className="rich-editor-toolbar-group rich-editor-toolbar-end">
            {onCreateNextChapter && (
              <button
                className="rich-editor-next-chapter-button"
                type="button"
                onClick={onCreateNextChapter}
                disabled={isCreatingNextChapter}
              >
                {isCreatingNextChapter
                  ? t("editor.nextChapter.creating")
                  : t("editor.nextChapter.create")}
              </button>
            )}
            {onGenerateChapterSummary && (
              <button
                className="rich-editor-summary-button"
                type="button"
                onClick={onGenerateChapterSummary}
                disabled={isGeneratingSummary}
              >
                {isGeneratingSummary
                  ? t("editor.summary.generating")
                  : t("editor.summary.updateMemory")}
              </button>
            )}

            {onOpenChapterBeatsModal && (
              <button
                className="rich-editor-next-chapter-button"
                type="button"
                onClick={onOpenChapterBeatsModal}
                disabled={isGeneratingChapterFromBeats}
              >
                {isGeneratingChapterFromBeats
                  ? t("editor.chapterFromBeats.generating")
                  : t("editor.chapterFromBeats.open")}
              </button>
            )}

            <button
              className="rich-editor-search-button"
              type="button"
              title={t("editor.toolbar.searchReplace")}
              aria-label={t("editor.toolbar.searchReplace")}
              onClick={() => setIsSearchReplaceOpen((current) => !current)}
            >
              <IconSearch />
            </button>
          </div>
        </div>
      )}

      {isSearchReplaceOpen && (
        <div className="search-replace-panel">
          <label className="search-replace-field">
            <span>{t("editor.search.searchLabel")}</span>
            <input
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
              placeholder={t("editor.search.searchPlaceholder")}
            />
          </label>

          <label className="search-replace-field">
            <span>{t("editor.search.replaceLabel")}</span>
            <input
              value={replaceText}
              onChange={(event) => setReplaceText(event.target.value)}
              placeholder={t("editor.search.replacePlaceholder")}
            />
          </label>

          <div className="search-replace-actions">
            <button type="button" onClick={findTextInEditor}>
              {t("editor.search.find")}
            </button>

            <button type="button" onClick={replaceSelectedText}>
              {t("editor.search.replace")}
            </button>

            <button type="button" onClick={replaceAllText}>
              {t("editor.search.replaceAll")}
            </button>
          </div>

          {searchMessage && (
            <p className="search-replace-message">{searchMessage}</p>
          )}
        </div>
      )}

      <div className="chapter-heading-row">
        <input
          className="chapter-heading-input"
          value={chapterTitle}
          onChange={(event) => onChapterTitleChange(event.target.value)}
          placeholder={chapterTitlePlaceholder}
        />
        {onGenerateChapterTitle && (
          <button
            className="chapter-title-ai-button"
            type="button"
            onClick={onGenerateChapterTitle}
            disabled={isGeneratingChapterTitle}
            title={t("editor.chapterTitleAi.button")}
            aria-label={t("editor.chapterTitleAi.button")}
          >
            <IconSparkle aria-hidden="true" />
            <span>{isGeneratingChapterTitle ? t("editor.chapterTitleAi.generating") : t("editor.chapterTitleAi.button")}</span>
          </button>
        )}
      </div>

      <EditorContent editor={editor} className="chapter-rich-editor" />

      <div className="chapter-editor-stats" role="status" aria-live="polite">
        <span><strong>{textStats.words}</strong> {t(textStats.words === 1 ? "editor.stats.word" : "editor.stats.words")}</span>
        <span aria-hidden="true">·</span>
        <span><strong>{textStats.characters}</strong> {t(textStats.characters === 1 ? "editor.stats.character" : "editor.stats.characters")}</span>
      </div>

    </div>
  );
}
