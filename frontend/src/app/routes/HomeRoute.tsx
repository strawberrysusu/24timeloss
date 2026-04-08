import { useNavigate } from "react-router-dom";

import { useHomePageData } from "../../features/home/useHomePageData";
import { HomePage } from "../../pages/HomePage";
import type { Category } from "../../shared/constants/categories";

interface HomeRouteProps {
  resetKey: number;
  activeInterests: Category[];
  savedArticleIds: Set<number>;
  onInterestToggle: (category: Category) => void;
  onToggleSave: (articleId: number) => Promise<void>;
}

export function HomeRoute({
  resetKey,
  activeInterests,
  savedArticleIds,
  onInterestToggle,
  onToggleSave,
}: HomeRouteProps) {
  const navigate = useNavigate();
  const home = useHomePageData(savedArticleIds, onToggleSave, resetKey);

  return (
    <HomePage
      keyword={home.keyword}
      selectedCategory={home.selectedCategory}
      categories={home.categories}
      trendingKeywords={home.trendingKeywords}
      articles={home.articles}
      isLastPage={home.isLastPage}
      briefingText={home.briefingText}
      briefingTimeLabel={home.briefingTimeLabel}
      trendingArticles={home.trendingArticles}
      activeInterests={activeInterests}
      savedArticleIds={home.savedArticleIds}
      onKeywordChange={home.setKeyword}
      onSearch={home.searchArticles}
      onKeywordClick={home.applyKeyword}
      onCategorySelect={home.selectCategory}
      onArticleClick={(articleId) => navigate(`/detail/${articleId}`)}
      onToggleSave={home.toggleSave}
      onLoadMore={home.loadMore}
      onInterestToggle={onInterestToggle}
    />
  );
}
