/**
 * @vitest-environment jsdom
 */
import { describe, expect, it } from "vitest";
import { plainTextToFormattedHtmlParagraphs } from "./editorFormatting";

describe("plainTextToFormattedHtmlParagraphs", () => {
  it("preserva a formatação do primeiro parágrafo ao substituir o rascunho", () => {
    const result = plainTextToFormattedHtmlParagraphs(
      "Primeiro parágrafo.\nSegundo parágrafo.",
      '<p style="text-align: center"><span style="font-family: Lora, serif; font-size: 20px"><strong>Original.</strong></span></p>'
    );

    expect(result).toBe(
      '<p style="text-align: center"><span style="font-family: Lora, serif; font-size: 20px"><strong>Primeiro parágrafo.</strong></span></p>' +
        '<p style="text-align: center"><span style="font-family: Lora, serif; font-size: 20px"><strong>Segundo parágrafo.</strong></span></p>'
    );
  });

  it("usa o último parágrafo como referência ao inserir no final", () => {
    const result = plainTextToFormattedHtmlParagraphs(
      "Continuação.",
      '<p><span style="font-size: 16px">Início.</span></p><p style="text-align: right"><em><span style="font-size: 22px">Fim.</span></em></p>',
      "last"
    );

    expect(result).toBe(
      '<p style="text-align: right"><em><span style="font-size: 22px">Continuação.</span></em></p>'
    );
  });

  it("trata a resposta como texto, não como HTML executável", () => {
    const result = plainTextToFormattedHtmlParagraphs(
      '<img src=x onerror="alert(1)">'
    );

    expect(result).toBe(
      '<p>&lt;img src=x onerror="alert(1)"&gt;</p>'
    );
  });
});
