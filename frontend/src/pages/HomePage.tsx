import { CategoryTabs } from "../components/home/CategoryTabs";
import { HeroSection } from "../components/home/HeroSection";
import { HomeSidebar } from "../components/home/HomeSidebar";
import { NewsFeedSection } from "../components/home/NewsFeedSection";
import type { Category } from "../shared/constants/categories";
import type { ArticleCardData } from "../shared/types/article";

interface HomePageProps {
  keyword: string;
  selectedCategory: Category | null;
  categories: readonly { label: string; value: Category | null }[];
  trendingKeywords: string[];
  articles: ArticleCardData[];
  isLastPage: boolean;
  briefingText: string;
  briefingTimeLabel: string;
  trendingArticles: ArticleCardData[];
  activeInterests: Category[];
  savedArticleIds: Set<number>;
  onKeywordChange: (value: string) => void;
  onSearch: () => void;
  onKeywordClick: (keyword: string) => void;
  onCategorySelect: (category: Category | null) => void;
  onArticleClick: (articleId: number) => void;
  onToggleSave: (articleId: number) => void;
  onLoadMore: () => void;
  onInterestToggle: (category: Category) => void;
}

export function HomePage(props: HomePageProps) {
  return (
    <div>
      <HeroSection
        keyword={props.keyword}
        trendingKeywords={props.trendingKeywords}
        onKeywordChange={props.onKeywordChange}
        onSearch={props.onSearch}
        onKeywordClick={props.onKeywordClick}
      />
      <CategoryTabs
        categories={props.categories}
        selectedCategory={props.selectedCategory}
        onSelect={props.onCategorySelect}
      />
      <div className="container">
        <div className="main-grid">
          <NewsFeedSection
            articles={props.articles}
            isLastPage={props.isLastPage}
            savedArticleIds={props.savedArticleIds}
            onArticleClick={props.onArticleClick}
            onToggleSave={props.onToggleSave}
            onLoadMore={props.onLoadMore}
          />
          <HomeSidebar
            briefingText={props.briefingText}
            briefingTimeLabel={props.briefingTimeLabel}
            trendingArticles={props.trendingArticles}
            activeInterests={props.activeInterests}
            onArticleClick={props.onArticleClick}
            onInterestToggle={props.onInterestToggle}
          />
        </div>
      </div>
    </div>
  );
}
