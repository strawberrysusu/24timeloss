import { useEffect, useState } from "react";

import {
  deleteArticle,
  generateArticleSummary,
  getArticleDetail,
  getRelatedArticles,
} from "../../shared/api/articles";
import { isAuthenticationError } from "../../shared/api/http";
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
  const [isArticleLoading, setIsArticleLoading] = useState(true);
  const [articleErrorMessage, setArticleErrorMessage] = useState("");
  const [isRelatedLoading, setIsRelatedLoading] = useState(true);
  const [relatedErrorMessage, setRelatedErrorMessage] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsArticleLoading(true);
      setArticleErrorMessage("");
      setIsRelatedLoading(true);
      setRelatedErrorMessage("");

      const [articleResult, relatedResult] = await Promise.allSettled([
        getArticleDetail(articleId),
        getRelatedArticles(articleId),
      ]);

      if (cancelled) {
        return;
      }

      if (articleResult.status === "fulfilled") {
        setArticle(articleResult.value);
      } else {
        setArticle(null);
        setArticleErrorMessage(
          articleResult.reason instanceof Error
            ? articleResult.reason.message
            : "기사를 불러오지 못했습니다.",
        );
      }
      setIsArticleLoading(false);

      if (relatedResult.status === "fulfilled") {
        setRelatedArticles(relatedResult.value);
      } else {
        setRelatedArticles([]);
        setRelatedErrorMessage(
          relatedResult.reason instanceof Error
            ? relatedResult.reason.message
            : "관련 뉴스를 불러오지 못했습니다.",
        );
      }
      setIsRelatedLoading(false);
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
      if (isAuthenticationError(error)) {
        onAuthenticationRequired();
        return;
      }
      setArticleErrorMessage(error instanceof Error ? error.message : "AI 요약 생성에 실패했습니다.");
    }
  }

  async function removeArticle() {
    try {
      await deleteArticle(articleId);
      onDeleted();
    } catch (error) {
      if (isAuthenticationError(error)) {
        onAuthenticationRequired();
        return;
      }
      setArticleErrorMessage(error instanceof Error ? error.message : "기사 삭제에 실패했습니다.");
    }
  }

  return {
    article,
    relatedArticles,
    isArticleLoading,
    articleErrorMessage,
    isRelatedLoading,
    relatedErrorMessage,
    refresh: () => setRefreshKey((previous) => previous + 1),
    regenerateSummary,
    removeArticle,
  };
}
