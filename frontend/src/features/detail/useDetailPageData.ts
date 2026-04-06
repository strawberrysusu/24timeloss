import { useEffect, useState } from "react";

import {
  deleteArticle,
  generateArticleSummary,
  getArticleDetail,
  getRelatedArticles,
} from "../../shared/api/articles";
import { recordReadHistory } from "../../shared/api/members";
import type { ArticleCardData, ArticleDetailData } from "../../shared/types/article";

interface UseDetailPageDataOptions {
  articleId: number;
  currentUserId: number | null;
  onDeleted: () => void;
  onAuthenticationRequired: () => void;
}

export function useDetailPageData({
  articleId,
  currentUserId,
  onDeleted,
  onAuthenticationRequired,
}: UseDetailPageDataOptions) {
  const [article, setArticle] = useState<ArticleDetailData | null>(null);
  const [relatedArticles, setRelatedArticles] = useState<ArticleCardData[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setErrorMessage("");

      try {
        const [articleResult, relatedResult] = await Promise.all([
          getArticleDetail(articleId),
          getRelatedArticles(articleId),
        ]);

        if (!cancelled) {
          setArticle(articleResult);
          setRelatedArticles(relatedResult);
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error instanceof Error ? error.message : "기사를 불러오지 못했습니다.");
          setArticle(null);
          setRelatedArticles([]);
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
  }, [articleId, refreshKey]);

  useEffect(() => {
    if (!article || !currentUserId) {
      return;
    }

    void recordReadHistory(article.id).catch(() => undefined);
  }, [article, currentUserId]);

  async function regenerateSummary() {
    try {
      const nextArticle = await generateArticleSummary(articleId);
      setArticle(nextArticle);
    } catch (error) {
      if (error instanceof Error && error.message.includes("로그인")) {
        onAuthenticationRequired();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "AI 요약 생성에 실패했습니다.");
    }
  }

  async function removeArticle() {
    try {
      await deleteArticle(articleId);
      onDeleted();
    } catch (error) {
      if (error instanceof Error && error.message.includes("로그인")) {
        onAuthenticationRequired();
        return;
      }
      setErrorMessage(error instanceof Error ? error.message : "기사 삭제에 실패했습니다.");
    }
  }

  return {
    article,
    relatedArticles,
    loading,
    errorMessage,
    refresh: () => setRefreshKey((previous) => previous + 1),
    regenerateSummary,
    removeArticle,
  };
}
