export function htmlToPlainText(html: string) {
  const parser = new DOMParser();
  const document = parser.parseFromString(html, "text/html");

  return document.body.textContent?.trim() ?? "";
}