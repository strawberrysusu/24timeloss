import { useEffect, useState } from "react";

import {
  getArticles,
  getBriefing,
  getPopularKeywords,
  getTrendingArticles,
  searchArticleList,
} from "../../shared/api/articles";
import { CATEGORY_OPTIONS, type Category } from "../../shared/constants/categories";
import type { ArticleCardData } from "../../shared/types/article";

function formatBriefingTime(dateTime: string) {
  const date = new Date(dateTime);
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes} 기준`;
}

export function useHomePageData(
  savedArticleIds: Set<number>,
  onToggleSave: (articleId: number) => Promise<void>,
  resetKey: number,
) {
  const [keyword, setKeyword] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);
  const [articles, setArticles] = useState<ArticleCardData[]>([]);
  const [trendingKeywords, setTrendingKeywords] = useState<string[]>([]);
  const [trendingArticles, setTrendingArticles] = useState<ArticleCardData[]>([]);
  const [briefingText, setBriefingText] = useState("브리핑을 불러오는 중...");
  const [briefingTimeLabel, setBriefingTimeLabel] = useState("--:-- 기준");
  const [currentPage, setCurrentPage] = useState(0);
  const [isLastPage, setIsLastPage] = useState(true);
  const [searchMode, setSearchMode] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");

  useEffect(() => {
    void loadHomeSidebarData();
  }, []);

  useEffect(() => {
    void loadArticles(0, selectedCategory);
  }, [selectedCategory]);

  useEffect(() => {
    setKeyword("");
    setSelectedCategory(null);
    setSearchMode(false);
    setSearchKeyword("");
    void loadArticles(0, null);
  }, [resetKey]);

  async function loadHomeSidebarData() {
    try {
      const [keywords, trending, briefing] = await Promise.all([
        getPopularKeywords(),
        getTrendingArticles(),
        getBriefing(),
      ]);

      setTrendingKeywords(keywords);
      setTrendingArticles(trending);
      setBriefingText(briefing.text);
      setBriefingTimeLabel(formatBriefingTime(briefing.generatedAt));
    } catch {
      setBriefingText("브리핑을 불러오지 못했습니다.");
      setTrendingKeywords([]);
      setTrendingArticles([]);
    }
  }

  async function loadArticles(page: number, category: Category | null) {
    try {
      const response = await getArticles({ page, category });
      setCurrentPage(page);
      setIsLastPage(response.last);
      setArticles(response.content);
    } catch {
      setArticles([]);
      setIsLastPage(true);
    }
  }

  async function searchArticles() {
    const trimmed = keyword.trim();

    if (!trimmed) {
      setSearchMode(false);
      setSearchKeyword("");
      await loadArticles(0, selectedCategory);
      return;
    }

    try {
      const response = await searchArticleList({ keyword: trimmed, page: 0 });
      setCurrentPage(0);
      setIsLastPage(response.last);
      setSearchMode(true);
      setSearchKeyword(trimmed);
      setArticles(response.content);
    } catch {
      setArticles([]);
      setIsLastPage(true);
    }
  }

  async function loadMore() {
    const nextPage = currentPage + 1;

    try {
      const response = searchMode
        ? await searchArticleList({ keyword: searchKeyword, page: nextPage })
        : await getArticles({ page: nextPage, category: selectedCategory });

      setCurrentPage(nextPage);
      setIsLastPage(response.last);
      setArticles((previous) => [...previous, ...response.content]);
    } catch {
      setIsLastPage(true);
    }
  }

  async function applyKeyword(nextKeyword: string) {
    setKeyword(nextKeyword);

    try {
      const response = await searchArticleList({ keyword: nextKeyword, page: 0 });
      setCurrentPage(0);
      setIsLastPage(response.last);
      setSearchMode(true);
      setSearchKeyword(nextKeyword);
      setArticles(response.content);
    } catch {
      setArticles([]);
      setIsLastPage(true);
    }
  }

  function selectCategory(category: Category | null) {
    setKeyword("");
    setSelectedCategory(category);
  }

  return {
    keyword,
    setKeyword,
    selectedCategory,
    categories: CATEGORY_OPTIONS,
    articles,
    trendingKeywords,
    trendingArticles,
    briefingText,
    briefingTimeLabel,
    savedArticleIds,
    isLastPage,
    searchArticles,
    applyKeyword,
    selectCategory,
    toggleSave: onToggleSave,
    loadMore,
  };
}
