import { useState, useCallback } from "react";
import {
  createStoryCoverSignedUrls,
  createStoryCoverSignedUrl,
} from "../../../services/storage/storyCoverStorage";
import type { ApiStory } from "../../../types/story";

export function useStoryCovers() {
  const [coverUrlMap, setCoverUrlMap] = useState<Map<string, string>>(new Map());
  const [isLoadingCovers, setIsLoadingCovers] = useState(false);

  const getCoverDisplayUrl = useCallback(
    (path: string | null | undefined): string | null => {
      if (!path) return null;
      return coverUrlMap.get(path) || null;
    },
    [coverUrlMap]
  );

  const loadStoryCovers = useCallback(async (stories: ApiStory[]) => {
    setIsLoadingCovers(true);
    try {
      const paths = stories
        .map((s) => s.coverImagePath)
        .filter((p): p is string => p !== null && p !== undefined && p !== "");

      if (paths.length === 0) return;

      const newUrlsMap = await createStoryCoverSignedUrls(paths);

      setCoverUrlMap((currentMap) => {
        const nextMap = new Map(currentMap);
        newUrlsMap.forEach((url, path) => {
          nextMap.set(path, url);
        });
        return nextMap;
      });
    } catch (error) {
      console.error("Failed to load story covers", error);
    } finally {
      setIsLoadingCovers(false);
    }
  }, []);

  const refreshStoryCover = useCallback(async (path: string) => {
    try {
      const signedUrl = await createStoryCoverSignedUrl(path);
      setCoverUrlMap((currentMap) => {
        const nextMap = new Map(currentMap);
        nextMap.set(path, signedUrl);
        return nextMap;
      });
      return signedUrl;
    } catch (error) {
      console.error("Failed to refresh story cover", error);
      return null;
    }
  }, []);

  const removeStoryCover = useCallback((path: string) => {
    setCoverUrlMap((currentMap) => {
      const nextMap = new Map(currentMap);
      nextMap.delete(path);
      return nextMap;
    });
  }, []);

  return {
    coverUrlMap,
    getCoverDisplayUrl,
    loadStoryCovers,
    refreshStoryCover,
    removeStoryCover,
    isLoadingCovers,
  };
}
