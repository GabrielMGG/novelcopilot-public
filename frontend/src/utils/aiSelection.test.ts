import { describe, expect, it } from "vitest";
import { resolveAiSelectionRange } from "./aiSelection";

describe("resolveAiSelectionRange", () => {
  it("uses the range delivered by the quick action instead of stale React state", () => {
    const requestedRange = { from: 12, to: 38 };
    const staleStoredRange = { from: 2, to: 8 };

    expect(resolveAiSelectionRange(requestedRange, staleStoredRange)).toBe(requestedRange);
  });

  it("keeps the stored range for generation started from the assistant panel", () => {
    const storedRange = { from: 4, to: 19 };

    expect(resolveAiSelectionRange(null, storedRange)).toBe(storedRange);
  });
});
