import { useEffect, useRef, useState } from "react";
import { useLocation, useParams } from "react-router-dom";
import {
  ChapterRichEditor,
  type EditorReplacement,
  type EditorSelectionRange,
} from "../components/workspace/ChapterRichEditor";
import {
  createChapter,
  deleteChapter,
  getChapterById,
  listChaptersByStory,
  updateChapter,
} from "../services/chapterService";
import {
  ChapterAutosaveQueue,
} from "../services/chapterAutosaveQueue";
import type { ApiChapter } from "../types/chapter";
import { AiAssistantPanel } from "../components/workspace/AiAssistantPanel";
import { generateAiText, estimateAiText } from "../services/aiService";
import type { AiActionType, AiGenerateResult, GenerateAiPayload } from "../types/ai";
import type { AiEstimateResponse } from "../types/credits";
import { useCredits } from "../hooks/useCredits";
import { AiConfirmModal } from "../components/workspace/AiConfirmModal";
import { htmlToPlainText } from "../utils/textUtils";
import { plainTextToFormattedHtmlParagraphs } from "../utils/editorFormatting";
import { resolveAiSelectionRange } from "../utils/aiSelection";
import { executeEstimatedAiAction } from "../utils/aiEstimateFlow";
import { useTranslation } from "react-i18next";
import { AiSuggestionModal } from "../components/workspace/AiSuggestionModal";
import { ChapterTitleSuggestionModal } from "../components/workspace/ChapterTitleSuggestionModal";
import { ChapterReviewModal } from "../components/workspace/ChapterReviewModal";
import { CharacterWorkspace } from "../components/characters/CharacterWorkspace";
import { WorldWorkspace } from "../components/world/WorldWorkspace";
import { generateChapterSummary } from "../services/chapterSummaryService";
import { useNavigate } from "react-router-dom";
import { getStoryById } from "../services/storyService";
import { LoadingScreen } from "../components/ui/LoadingScreen";
import { useLoadingPresentation } from "../hooks/useLoadingPresentation";
import { WorkspaceTopbar } from "../components/workspace/WorkspaceTopbar";
import { IconTrash, IconFocus, IconMinimize, IconFileText, IconChevronLeftDouble, IconChevronRightDouble } from "../components/icons/Icons";


function wait(milliseconds: number) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, milliseconds);
  });
}

function getCurrentTimestamp() {
  return Date.now();
}

function getAiErrorMessage(e: unknown): string {
  const err = e as { message?: string; name?: string };
  const msg = err.message || "";
  if (msg.includes("402") || msg.includes("AI_INSUFFICIENT_CREDITS") || msg.includes("Saldo insuficiente")) return "Créditos insuficientes para esta geração.";
  if (msg.includes("AI_MODEL_PRICING_NOT_FOUND")) return "Preço do modelo não encontrado. Contate o suporte.";
  if (msg.includes("AI_CREDIT_ACCOUNT_NOT_FOUND")) return "Conta de créditos não encontrada.";
  if (msg.includes("AI_CREDIT_RESERVATION_FAILED")) return "Falha ao reservar créditos.";
  if (msg.includes("AI_CREDIT_CAPTURE_FAILED")) return "Falha ao capturar créditos.";
  if (msg.includes("AI_CREDIT_RELEASE_FAILED")) return "Falha ao liberar créditos.";
  if (msg.includes("AI_ESTIMATE_EXPIRED")) return "A estimativa expirou. Tente novamente.";
  return msg || "Não foi possível gerar texto com IA.";
}

type WorkspaceNotification = {
  type: "success" | "error" | "info";
  message: string;
};

type StoryWorkspaceTab =
  | "writing"
  | "characters"
  | "world"
  | "notes"
  | "references";

function getActiveWorkspaceTab(pathname: string): StoryWorkspaceTab {
  if (pathname.includes("/characters")) {
    return "characters";
  }

  if (pathname.includes("/world")) {
    return "world";
  }

  if (pathname.includes("/notes")) {
    return "notes";
  }

  if (pathname.includes("/references")) {
    return "references";
  }

  return "writing";
}


export type AiSelectionSnapshot = {
  chapterId: number;
  from: number;
  to: number;
  selectedText: string;
  action: AiActionType;
  requestId: string;
  idempotencyKey: string;
  documentVersion: number;
  createdAt: number;
};

export function StoryWorkspacePage() {
  const { storyId } = useParams();
  const location = useLocation();

  const activeTab = getActiveWorkspaceTab(location.pathname);
  const [isChapterPanelOpen, setIsChapterPanelOpen] = useState(true);
  const [isAiPanelOpen, setIsAiPanelOpen] = useState(true);
  const [isFocusMode, setIsFocusMode] = useState(false);
  const [chapterEditorContent, setChapterEditorContent] = useState("");

  const [isMobileChapterDrawerOpen, setIsMobileChapterDrawerOpen] = useState(false);
  const [isAiMobileSheetOpen, setIsAiMobileSheetOpen] = useState(false);

  useEffect(() => {
    const mq = window.matchMedia("(min-width: 1200px)");
    function handleChange(e: MediaQueryListEvent) {
      if (e.matches) {
        setIsMobileChapterDrawerOpen(false);
        setIsAiMobileSheetOpen(false);
      }
    }
    mq.addEventListener("change", handleChange);
    return () => mq.removeEventListener("change", handleChange);
  }, []);

  useEffect(() => {
    if (isMobileChapterDrawerOpen || isAiMobileSheetOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [isMobileChapterDrawerOpen, isAiMobileSheetOpen]);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setIsMobileChapterDrawerOpen(false);
        setIsAiMobileSheetOpen(false);
      }
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

  const [chapters, setChapters] = useState<ApiChapter[]>([]);
  const [isCreatingChapter, setIsCreatingChapter] = useState(false);
  const [chapterTitleInput, setChapterTitleInput] = useState("");
  const [selectedChapterId, setSelectedChapterId] = useState<number | null>(null);
  const [isLoadingChapters, setIsLoadingChapters] = useState(true);
  const [chaptersError, setChaptersError] = useState("");

  const [saveChapterStatus, setSaveChapterStatus] = useState("Salvo");
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [chapterSaveConflictId, setChapterSaveConflictId] =
    useState<number | null>(null);
  const [isGeneratingChapterSummary, setIsGeneratingChapterSummary] =
    useState(false);
  const [isCreatingNextChapter, setIsCreatingNextChapter] = useState(false);
  const lastAutoSaveAt = useRef(0);

  const [workspaceNotification, setWorkspaceNotification] =
    useState<WorkspaceNotification | null>(null);

  const documentVersionRef = useRef(0);
  const activeAiRequestRef = useRef<AbortController | null>(null);
  const currentAiRequestIdRef = useRef<string | null>(null);
  const quickAiActionInFlightRef = useRef(false);

  const [chapterIdPendingDelete, setChapterIdPendingDelete] =
    useState<number | null>(null);

  const notificationTimeoutRef = useRef<number | null>(null);

  const selectedChapterStorageKey = storyId
    ? `novelcopilot:selected-chapter:${storyId}`
    : "";

  const [isAiSuggestionModalOpen, setIsAiSuggestionModalOpen] = useState(false);
  const [selectedTextForAi, setSelectedTextForAi] = useState("");

  const [selectedEditorRange, setSelectedEditorRange] =
    useState<EditorSelectionRange | null>(null);

  const [editorReplacement, setEditorReplacement] =
    useState<EditorReplacement | null>(null);

  const navigate = useNavigate();
  const [isCheckingStoryAccess, setIsCheckingStoryAccess] = useState(true);
  const [storyAccessError, setStoryAccessError] = useState("");
  const storyLoadingPresentation = useLoadingPresentation(isCheckingStoryAccess);

  const [isChapterBeatsModalOpen, setIsChapterBeatsModalOpen] = useState(false);
  const [chapterBeatsMode, setChapterBeatsMode] =
    useState<"STRICT" | "FLEXIBLE">("FLEXIBLE");
  const [isGeneratingChapterFromBeats, setIsGeneratingChapterFromBeats] =
    useState(false);
  const [isGeneratingChapterTitle, setIsGeneratingChapterTitle] = useState(false);
  const [chapterTitleSuggestions, setChapterTitleSuggestions] =
    useState<[string, string, string] | null>(null);
  const [isChapterReviewModalOpen, setIsChapterReviewModalOpen] = useState(false);
  const [isReplaceEditorConfirmOpen, setIsReplaceEditorConfirmOpen] =
    useState(false);

  const [aiSelectionSnapshot, setAiSelectionSnapshot] = useState<AiSelectionSnapshot | null>(null);
  const [inlineAiSuggestion, setInlineAiSuggestion] = useState("");
  const [inlineAiRange, setInlineAiRange] =
    useState<EditorSelectionRange | null>(null);
  const [isInlineAiLoading, setIsInlineAiLoading] = useState(false);
  const [isInlineAiAutoEnabled, setIsInlineAiAutoEnabled] = useState(false);

  const { t } = useTranslation("workspace");
  const { refreshBalance } = useCredits();
  const [chapterSaveQueue] = useState(
    () =>
      new ChapterAutosaveQueue(updateChapter, {
        onSaved: () => undefined,
        onConflict: () => undefined,
        onError: () => undefined,
      })
  );

  useEffect(() => {
    chapterSaveQueue.setEvents({
      onSaved: (savedChapter, hasPendingSave) => {
        setChapters((currentChapters) =>
          currentChapters.map((chapter) =>
            chapter.id === savedChapter.id ? savedChapter : chapter
          )
        );
        lastAutoSaveAt.current = getCurrentTimestamp();
        setHasUnsavedChanges(hasPendingSave);
        setSaveChapterStatus(
          hasPendingSave
            ? t("editor.saveStatus.unsaved")
            : t("editor.saveStatus.saved")
        );
      },
      onConflict: (chapterId) => {
        setChapterSaveConflictId(chapterId);
        setHasUnsavedChanges(true);
        setSaveChapterStatus(t("editor.saveStatus.conflict"));
      },
      onError: () => {
        setHasUnsavedChanges(true);
        setSaveChapterStatus(t("editor.saveStatus.error"));
      },
    });
  }, [chapterSaveQueue, t]);

  // --- AI Estimate State ---
  const [aiEstimate, setAiEstimate] = useState<AiEstimateResponse | null>(null);
  const [aiEstimateCallback, setAiEstimateCallback] = useState<((payload: GenerateAiPayload) => Promise<void>) | null>(null);
  const [estimatedPayloadHash, setEstimatedPayloadHash] = useState<string | null>(null);
  const [estimatedPayloadBuilder, setEstimatedPayloadBuilder] = useState<(() => GenerateAiPayload) | null>(null);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<AiGenerateResult | null>(null);
  const [aiInstructionText, setAiInstructionText] = useState("");
  const [aiErrorMessage, setAiErrorMessage] = useState("");
  const estimateContextRef = useRef({
    chapterEditorContent,
    selectedTextForAi,
  });

  function openAiSuggestionModal() {
    setIsAiMobileSheetOpen(false);
    setIsAiSuggestionModalOpen(true);
  }

  function handleOpenHistoryResult(result: AiGenerateResult) {
    if (result.format === "title-suggestions") {
      setChapterTitleSuggestions(result.options);
      setIsAiMobileSheetOpen(false);
      return;
    }
    if (result.format === "review") {
      setAiResult(result);
      setIsAiMobileSheetOpen(false);
      setIsChapterReviewModalOpen(true);
      return;
    }
    setAiResult(result);
    setSelectedTextForAi("");
    setSelectedEditorRange(null);
    setAiSelectionSnapshot(null);
    openAiSuggestionModal();
  }

  function getPayloadHash(payload: GenerateAiPayload) {
    return JSON.stringify(
      Object.fromEntries(
        Object.entries(payload).filter(
          ([key]) => key !== "idempotencyKey" && key !== "abortSignal",
        ),
      ),
    );
  }

  async function wrapWithEstimate(
    buildPayload: () => GenerateAiPayload,
    executeCall: (payload: GenerateAiPayload) => Promise<void>,
    requestId?: string,
    executeImmediately = false
  ) {
    setIsAiLoading(true);
    setAiErrorMessage("");

    try {
      const payload = buildPayload();
      const estimate = await estimateAiText(Number(storyId), payload);

      if (requestId && currentAiRequestIdRef.current !== requestId) {
        return;
      }

      if (executeImmediately) {
        const executed = await executeEstimatedAiAction(estimate, payload, executeCall);
        if (!executed) {
          const message = estimate.exceedsPerRequestLimit
            ? `Esta ação ultrapassa o limite seguro de ${estimate.maximumCreditsPerGeneration ?? 0} créditos.`
            : "Créditos insuficientes para esta geração.";
          setAiErrorMessage(message);
          showWorkspaceNotification("error", message);
          return;
        }

        return;
      }

      setAiEstimate(estimate);
      setEstimatedPayloadHash(getPayloadHash(payload));
      setEstimatedPayloadBuilder(() => buildPayload);
      setAiEstimateCallback(() => executeCall);
    } catch (e: unknown) {
      if (e instanceof Error && e.name === "AbortError") {
        return;
      }

      setAiErrorMessage(getAiErrorMessage(e));
      showWorkspaceNotification("error", getAiErrorMessage(e));
    } finally {
      if (!requestId || currentAiRequestIdRef.current === requestId) {
        setIsAiLoading(false);
      }
    }
  }

  async function handleConfirmAiGeneration() {
    if (!aiEstimateCallback || !aiEstimate?.canGenerate || !estimatedPayloadBuilder || !estimatedPayloadHash) return;
    
    const currentPayload = estimatedPayloadBuilder();
    const currentHash = getPayloadHash(currentPayload);

    if (currentHash !== estimatedPayloadHash) {
      setAiEstimate(null);
      showWorkspaceNotification("info", "O conteúdo mudou desde a estimativa. O custo será recalculado.");
      await wrapWithEstimate(estimatedPayloadBuilder, aiEstimateCallback);
      return;
    }

    setAiEstimate(null);
    try {
      await aiEstimateCallback(currentPayload);
    } finally {
      setAiEstimateCallback(null);
      setEstimatedPayloadBuilder(null);
      setEstimatedPayloadHash(null);
      refreshBalance();
    }
  }

  function handleCancelAiGeneration() {
    activeAiRequestRef.current?.abort();
    activeAiRequestRef.current = null;
    currentAiRequestIdRef.current = null;
    setIsAiLoading(false);
    setAiEstimate(null);
    setAiEstimateCallback(null);
    setEstimatedPayloadBuilder(null);
    setEstimatedPayloadHash(null);
  }

  useEffect(() => {
    const contextChanged =
      estimateContextRef.current.chapterEditorContent !== chapterEditorContent
      || estimateContextRef.current.selectedTextForAi !== selectedTextForAi;

    estimateContextRef.current = {
      chapterEditorContent,
      selectedTextForAi,
    };

    if (!contextChanged || !aiEstimate) {
      return;
    }

    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) {
        return;
      }
      setAiEstimate(null);
      setAiEstimateCallback(null);
      setEstimatedPayloadBuilder(null);
      setEstimatedPayloadHash(null);
    });

    return () => {
      cancelled = true;
    };
  }, [aiEstimate, chapterEditorContent, selectedTextForAi]);

  async function handleCreateChapter() {
    if (!storyId || isCreatingChapter) {
      return;
    }

    try {
      setIsCreatingChapter(true);

      let memoryGenerationFailed = false;
      if (selectedChapter && htmlToPlainText(chapterEditorContent).trim()) {
        setSaveChapterStatus(t("editor.saveStatus.saving"));
        const savedChapter = await chapterSaveQueue.enqueue(selectedChapter.id, {
          title: chapterTitleInput,
          chapterOrder: selectedChapter.chapterOrder,
          type: selectedChapter.type,
          goal: selectedChapter.goal,
          summary: selectedChapter.summary,
          content: chapterEditorContent,
          version: selectedChapter.version,
        });

        try {
          const summaryResponse = await generateChapterSummary(
            Number(storyId),
            savedChapter.id
          );
          setChapters((currentChapters) =>
            currentChapters.map((chapter) =>
              chapter.id === summaryResponse.chapterId
                ? {
                  ...chapter,
                  summary: summaryResponse.summary,
                  version: summaryResponse.chapterVersion,
                  summaryContentHash: summaryResponse.summaryContentHash,
                  summaryGeneratedAt: summaryResponse.summaryGeneratedAt,
                }
                : chapter
            )
          );
        } catch {
          memoryGenerationFailed = true;
        }
      }

      const nextChapterOrder = chapters.length + 1;

      const createdChapter = await createChapter(Number(storyId), {
        title: `Capítulo ${nextChapterOrder}`,
        chapterOrder: nextChapterOrder,
        type: "chapter",
        goal: "",
        summary: "",
        content: "",
      });

      setChapters((currentChapters) => [...currentChapters, createdChapter]);
      setSelectedChapterId(createdChapter.id);
      if (selectedChapterStorageKey) {
        localStorage.setItem(selectedChapterStorageKey, String(createdChapter.id));
      }
      setChapterEditorContent(createdChapter.content ?? "");
      setChapterTitleInput(createdChapter.title);
      setHasUnsavedChanges(false);
      setChapterSaveConflictId(null);
      chapterSaveQueue.reset();
      setSaveChapterStatus(t("editor.saveStatus.saved"));
      lastAutoSaveAt.current = getCurrentTimestamp();

      if (memoryGenerationFailed) {
        showWorkspaceNotification("error", t("chapters.createdWithoutMemory"));
      }
    } catch {
      showWorkspaceNotification("error", t("chapters.createError"));
    } finally {
      setIsCreatingChapter(false);
    }
  }

  function handleRequestDeleteChapter(chapterId: number) {
    if (chapters.length <= 1) {
      showWorkspaceNotification("error", t("chapters.deleteOnlyChapterError"));
      return;
    }

    setChapterIdPendingDelete(chapterId);
  }

  async function handleConfirmDeleteChapter() {
    if (!chapterIdPendingDelete) {
      return;
    }

    try {
      await deleteChapter(chapterIdPendingDelete);

      const deletedChapterIndex = chapters.findIndex(
        (chapter) => chapter.id === chapterIdPendingDelete
      );

      const remainingChapters = chapters.filter(
        (chapter) => chapter.id !== chapterIdPendingDelete
      );

      setChapters(remainingChapters);

      const nextSelectedChapter =
        remainingChapters[deletedChapterIndex] ??
        remainingChapters[deletedChapterIndex - 1] ??
        remainingChapters[0];

      setSelectedChapterId(nextSelectedChapter.id);
      if (selectedChapterStorageKey) {
        localStorage.setItem(selectedChapterStorageKey, String(nextSelectedChapter.id));
      }
      setChapterEditorContent(nextSelectedChapter.content ?? "");
      setChapterTitleInput(nextSelectedChapter.title);
      setHasUnsavedChanges(false);
      setChapterSaveConflictId(null);
      chapterSaveQueue.reset();
      setSaveChapterStatus(t("editor.saveStatus.saved"));
      lastAutoSaveAt.current = getCurrentTimestamp();

      showWorkspaceNotification("success", t("chapters.deleteSuccess"));
    } catch {
      showWorkspaceNotification("error", t("chapters.deleteError"));
    } finally {
      setChapterIdPendingDelete(null);
    }
  }

  function showWorkspaceNotification(
    type: WorkspaceNotification["type"],
    message: string
  ) {
    setWorkspaceNotification({ type, message });

    if (notificationTimeoutRef.current) {
      window.clearTimeout(notificationTimeoutRef.current);
    }

    notificationTimeoutRef.current = window.setTimeout(() => {
      setWorkspaceNotification(null);
    }, 3500);
  }

  async function handleRequestInlineContinuation(payload: {
    currentText: string;
    cursorRange: EditorSelectionRange;
  }) {
    if (!storyId || !selectedChapter || isInlineAiLoading) {
      return;
    }

    if (!payload.currentText.trim()) {
      showWorkspaceNotification("error", "Escreva algo antes de pedir continuação.");
      return;
    }

    wrapWithEstimate(() => {
      return {
        chapterId: selectedChapter.id,
        actionType: "INLINE_CONTINUATION",
        instruction:
          "Continue exatamente a última frase e avance só um pequeno trecho da cena. Não resuma, não recapitule, não conclua a cena e não avance demais no tempo. Entregue entre 80 e 180 palavras.",
        currentText: payload.currentText,
        selectedText: null,
      };
    }, async (aiPayload) => {
      try {
        setIsInlineAiLoading(true);
        setInlineAiSuggestion("");
        setInlineAiRange(payload.cursorRange);

        const response = await generateAiText(Number(storyId), aiPayload);

        setInlineAiSuggestion(response.result.format === "prose" ? response.result.content : "");
      } catch (e: unknown) {
        showWorkspaceNotification("error", getAiErrorMessage(e));
      } finally {
        setIsInlineAiLoading(false);
        refreshBalance();
      }
    });
  }

  async function handleCopyAiSuggestion(textToCopy: string) {
    if (!textToCopy) {
      return;
    }

    await navigator.clipboard.writeText(textToCopy);
    showWorkspaceNotification("success", t("ai.suggestion.copied"));
  }

  function handleReplaceSelectedTextWithAiSuggestion(textToReplaceWith: string) {
    if (!textToReplaceWith || !aiSelectionSnapshot || !selectedEditorRange || !selectedChapter) {
      return;
    }

    if (aiSelectionSnapshot.chapterId !== selectedChapter.id) {
      showWorkspaceNotification("error", "O trecho foi alterado desde o pedido. A sugestão não foi aplicada automaticamente.");
      return;
    }

    if (currentAiRequestIdRef.current !== aiSelectionSnapshot.requestId) {
      showWorkspaceNotification("error", "O trecho foi alterado desde o pedido. A sugestão não foi aplicada automaticamente.");
      return;
    }

    if (documentVersionRef.current !== aiSelectionSnapshot.documentVersion) {
      showWorkspaceNotification("error", "O trecho foi alterado desde o pedido. A sugestão não foi aplicada automaticamente.");
      return;
    }

    setEditorReplacement({
      text: plainTextToFormattedHtmlParagraphs(textToReplaceWith),
      range: selectedEditorRange,
    });

    setHasUnsavedChanges(true);
    setSaveChapterStatus(t("editor.saveStatus.unsaved"));
    setIsAiSuggestionModalOpen(false);
    showWorkspaceNotification("success", t("ai.suggestion.replaced"));
  }

  async function handleGenerateChapterSummary() {
    if (!storyId || !selectedChapter || isGeneratingChapterSummary) {
      return;
    }

    if (!htmlToPlainText(chapterEditorContent).trim()) {
      showWorkspaceNotification("error", t("editor.summary.empty"));
      return;
    }

    try {
      setIsGeneratingChapterSummary(true);
      setSaveChapterStatus(t("editor.saveStatus.saving"));

      const savedChapter = await chapterSaveQueue.enqueue(selectedChapter.id, {
        title: chapterTitleInput,
        chapterOrder: selectedChapter.chapterOrder,
        type: selectedChapter.type,
        goal: selectedChapter.goal,
        summary: selectedChapter.summary,
        content: chapterEditorContent,
        version: selectedChapter.version,
      });

      const summaryResponse = await generateChapterSummary(
        Number(storyId),
        savedChapter.id
      );

      setChapters((currentChapters) =>
        currentChapters.map((chapter) =>
          chapter.id === summaryResponse.chapterId
            ? {
              ...chapter,
              summary: summaryResponse.summary,
              version: summaryResponse.chapterVersion,
              summaryContentHash: summaryResponse.summaryContentHash,
              summaryGeneratedAt: summaryResponse.summaryGeneratedAt,
            }
            : chapter
        )
      );

      showWorkspaceNotification("success", t("editor.summary.success"));
    } catch (error: unknown) {
      setSaveChapterStatus(t("editor.saveStatus.error"));
      showWorkspaceNotification(
        "error",
        error instanceof Error ? error.message : t("editor.summary.error")
      );
    } finally {
      setIsGeneratingChapterSummary(false);
    }
  }

  async function handleCreateNextChapter() {
    if (!storyId || !selectedChapter || isCreatingNextChapter) {
      return;
    }

    wrapWithEstimate(() => {
      return {
        chapterId: selectedChapter.id,
        actionType: "CREATE_NEXT_CHAPTER",
        instruction:
          "Crie o próximo capítulo como uma unidade narrativa completa. Desenvolva a situação com começo, progressão, complicação e consequência. Não entregue apenas uma continuação curta da última cena.",
        currentText: htmlToPlainText(chapterEditorContent),
        selectedText: null,
      };
    }, async (payload) => {
      try {
        setIsCreatingNextChapter(true);
        setAiErrorMessage("");
        setAiResult(null);
        setSelectedTextForAi("");
        setSelectedEditorRange(null);

        const response = await generateAiText(Number(storyId), payload);

        setAiResult(response.result);
        openAiSuggestionModal();
      } catch (e: unknown) {
        showWorkspaceNotification("error", getAiErrorMessage(e));
      } finally {
        setIsCreatingNextChapter(false);
        refreshBalance();
      }
    });
  }

  async function handleGenerateChapterFromBeats() {
    if (!storyId || !selectedChapter || isGeneratingChapterFromBeats) {
      return;
    }

    const chapterIdeasText = htmlToPlainText(chapterEditorContent).trim();

    if (!chapterIdeasText) {
      showWorkspaceNotification("error", t("editor.chapterFromBeats.empty"));
      return;
    }

    wrapWithEstimate(() => {
      const modeInstruction =
        chapterBeatsMode === "STRICT"
          ? "Modo FIEL. Use o texto do editor como acontecimentos obrigatórios para o capítulo. Não resuma. Desenvolva cada acontecimento em cenas completas, preservando a ordem e a intenção do usuário."
          : "Modo FLEXIVEL. Use o texto do editor como base para criar um capítulo completo. Pode melhorar ritmo, tensão e ordem dos acontecimentos, mas não deve transformar a ideia em resumo curto.";

      return {
        chapterId: selectedChapter.id,
        actionType: "WRITE_CHAPTER_FROM_BEATS",
        instruction: modeInstruction,
        currentText: htmlToPlainText(chapterEditorContent).trim(),
        selectedText: null,
      };
    }, async (payload) => {
      try {
        setIsGeneratingChapterFromBeats(true);
        setAiErrorMessage("");
        setAiResult(null);
        setSelectedTextForAi("");
        setSelectedEditorRange(null);

        const response = await generateAiText(Number(storyId), payload);

        setAiResult(response.result);
        setIsChapterBeatsModalOpen(false);
        openAiSuggestionModal();
      } catch (e: unknown) {
        showWorkspaceNotification("error", getAiErrorMessage(e));
      } finally {
        setIsGeneratingChapterFromBeats(false);
        refreshBalance();
      }
    });
  }

  function handleClearInlineAiSuggestion() {
    setInlineAiSuggestion("");
    setInlineAiRange(null);
  }

  function handleToggleInlineAiAuto() {
    setIsInlineAiAutoEnabled((current) => {
      const next = !current;

      localStorage.setItem("novelcopilot:inline-ai-auto", String(next));

      if (!next) {
        handleClearInlineAiSuggestion();
      }

      return next;
    });
  }

  const aiActions = [
    {
      title: t("ai.actions.continue.title"),
      description: t("ai.actions.continue.description"),
      actionType: "CONTINUE_SCENE" as const,
    },
    {
      title: t("ai.actions.rewrite.title"),
      description: t("ai.actions.rewrite.description"),
      actionType: "REWRITE_SELECTION" as const,
    },
    {
      title: t("ai.actions.review.title"),
      description: t("ai.actions.review.description"),
      actionType: "REVIEW_CHAPTER" as const,
    },
    {
      title: t("ai.actions.plan.title"),
      description: t("ai.actions.plan.description"),
      actionType: "PLAN_SCENE" as const,
    },
  ];

  useEffect(() => {
    let isCancelled = false;

    async function checkStoryAccess() {
      if (!storyId) {
        await wait(650);

        if (!isCancelled) {
          navigate("/not-found", { replace: true });
        }

        return;
      }

      try {
        setIsCheckingStoryAccess(true);
        setStoryAccessError("");

        setChapters([]);
        setSelectedChapterId(null);
        setChapterEditorContent("");
        setChapterTitleInput("");

        await Promise.all([
          getStoryById(Number(storyId)),
          wait(650),
        ]);

        if (!isCancelled) {
          setIsCheckingStoryAccess(false);
        }
      } catch (error) {
        await wait(650);

        if (isCancelled) {
          return;
        }

        if (error instanceof Error && error.message === "NOT_FOUND") {
          navigate("/not-found", { replace: true });
          return;
        }

        setStoryAccessError("Não foi possível carregar esta história.");
        setIsCheckingStoryAccess(false);
      }
    }

    checkStoryAccess();

    return () => {
      isCancelled = true;
    };
  }, [storyId, navigate]);

  useEffect(() => {
    let isCancelled = false;

    async function loadChapters() {
      if (!storyId || isCheckingStoryAccess) {
        return;
      }

      try {
        setIsLoadingChapters(true);
        setChaptersError("");
        setChapterSaveConflictId(null);
        setHasUnsavedChanges(false);
        chapterSaveQueue.reset();

        const chaptersFromApi = await listChaptersByStory(Number(storyId));

        if (isCancelled) {
          return;
        }

        setChapters(chaptersFromApi);

        if (chaptersFromApi.length > 0) {
          const storedChapterId = selectedChapterStorageKey
            ? Number(localStorage.getItem(selectedChapterStorageKey))
            : null;

          const initialChapter =
            chaptersFromApi.find((chapter) => chapter.id === storedChapterId) ??
            chaptersFromApi[0];

          setSelectedChapterId(initialChapter.id);

          const initialContent = initialChapter.content ?? "";

          setChapterEditorContent(initialContent);
          setChapterTitleInput(initialChapter.title);
          setHasUnsavedChanges(false);
          setChapterSaveConflictId(null);
          setSaveChapterStatus(t("editor.saveStatus.saved"));
          lastAutoSaveAt.current = getCurrentTimestamp();
        }
      } catch (error) {
        if (isCancelled) {
          return;
        }

        if (error instanceof Error && error.message === "NOT_FOUND") {
          navigate("/not-found", { replace: true });
          return;
        }

        setChaptersError(t("workspace:chapters.loadError"));
      } finally {
        if (!isCancelled) {
          setIsLoadingChapters(false);
        }
      }
    }

    loadChapters();

    return () => {
      isCancelled = true;
    };
  }, [
    chapterSaveQueue,
    storyId,
    navigate,
    selectedChapterStorageKey,
    isCheckingStoryAccess,
    t,
  ]);

  useEffect(() => {
    // Abort ongoing AI request if chapter changes
    if (activeAiRequestRef.current) {
      activeAiRequestRef.current.abort();
      activeAiRequestRef.current = null;
      setIsAiLoading(false);
      setAiResult(null);
      setIsAiSuggestionModalOpen(false);
    }
  }, [selectedChapterId]);

  const selectedChapter = chapters.find(
    (chapter) => chapter.id === selectedChapterId
  );

  useEffect(() => {
    if (
      !selectedChapter ||
      !hasUnsavedChanges ||
      chapterSaveConflictId === selectedChapter.id ||
      isGeneratingChapterSummary
    ) {
      return;
    }

    const now = getCurrentTimestamp();
    const timeSinceLastAutoSave = now - lastAutoSaveAt.current;
    const shouldSaveImmediately = timeSinceLastAutoSave >= 5000;

    const saveDelay = shouldSaveImmediately ? 0 : 1200;

    const timeoutId = window.setTimeout(async () => {

      const savingStatusTimeout = window.setTimeout(() => {
        setSaveChapterStatus(t("editor.saveStatus.saving"));
      }, 500);
      void chapterSaveQueue.enqueue(selectedChapter.id, {
          title: chapterTitleInput,
          chapterOrder: selectedChapter.chapterOrder,
          type: selectedChapter.type,
          goal: selectedChapter.goal,
          summary: selectedChapter.summary,
          content: chapterEditorContent,
          version: selectedChapter.version,
        })
        .catch(() => undefined)
        .finally(() => window.clearTimeout(savingStatusTimeout));
    }, saveDelay);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [
    chapterEditorContent,
    chapterTitleInput,
    chapterSaveQueue,
    chapterSaveConflictId,
    hasUnsavedChanges,
    isGeneratingChapterSummary,
    selectedChapter,
    t,
  ]);

  async function handleReloadChapterAfterConflict() {
    if (!chapterSaveConflictId) {
      return;
    }

    try {
      const serverChapter = await getChapterById(chapterSaveConflictId);
      chapterSaveQueue.reset();
      setChapters((currentChapters) =>
        currentChapters.map((chapter) =>
          chapter.id === serverChapter.id ? serverChapter : chapter
        )
      );
      setChapterEditorContent(serverChapter.content ?? "");
      setChapterTitleInput(serverChapter.title);
      setChapterSaveConflictId(null);
      setHasUnsavedChanges(false);
      setSaveChapterStatus(t("editor.saveStatus.saved"));
      lastAutoSaveAt.current = getCurrentTimestamp();
    } catch {
      showWorkspaceNotification("error", t("editor.conflict.reloadError"));
    }
  }

  async function handleGenerateChapterTitle() {
    if (!storyId || !selectedChapter || isGeneratingChapterTitle) {
      return;
    }

    const currentText = htmlToPlainText(chapterEditorContent).trim();
    if (!currentText) {
      showWorkspaceNotification("error", t("editor.chapterTitleAi.empty"));
      return;
    }

    try {
      setIsGeneratingChapterTitle(true);
      setSaveChapterStatus(t("editor.saveStatus.saving"));
      await chapterSaveQueue.enqueue(selectedChapter.id, {
        title: chapterTitleInput,
        chapterOrder: selectedChapter.chapterOrder,
        type: selectedChapter.type,
        goal: selectedChapter.goal,
        summary: selectedChapter.summary,
        content: chapterEditorContent,
        version: selectedChapter.version,
      });

      await wrapWithEstimate(() => ({
        chapterId: selectedChapter.id,
        actionType: "GENERATE_CHAPTER_TITLE",
        instruction: "Sugira títulos curtos e específicos para este capítulo.",
        currentText: htmlToPlainText(chapterEditorContent).trim(),
        selectedText: null,
      }), async (payload) => {
        try {
          setIsGeneratingChapterTitle(true);
          const response = await generateAiText(Number(storyId), payload);
          if (response.result.format !== "title-suggestions") {
            throw new Error(t("editor.chapterTitleAi.error"));
          }
          setChapterTitleSuggestions(response.result.options);
        } catch (error: unknown) {
          showWorkspaceNotification(
            "error",
            error instanceof Error ? error.message : t("editor.chapterTitleAi.error")
          );
        } finally {
          setIsGeneratingChapterTitle(false);
          refreshBalance();
        }
      });
    } catch (error: unknown) {
      setSaveChapterStatus(t("editor.saveStatus.error"));
      showWorkspaceNotification(
        "error",
        error instanceof Error ? error.message : t("editor.chapterTitleAi.error")
      );
    } finally {
      setIsGeneratingChapterTitle(false);
    }
  }

  function handleApplyChapterTitle(title: string) {
    setChapterTitleInput(title);
    setHasUnsavedChanges(true);
    setSaveChapterStatus(t("editor.saveStatus.unsaved"));
    setChapterTitleSuggestions(null);
  }

  async function handleCopyLocalChapterAfterConflict() {
    try {
      await navigator.clipboard.writeText(htmlToPlainText(chapterEditorContent));
      showWorkspaceNotification("success", t("editor.conflict.copySuccess"));
    } catch {
      showWorkspaceNotification("error", t("editor.conflict.copyError"));
    }
  }

  async function handleQuickAiAction(
    actionType: AiActionType,
    selectedText: string,
    selectionRange: EditorSelectionRange
  ) {
    if (!selectedText.trim() || quickAiActionInFlightRef.current) {
      return;
    }

    quickAiActionInFlightRef.current = true;
    try {
      setSelectedTextForAi(selectedText);
      setSelectedEditorRange(selectionRange);
      await handleGenerateAiText(actionType, selectedText, selectionRange, true);
    } finally {
      quickAiActionInFlightRef.current = false;
    }
  }

  function toggleFocusMode() {
    setIsFocusMode(!isFocusMode);
    if (!isFocusMode) {
      setIsChapterPanelOpen(false);
      setIsAiPanelOpen(false);
    }
  }

  async function handleGenerateAiText(
    actionType: AiActionType,
    selectedText = "",
    selectionRange: EditorSelectionRange | null = null,
    executeImmediately = false
  ) {
    if (!storyId) {
      return;
    }

    if (!selectedText) {
      setSelectedTextForAi("");
    }

    try {
      if (activeAiRequestRef.current) {
        activeAiRequestRef.current.abort();
      }
      const requestId = crypto.randomUUID();
      const idempotencyKey = crypto.randomUUID();
      currentAiRequestIdRef.current = requestId;

      setAiErrorMessage("");
      setAiResult(null);

      const effectiveSelectionRange = resolveAiSelectionRange(selectionRange, selectedEditorRange);

      if (selectedText && effectiveSelectionRange && selectedChapter) {
        setAiSelectionSnapshot({
          chapterId: selectedChapter.id,
          from: effectiveSelectionRange.from,
          to: effectiveSelectionRange.to,
          selectedText,
          action: actionType,
          requestId,
          idempotencyKey,
          documentVersion: documentVersionRef.current,
          createdAt: getCurrentTimestamp()
        });
      }

      await wrapWithEstimate(() => {
        const abortController = new AbortController();
        activeAiRequestRef.current = abortController;

        return {
          chapterId: selectedChapter?.id ?? null,
          actionType,
          instruction: aiInstructionText,
          currentText: htmlToPlainText(chapterEditorContent),
          selectedText: selectedText || null,
          idempotencyKey,
          abortSignal: abortController.signal,
        };
      }, async (payload) => {
        setIsAiLoading(true);

        try {
          const response = await generateAiText(Number(storyId), payload);

          if (currentAiRequestIdRef.current !== requestId) {
            return; // Request was superseded
          }

          setAiResult(response.result);
          if (response.result.format === "review") {
            setIsAiMobileSheetOpen(false);
            setIsChapterReviewModalOpen(true);
          } else {
            openAiSuggestionModal();
          }
        } catch (e: unknown) {
          const err = e as { name?: string; message?: string };
          if (err.name === "AbortError") {
            return; // Silent cancel
          }
          setAiErrorMessage(getAiErrorMessage(e));
        } finally {
          if (currentAiRequestIdRef.current === requestId) {
            activeAiRequestRef.current = null;
            setIsAiLoading(false);
          }
          refreshBalance();
        }
      }, requestId, executeImmediately);
    } catch (e: unknown) {
      if (e instanceof Error && e.name === "AbortError") {
        return; // Silent cancel
      }
      setAiErrorMessage(e instanceof Error ? e.message : "Não foi possível gerar texto com IA.");
      setIsAiLoading(false);
    }
  }

  function handleRequestReplaceEditorWithAiSuggestion() {
    if (!aiResult || aiResult.format !== "prose") {
      return;
    }

    setIsReplaceEditorConfirmOpen(true);
  }

  function handleReplaceEditorWithAiSuggestion() {
    if (!aiResult || aiResult.format !== "prose") {
      return;
    }

    setChapterEditorContent(
      plainTextToFormattedHtmlParagraphs(aiResult.content, chapterEditorContent)
    );
    setHasUnsavedChanges(true);
    setSaveChapterStatus(t("editor.saveStatus.unsaved"));
    setIsAiSuggestionModalOpen(false);
    setIsReplaceEditorConfirmOpen(false);
    showWorkspaceNotification("success", t("ai.suggestion.editorReplaced"));
  }

  function handleInsertAiSuggestionAtEnd(textToInsert: string) {
    if (!textToInsert || !selectedChapter) {
      return;
    }

    if (aiSelectionSnapshot && aiSelectionSnapshot.chapterId !== selectedChapter.id) {
      showWorkspaceNotification("error", "O trecho foi alterado desde o pedido. A sugestão não foi aplicada automaticamente.");
      return;
    }

    if (aiSelectionSnapshot && documentVersionRef.current !== aiSelectionSnapshot.documentVersion) {
      showWorkspaceNotification("error", "O trecho foi alterado desde o pedido. A sugestão não foi aplicada automaticamente.");
      return;
    }

    setChapterEditorContent((currentContent) => {
      const suggestionHtml = plainTextToFormattedHtmlParagraphs(
        textToInsert,
        currentContent,
        "last"
      );
      return `${currentContent}${suggestionHtml}`;
    });

    setHasUnsavedChanges(true);
    setSaveChapterStatus(t("editor.saveStatus.unsaved"));
    setIsAiSuggestionModalOpen(false);
    showWorkspaceNotification("success", t("ai.suggestion.inserted"));
  }

  if (storyLoadingPresentation.blockContent) {
    return storyLoadingPresentation.showLoading ? (
      <LoadingScreen
        title="Abrindo sua história"
        description="Verificando acesso e preparando o ambiente de escrita."
      />
    ) : null;
  }

  if (storyAccessError) {
    return <div className="app-loading">{storyAccessError}</div>;
  }

  const chapterPanelContent = (
    <div className="panel-content">
      <div className="structure-header" style={{ display: 'flex', gap: '8px' }}>
        <button
          className="new-chapter-button"
          type="button"
          onClick={handleCreateChapter}
          disabled={isCreatingChapter}
          style={{ flex: 1 }}
        >
          {isCreatingChapter ? t("chapters.creating") : t("chapters.newChapter")}
        </button>
        <button
          type="button"
          className="mobile-chapter-drawer-close"
          onClick={() => setIsMobileChapterDrawerOpen(false)}
          aria-label="Fechar capítulos"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: 20, height: 20 }}>
            <path d="M6 18L18 6M6 6l12 12" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>
      <div className="chapter-group">
        <p>{t("chapters.defaultGroup")}</p>

        {isLoadingChapters && (
          <p className="panel-state">{t("chapters.loading")}</p>
        )}

        {chaptersError && (
          <p className="panel-state error">{chaptersError}</p>
        )}

        {!isLoadingChapters &&
          !chaptersError &&
          chapters.map((chapter) => (
            <div
              key={chapter.id}
              className={`chapter-row ${chapter.id === selectedChapterId ? "is-active" : ""
                }`}
            >
              <button
                className="chapter-select-button"
                type="button"
                onClick={() => {
                  setSelectedChapterId(chapter.id);
                  if (selectedChapterStorageKey) {
                    localStorage.setItem(selectedChapterStorageKey, String(chapter.id));
                  }

                  const chapterContent = chapter.content ?? "";

                  setChapterEditorContent(chapterContent);
                  setChapterTitleInput(chapter.title);
                  setHasUnsavedChanges(false);
                  setChapterSaveConflictId(null);
                  chapterSaveQueue.reset();
                  setSaveChapterStatus(t("editor.saveStatus.saved"));
                  lastAutoSaveAt.current = getCurrentTimestamp();
                }}
              >
                <IconFileText />
                <span className="chapter-title-text">{chapter.title}</span>
              </button>

              <button
                className="chapter-delete-button"
                type="button"
                onClick={() => handleRequestDeleteChapter(chapter.id)}
                title={t("chapters.deleteChapter")}
                aria-label={t("chapters.deleteChapter")}
              >
                <IconTrash />
              </button>
            </div>
          ))}
      </div>
    </div>
  );

  return (
    <div className="story-workspace">
      <WorkspaceTopbar
        storyId={storyId ?? ""}
        activeTab={activeTab}
        isFocusMode={isFocusMode}
      />

      {activeTab === "writing" ? (
        <main
          className={`writer-shell 
          ${!isChapterPanelOpen ? "chapters-collapsed" : ""} 
          ${!isAiPanelOpen ? "ai-collapsed" : ""}
          ${isFocusMode ? "is-focus-mode" : ""}`}
        >
          {/* Mobile Backdrop */}
          <div
            className={`workspace-mobile-backdrop ${isMobileChapterDrawerOpen || isAiMobileSheetOpen ? "is-active" : ""}`}
            onClick={() => {
              setIsMobileChapterDrawerOpen(false);
              setIsAiMobileSheetOpen(false);
            }}
          />

          {/* Desktop Chapter Panel */}
          <aside className={`book-structure-panel ${!isChapterPanelOpen ? "is-collapsed" : ""}`}>
            <button
              className="panel-collapse"
              type="button"
              onClick={() => setIsChapterPanelOpen(!isChapterPanelOpen)}
              title={t("chapters.collapse")}
              aria-label={t("chapters.collapse")}
            >
              {isChapterPanelOpen ? <IconChevronLeftDouble /> : <IconChevronRightDouble />}
            </button>

            {isChapterPanelOpen && chapterPanelContent}
          </aside>

          {/* Mobile Chapter Drawer */}
          <div className={`mobile-chapter-drawer ${isMobileChapterDrawerOpen ? "is-open" : ""}`} role="dialog" aria-modal="true">
            {chapterPanelContent}
          </div>

          <section className="writing-center">
            <div className="editor-controls-bar">
              {/* Mobile Chapters Button */}
              {!isFocusMode && (
                <button
                  type="button"
                  className="mobile-chapters-button"
                  onClick={() => {
                    setIsMobileChapterDrawerOpen(true);
                    setIsAiMobileSheetOpen(false);
                  }}
                >
                  <IconFileText />
                  Capítulos
                </button>
              )}

              <button
                className={`focus-mode-button ${isFocusMode ? "active" : ""}`}
                onClick={toggleFocusMode}
                title="Modo Foco"
                aria-label="Modo Foco"
                type="button"
              >
                {isFocusMode ? <IconMinimize /> : <IconFocus />}
              </button>

              <button
                className={`inline-ai-toggle ${isInlineAiAutoEnabled ? "is-active" : ""}`}
                type="button"
                onClick={handleToggleInlineAiAuto}
              >
                {isInlineAiAutoEnabled
                  ? t("editor.inlineAi.autoOn")
                  : t("editor.inlineAi.autoOff")}
              </button>

              <div className="editor-save-group">
                <span className="save-chapter-status">
                  {saveChapterStatus || t("editor.saveStatus.saved")}
                </span>
              </div>
            </div>

            {!isLoadingChapters &&
              selectedChapter &&
              chapterSaveConflictId === selectedChapterId && (
              <div className="chapter-save-conflict" role="alert">
                <div>
                  <strong>{t("editor.conflict.title")}</strong>
                  <span>{t("editor.conflict.message")}</span>
                </div>
                <div className="chapter-save-conflict-actions">
                  <button type="button" onClick={handleCopyLocalChapterAfterConflict}>
                    {t("editor.conflict.copyLocal")}
                  </button>
                  <button type="button" onClick={handleReloadChapterAfterConflict}>
                    {t("editor.conflict.reloadServer")}
                  </button>
                </div>
              </div>
              )}

            <article className="editor-card">
              <div className="editor-scroll-area">
                <div className="editor-page-container">
                  {!isLoadingChapters && selectedChapter ? (
                    <ChapterRichEditor
                    content={chapterEditorContent}
                    replacement={editorReplacement}
                    chapterTitle={chapterTitleInput}
                    chapterTitlePlaceholder={t("editor.chapterTitlePlaceholder")}
                    isGeneratingSummary={isGeneratingChapterSummary}
                    isCreatingNextChapter={isCreatingNextChapter}
                    isGeneratingChapterFromBeats={isGeneratingChapterFromBeats}
                    isGeneratingChapterTitle={isGeneratingChapterTitle}
                    inlineAiSuggestion={inlineAiSuggestion}
                    inlineAiRange={inlineAiRange}
                    isInlineAiLoading={isInlineAiLoading}
                    isInlineAiAutoEnabled={isInlineAiAutoEnabled}
                    onRequestInlineContinuation={handleRequestInlineContinuation}
                    onInlineSuggestionAccepted={handleClearInlineAiSuggestion}
                    onInlineSuggestionDismissed={handleClearInlineAiSuggestion}
                    onOpenChapterBeatsModal={() => setIsChapterBeatsModalOpen(true)}
                    onGenerateChapterSummary={handleGenerateChapterSummary}
                    onCreateNextChapter={handleCreateNextChapter}
                    onGenerateChapterTitle={handleGenerateChapterTitle}
                    onChapterTitleChange={(title) => {
                      if (title === chapterTitleInput) {
                        return;
                      }

                      setChapterTitleInput(title);
                      setHasUnsavedChanges(true);
                      setSaveChapterStatus(t("editor.saveStatus.unsaved"));
                    }}
                    onReplacementApplied={() => setEditorReplacement(null)}
                    onContentChange={(content) => {
                      if (content === chapterEditorContent) {
                        return;
                      }

                      setChapterEditorContent(content);
                      setHasUnsavedChanges(true);
                      setSaveChapterStatus(t("editor.saveStatus.unsaved"));
                      documentVersionRef.current += 1;
                    }}
                    onQuickAiAction={handleQuickAiAction}
                    />
                  ) : (
                    <p className="panel-state">{t("chapters.loading")}</p>
                  )}
                </div>
              </div>
            </article>
          </section>



          <AiAssistantPanel
            key={selectedChapter?.id ?? "no-chapter"}
            isOpen={isAiPanelOpen}
            isLoading={isAiLoading}
            instructionText={aiInstructionText}
            resultText={aiResult?.format === "prose" ? aiResult.content : (aiResult ? "Resultados gerados." : "")}
            errorMessage={aiErrorMessage}
            actions={aiActions}
            onToggleOpen={() => setIsAiPanelOpen((current) => !current)}
            onInstructionChange={(value) => {
              setAiInstructionText(value);
              handleCancelAiGeneration(); // Invalidate estimate
            }}
            onGenerate={handleGenerateAiText}
            onOpenSuggestion={openAiSuggestionModal}
            isMobileSheetOpen={isAiMobileSheetOpen}
            onOpenMobileSheet={() => {
              setIsAiMobileSheetOpen(true);
              setIsMobileChapterDrawerOpen(false);
            }}
            onCloseMobileSheet={() => setIsAiMobileSheetOpen(false)}
            aiEstimate={aiEstimate}
            onConfirmEstimate={handleConfirmAiGeneration}
            storyId={Number(storyId)}
            chapterId={selectedChapter?.id ?? null}
            onOpenHistoryResult={handleOpenHistoryResult}
          />
        </main>
      ) : (
        <main className="story-section-shell">
          {activeTab === "characters" && storyId && (
            <CharacterWorkspace storyId={Number(storyId)} />
          )}

          {activeTab === "world" && storyId && (
            <WorldWorkspace storyId={Number(storyId)} />
          )}

          {activeTab === "notes" && (
            <div className="workspace-placeholder">
              <h2>{t("tabs.notes")}</h2>
              <p>Área de notas será construída depois.</p>
            </div>
          )}

          {activeTab === "references" && (
            <div className="workspace-placeholder">
              <h2>{t("tabs.references")}</h2>
              <p>Área de referências será construída depois.</p>
            </div>
          )}
        </main>
      )}

      {isChapterBeatsModalOpen && (
        <div className="workspace-dialog-backdrop">
          <div className="workspace-dialog chapter-beats-dialog">
            <h2>{t("editor.chapterFromBeats.title")}</h2>
            <p className="chapter-beats-description">
              {t("editor.chapterFromBeats.description")}
            </p>

            <div className="chapter-beats-mode">
              <label>
                <input
                  type="radio"
                  checked={chapterBeatsMode === "FLEXIBLE"}
                  onChange={() => setChapterBeatsMode("FLEXIBLE")}
                />
                {t("editor.chapterFromBeats.flexible")}
              </label>

              <label>
                <input
                  type="radio"
                  checked={chapterBeatsMode === "STRICT"}
                  onChange={() => setChapterBeatsMode("STRICT")}
                />
                {t("editor.chapterFromBeats.strict")}
              </label>
            </div>

            <div className="workspace-dialog-actions">
              <button
                className="workspace-dialog-secondary"
                type="button"
                onClick={() => setIsChapterBeatsModalOpen(false)}
              >
                {t("chapters.deleteCancel")}
              </button>

              <button
                className="workspace-dialog-primary"
                type="button"
                onClick={handleGenerateChapterFromBeats}
                disabled={isGeneratingChapterFromBeats}
              >
                {isGeneratingChapterFromBeats
                  ? t("editor.chapterFromBeats.generating")
                  : t("editor.chapterFromBeats.generate")}
              </button>


            </div>
          </div>
        </div>
      )}

      {isAiSuggestionModalOpen && aiResult && aiResult.format !== "title-suggestions" && aiResult.format !== "review" && (
        <AiSuggestionModal
          result={aiResult}
          canReplaceSelection={Boolean(selectedTextForAi)}
          onClose={() => setIsAiSuggestionModalOpen(false)}
          onCopy={handleCopyAiSuggestion}
          onInsertAtEnd={handleInsertAiSuggestionAtEnd}
          onReplaceSelection={handleReplaceSelectedTextWithAiSuggestion}
          onReplaceEditor={handleRequestReplaceEditorWithAiSuggestion}
          isFirstDraft={aiSelectionSnapshot?.action === "WRITE_CHAPTER_FROM_BEATS"}
        />
      )}

      {chapterTitleSuggestions && (
        <ChapterTitleSuggestionModal
          options={chapterTitleSuggestions}
          onApply={handleApplyChapterTitle}
          onClose={() => setChapterTitleSuggestions(null)}
        />
      )}

      {isChapterReviewModalOpen && aiResult?.format === "review" && (
        <ChapterReviewModal
          review={aiResult}
          onCopy={handleCopyAiSuggestion}
          onClose={() => setIsChapterReviewModalOpen(false)}
        />
      )}

      {workspaceNotification && (
        <div className={`workspace-toast ${workspaceNotification.type}`}>
          {workspaceNotification.message}
        </div>
      )}

      {chapterIdPendingDelete && (
        <div className="workspace-dialog-backdrop">
          <div className="workspace-dialog">
            <h2>{t("chapters.deleteTitle")}</h2>
            <p>{t("chapters.deleteMessage")}</p>

            <div className="workspace-dialog-actions">
              <button
                className="workspace-dialog-secondary"
                type="button"
                onClick={() => setChapterIdPendingDelete(null)}
              >
                {t("chapters.deleteCancel")}
              </button>

              <button
                className="workspace-dialog-danger"
                type="button"
                onClick={handleConfirmDeleteChapter}
              >
                {t("chapters.deleteConfirmAction")}
              </button>

            </div>
          </div>
        </div>
      )}

      {isReplaceEditorConfirmOpen && (
        <div className="workspace-dialog-backdrop workspace-dialog-backdrop-top">
          <div className="workspace-dialog">
            <h2>{t("ai.suggestion.replaceEditorConfirmTitle")}</h2>
            <p>{t("ai.suggestion.replaceEditorConfirmMessage")}</p>

            <div className="workspace-dialog-actions">
              <button
                className="workspace-dialog-secondary"
                type="button"
                onClick={() => setIsReplaceEditorConfirmOpen(false)}
              >
                {t("chapters.deleteCancel")}
              </button>

              <button
                className="workspace-dialog-danger"
                type="button"
                onClick={handleReplaceEditorWithAiSuggestion}
              >
                {t("ai.suggestion.replaceEditorConfirmAction")}
              </button>
            </div>
          </div>
        </div>
      )}

      <AiConfirmModal
        isOpen={!!aiEstimate}
        estimate={aiEstimate}
        onConfirm={handleConfirmAiGeneration}
        onCancel={handleCancelAiGeneration}
        isLoading={isAiLoading || isGeneratingChapterFromBeats || isCreatingNextChapter || isInlineAiLoading}
      />
    </div>
  );
}
