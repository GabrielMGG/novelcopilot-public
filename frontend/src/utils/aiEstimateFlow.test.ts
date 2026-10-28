import { describe, expect, it, vi } from "vitest";
import type { GenerateAiPayload } from "../types/ai";
import type { AiEstimateResponse } from "../types/credits";
import { executeEstimatedAiAction } from "./aiEstimateFlow";

const payload: GenerateAiPayload = {
  chapterId: 50,
  actionType: "REWRITE_SELECTION",
  instruction: "",
  currentText: "O capítulo completo.",
  selectedText: "trecho selecionado",
};

function estimate(canGenerate: boolean): AiEstimateResponse {
  return {
    action: "REWRITE_SELECTION",
    estimatedCredits: 0.1,
    estimatedInputTokens: 100,
    maxOutputTokens: 64,
    contextTruncated: false,
    profile: "SELECTION_EDIT",
    canGenerate,
    availableCredits: canGenerate ? 10 : 0,
  };
}

describe("executeEstimatedAiAction", () => {
  it("executa uma única vez quando a estimativa autoriza a ação rápida", async () => {
    const executeCall = vi.fn().mockResolvedValue(undefined);

    await expect(
      executeEstimatedAiAction(estimate(true), payload, executeCall)
    ).resolves.toBe(true);
    expect(executeCall).toHaveBeenCalledTimes(1);
    expect(executeCall).toHaveBeenCalledWith(payload);
  });

  it("não executa nem cobra quando a estimativa recusa a ação", async () => {
    const executeCall = vi.fn().mockResolvedValue(undefined);

    await expect(
      executeEstimatedAiAction(estimate(false), payload, executeCall)
    ).resolves.toBe(false);
    expect(executeCall).not.toHaveBeenCalled();
  });
});
