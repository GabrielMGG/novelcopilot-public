type ReferenceEdge = "first" | "last";

function findFirstTextNode(node: Node): Text | null {
  if (node.nodeType === Node.TEXT_NODE) {
    return node as Text;
  }

  for (const child of Array.from(node.childNodes)) {
    const textNode = findFirstTextNode(child);
    if (textNode) {
      return textNode;
    }
  }

  return null;
}

function cloneInlineFormatting(
  referenceParagraph: HTMLParagraphElement,
  text: string
): HTMLParagraphElement {
  const paragraph = referenceParagraph.cloneNode(false) as HTMLParagraphElement;
  const referenceText = findFirstTextNode(referenceParagraph);

  if (!referenceText) {
    paragraph.textContent = text;
    return paragraph;
  }

  const wrappers: HTMLElement[] = [];
  let current = referenceText.parentElement;

  while (current && current !== referenceParagraph) {
    wrappers.push(current);
    current = current.parentElement;
  }

  let content: Node = document.createTextNode(text);
  for (const wrapper of wrappers) {
    const clone = wrapper.cloneNode(false) as HTMLElement;
    clone.append(content);
    content = clone;
  }

  paragraph.append(content);
  return paragraph;
}

export function plainTextToFormattedHtmlParagraphs(
  text: string,
  referenceHtml = "",
  referenceEdge: ReferenceEdge = "first"
): string {
  const paragraphs = text
    .split(/\n+/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);
  const referenceContainer = document.createElement("div");
  referenceContainer.innerHTML = referenceHtml;
  const referenceParagraphs = Array.from(
    referenceContainer.querySelectorAll("p")
  );
  const referenceParagraph =
    referenceEdge === "last"
      ? referenceParagraphs.at(-1)
      : referenceParagraphs[0];
  const output = document.createElement("div");

  for (const paragraphText of paragraphs) {
    const paragraph = referenceParagraph
      ? cloneInlineFormatting(referenceParagraph, paragraphText)
      : document.createElement("p");

    if (!referenceParagraph) {
      paragraph.textContent = paragraphText;
    }
    output.append(paragraph);
  }

  return output.innerHTML;
}
