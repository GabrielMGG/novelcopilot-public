import { useEffect, useRef, useState } from "react";

type LoadingPresentation = {
  blockContent: boolean;
  showLoading: boolean;
};

export function useLoadingPresentation(
  isLoading: boolean,
  delayMs = 200,
  minimumVisibleMs = 300,
): LoadingPresentation {
  const [showLoading, setShowLoading] = useState(false);
  const visibleAtRef = useRef<number | null>(null);

  useEffect(() => {
    let timer: number | undefined;

    if (isLoading) {
      if (visibleAtRef.current === null) {
        timer = window.setTimeout(() => {
          visibleAtRef.current = Date.now();
          setShowLoading(true);
        }, delayMs);
      }
    } else if (visibleAtRef.current !== null) {
      const remainingMs = Math.max(0, minimumVisibleMs - (Date.now() - visibleAtRef.current));
      timer = window.setTimeout(() => {
        visibleAtRef.current = null;
        setShowLoading(false);
      }, remainingMs);
    }

    return () => {
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [delayMs, isLoading, minimumVisibleMs]);

  return {
    blockContent: isLoading || showLoading,
    showLoading,
  };
}
