import { useEffect, useState } from "react";

import {
  getMyPage,
  getReadHistory,
  removeInterest,
  unsaveArticle,
  updateNickname,
  updatePassword,
  addInterest,
} from "../../shared/api/members";
import type { Category } from "../../shared/constants/categories";
import type { ReadHistoryItem } from "../../shared/types/article";
import type { CurrentUser, MyPageData } from "../../shared/types/member";

interface UseMyPageDataOptions {
  enabled: boolean;
  onAuthenticationRequired: () => void;
  onUserUpdated: (user: CurrentUser) => void;
  onSavedArticlesChanged: () => Promise<void>;
}

export function useMyPageData({
  enabled,
  onAuthenticationRequired,
  onUserUpdated,
  onSavedArticlesChanged,
}: UseMyPageDataOptions) {
  const [data, setData] = useState<MyPageData | null>(null);
  const [history, setHistory] = useState<ReadHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (!enabled) {
      setData(null);
      setHistory([]);
      setLoading(false);
      return;
    }

    let cancelled = false;

    async function load() {
      setLoading(true);
      setErrorMessage("");

      try {
        const [myPage, readHistory] = await Promise.all([getMyPage(), getReadHistory()]);
        if (!cancelled) {
          setData(myPage);
          setHistory(readHistory);
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error instanceof Error ? error.message : "마이페이지를 불러오지 못했습니다.");
          setData(null);
          setHistory([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [enabled]);

  async function refresh() {
    try {
      const [myPage, readHistory] = await Promise.all([getMyPage(), getReadHistory()]);
      setData(myPage);
      setHistory(readHistory);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "데이터를 다시 불러오지 못했습니다.");
    }
  }

  async function toggleInterest(category: Category) {
    if (!data) {
      return;
    }

    const isActive = data.interests.includes(category);

    try {
      if (isActive) {
        await removeInterest(category);
        setData({ ...data, interests: data.interests.filter((item) => item !== category) });
      } else {
        await addInterest(category);
        setData({ ...data, interests: [...data.interests, category] });
      }
    } catch (error) {
      if (error instanceof Error && error.message.includes("로그인")) {
        onAuthenticationRequired();
        return;
      }
      await refresh();
    }
  }

  async function removeSavedArticle(articleId: number) {
    try {
      await unsaveArticle(articleId);
      await refresh();
      await onSavedArticlesChanged();
    } catch (error) {
      if (error instanceof Error && error.message.includes("로그인")) {
        onAuthenticationRequired();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "저장 기사를 삭제하지 못했습니다.");
    }
  }

  async function changeNickname(nickname: string) {
    try {
      const user = await updateNickname(nickname);
      onUserUpdated(user);
      await refresh();
    } catch (error) {
      if (error instanceof Error && error.message.includes("로그인")) {
        onAuthenticationRequired();
        return;
      }
      throw error;
    }
  }

  async function changePassword(currentPassword: string, newPassword: string) {
    try {
      await updatePassword(currentPassword, newPassword);
    } catch (error) {
      if (error instanceof Error && error.message.includes("로그인")) {
        onAuthenticationRequired();
        return;
      }
      throw error;
    }
  }

  return {
    data,
    history,
    loading,
    errorMessage,
    toggleInterest,
    removeSavedArticle,
    changeNickname,
    changePassword,
    refresh,
  };
}
