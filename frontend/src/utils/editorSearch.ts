import type { Node as ProseMirrorNode } from "@tiptap/pm/model";

export type EditorTextMatch = {
  from: number;
  to: number;
};

type SearchableDocument = {
  text: string;
  positions: Array<number | null>;
};

function buildSearchableDocument(doc: ProseMirrorNode): SearchableDocument {
  let text = "";
  const positions: Array<number | null> = [];
  let hasTextBlock = false;

  doc.descendants((node, position) => {
    if (node.isTextblock) {
      if (hasTextBlock) {
        text += "\n";
        positions.push(null);
      }
      hasTextBlock = true;
    }

    if (node.isText && node.text) {
      for (let index = 0; index < node.text.length; index += 1) {
        text += node.text[index];
        positions.push(position + index);
      }
    } else if (node.type.name === "hardBreak") {
      text += "\n";
      positions.push(position);
    }
  });

  return { text, positions };
}

export function findEditorTextMatches(
  doc: ProseMirrorNode,
  searchText: string
): EditorTextMatch[] {
  if (!searchText) {
    return [];
  }

  const searchable = buildSearchableDocument(doc);
  const normalizedText = searchable.text.toLocaleLowerCase();
  const normalizedSearch = searchText.toLocaleLowerCase();
  const matches: EditorTextMatch[] = [];
  let searchFrom = 0;

  while (searchFrom <= normalizedText.length - normalizedSearch.length) {
    const foundIndex = normalizedText.indexOf(normalizedSearch, searchFrom);
    if (foundIndex === -1) {
      break;
    }

    const matchedPositions = searchable.positions
      .slice(foundIndex, foundIndex + searchText.length)
      .filter((position): position is number => position !== null);

    if (matchedPositions.length > 0) {
      matches.push({
        from: matchedPositions[0],
        to: matchedPositions[matchedPositions.length - 1] + 1,
      });
    }

    searchFrom = foundIndex + Math.max(normalizedSearch.length, 1);
  }

  return matches;
}
