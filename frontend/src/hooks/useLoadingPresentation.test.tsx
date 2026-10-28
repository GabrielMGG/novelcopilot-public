// @vitest-environment jsdom

import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useLoadingPresentation } from "./useLoadingPresentation";

function LoadingHarness({ loading }: { loading: boolean }) {
  const presentation = useLoadingPresentation(loading, 200, 300);

  if (presentation.blockContent) {
    return presentation.showLoading ? <p>Carregando</p> : null;
  }

  return <p>Conteúdo</p>;
}

describe("useLoadingPresentation", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("nao mostra carregamento quando a operacao termina rapidamente", () => {
    vi.useFakeTimers();
    const view = render(<LoadingHarness loading />);

    act(() => vi.advanceTimersByTime(150));
    expect(screen.queryByText("Carregando")).not.toBeInTheDocument();

    view.rerender(<LoadingHarness loading={false} />);
    expect(screen.getByText("Conteúdo")).toBeInTheDocument();
  });

  it("mantem a tela pelo tempo minimo depois que ela aparece", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-08-01T18:00:00Z"));
    const view = render(<LoadingHarness loading />);

    act(() => {
      vi.advanceTimersByTime(200);
      vi.setSystemTime(new Date("2026-08-01T18:00:00.200Z"));
    });
    expect(screen.getByText("Carregando")).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(100);
      vi.setSystemTime(new Date("2026-08-01T18:00:00.300Z"));
    });
    view.rerender(<LoadingHarness loading={false} />);
    expect(screen.getByText("Carregando")).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(200));
    expect(screen.getByText("Conteúdo")).toBeInTheDocument();
  });

  it("cancela os temporizadores quando desmontado", () => {
    vi.useFakeTimers();
    const view = render(<LoadingHarness loading />);

    view.unmount();
    act(() => vi.runAllTimers());

    expect(vi.getTimerCount()).toBe(0);
  });
});
