export type EditorTextStats = {
  words: number;
  characters: number;
};

const WORD_PATTERN = /[\p{L}\p{N}]+(?:['’-][\p{L}\p{N}]+)*/gu;

export function countEditorText(text: string): EditorTextStats {
  const words = text.match(WORD_PATTERN)?.length ?? 0;
  const characters = Array.from(text.replace(/[\r\n]/g, "")).length;

  return { words, characters };
}
