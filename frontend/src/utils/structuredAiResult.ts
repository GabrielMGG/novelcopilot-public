export function normalizeStructuredAiResult(value: unknown): Record<string, unknown> | null {
  if (typeof value === "string") {
    try {
      return normalizeStructuredAiResult(JSON.parse(value));
    } catch {
      return null;
    }
  }

  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return null;
  }

  return value as Record<string, unknown>;
}

export function readStructuredAiResult(source: Record<string, unknown>): Record<string, unknown> | null {
  return normalizeStructuredAiResult(
    source.structuredResult
      ?? source.structured_result
      ?? source.structuredOutput
      ?? source.structured_output
  );
}
