import type { GenerateAiPayload } from "../types/ai";
import type { AiActionType } from "../types/ai";
import type { AiEstimateResponse } from "../types/credits";

export const QUICK_AI_ACTION_TYPES = [
  "REWRITE_SELECTION",
  "IMPROVE_DIALOGUE",
  "ADD_TENSION",
] as const satisfies readonly AiActionType[];

export type QuickAiAction = (typeof QUICK_AI_ACTION_TYPES)[number];

export async function executeEstimatedAiAction(
  estimate: AiEstimateResponse,
  payload: GenerateAiPayload,
  executeCall: (payload: GenerateAiPayload) => Promise<void>
) {
  if (!estimate.canGenerate) {
    return false;
  }

  await executeCall(payload);
  return true;
}
