import { describe, expect, it } from "vitest";
import { countEditorText } from "./editorTextStats";

describe("countEditorText", () => {
  it("returns zero for empty or whitespace-only text", () => {
    expect(countEditorText("")).toEqual({ words: 0, characters: 0 });
    expect(countEditorText("   \n")).toEqual({ words: 0, characters: 3 });
  });

  it("counts accented words, numbers and punctuation without counting markup", () => {
    expect(countEditorText("Olá, mundo! Capítulo 2."))
      .toEqual({ words: 4, characters: 23 });
  });

  it("keeps apostrophes and hyphens inside a word", () => {
    expect(countEditorText("d'água guarda-chuva l’écriture"))
      .toEqual({ words: 3, characters: 30 });
  });

  it("does not count paragraph separators as characters", () => {
    expect(countEditorText("Primeiro parágrafo.\nSegundo parágrafo."))
      .toEqual({ words: 4, characters: 37 });
  });
});
