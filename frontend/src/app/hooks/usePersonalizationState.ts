import { useEffect, useState } from "react";

import {
  addInterest,
  getMyPage,
  listSavedArticleIds,
  removeInterest,
  saveArticle,
  unsaveArticle,
} from "../../shared/api/members";
import type { Category } from "../../shared/constants/categories";
import type { CurrentUser } from "../../shared/types/member";

export function usePersonalizationState(currentUser: CurrentUser | null) {
  const [activeInterests, setActiveInterests] = useState<Category[]>([]);
  const [savedArticleIds, setSavedArticleIds] = useState<Set<number>>(new Set());

  function clearPersonalization() {
    setActiveInterests([]);
    setSavedArticleIds(new Set());
  }

  async function refreshMyPageState() {
    if (!currentUser) {
      clearPersonalization();
      return;
    }

    const [myPageResult, savedIdsResult] = await Promise.allSettled([
      getMyPage(),
      listSavedArticleIds(),
    ]);

    if (myPageResult.status === "fulfilled") {
      setActiveInterests(myPageResult.value.interests);
    } else {
      setActiveInterests([]);
    }

    if (savedIdsResult.status === "fulfilled") {
      setSavedArticleIds(new Set(savedIdsResult.value));
    } else {
      setSavedArticleIds(new Set());
    }
  }

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      if (!currentUser) {
        clearPersonalization();
        return;
      }

      const [myPageResult, savedIdsResult] = await Promise.allSettled([
        getMyPage(),
        listSavedArticleIds(),
      ]);

      if (cancelled) {
        return;
      }

      if (myPageResult.status === "fulfilled") {
        setActiveInterests(myPageResult.value.interests);
      } else {
        setActiveInterests([]);
      }

      if (savedIdsResult.status === "fulfilled") {
        setSavedArticleIds(new Set(savedIdsResult.value));
      } else {
        setSavedArticleIds(new Set());
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, [currentUser]);

  async function toggleSave(articleId: number) {
    const isSaved = savedArticleIds.has(articleId);

    try {
      if (isSaved) {
        await unsaveArticle(articleId);
        setSavedArticleIds((previous) => {
          const next = new Set(previous);
          next.delete(articleId);
          return next;
        });
      } else {
        await saveArticle(articleId);
        setSavedArticleIds((previous) => {
          const next = new Set(previous);
          next.add(articleId);
          return next;
        });
      }
    } catch {
      await refreshMyPageState();
    }
  }

  async function toggleInterest(category: Category) {
    const isActive = activeInterests.includes(category);

    try {
      if (isActive) {
        await removeInterest(category);
        setActiveInterests((previous) => previous.filter((item) => item !== category));
      } else {
        await addInterest(category);
        setActiveInterests((previous) => [...previous, category]);
      }
    } catch {
      await refreshMyPageState();
    }
  }

  return {
    activeInterests,
    savedArticleIds,
    refreshMyPageState,
    clearPersonalization,
    toggleSave,
    toggleInterest,
  };
}
