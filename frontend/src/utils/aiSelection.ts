import type { EditorSelectionRange } from "../components/workspace/ChapterRichEditor";

export function resolveAiSelectionRange(
  requestedRange: EditorSelectionRange | null,
  storedRange: EditorSelectionRange | null
): EditorSelectionRange | null {
  return requestedRange ?? storedRange;
}
