import { CATEGORY_LABELS, CATEGORY_OPTIONS } from "../../shared/constants/categories";
import type { Category } from "../../shared/constants/categories";
import type { ArticleCardData } from "../../shared/types/article";

interface HomeSidebarProps {
  briefingText: string;
  briefingTimeLabel: string;
  errorMessage: string;
  trendingArticles: ArticleCardData[];
  activeInterests: Category[];
  onArticleClick: (articleId: number) => void;
  onInterestToggle: (category: Category) => void;
}

export function HomeSidebar({
  briefingText,
  briefingTimeLabel,
  errorMessage,
  trendingArticles,
  activeInterests,
  onArticleClick,
  onInterestToggle,
}: HomeSidebarProps) {
  const interestCategories = CATEGORY_OPTIONS.flatMap((category) =>
    category.value === null ? [] : [category],
  );

  return (
    <aside className="sidebar">
      <div className="sidebar-card-dark">
        <div className="sidebar-title-dark">
          <span className="sidebar-badge-dark">✦ 오늘의 뉴스 브리핑</span>
          <span className="sidebar-time">{briefingTimeLabel}</span>
        </div>
        <p className="briefing-text">{briefingText}</p>
        {errorMessage ? <p className="loading-msg" style={{ padding: "12px 0 0", color: "rgba(255, 255, 255, 0.82)" }}>{errorMessage}</p> : null}
      </div>

      <div className="sidebar-card">
        <h3 className="sidebar-title">📈 실시간 트렌딩</h3>
        <ol className="trending-list" id="trending-list">
          {trendingArticles.length === 0 ? (
            <li className="trending-item">
              <span className="trending-text" style={{ color: "var(--ash)" }}>
                트렌딩 뉴스가 없습니다.
              </span>
            </li>
          ) : null}
          {trendingArticles.map((article, index) => {
            const badges = ["🔥", "↑", "", "NEW", ""];
            const badgeClasses = ["trending-hot", "trending-up", "", "trending-new", ""];
            const badge = badges[index] ?? "";
            const badgeClass = badgeClasses[index] ?? "";

            return (
              <li
                key={article.id}
                className="trending-item clickable"
                onClick={() => onArticleClick(article.id)}
              >
                <span className="trending-num">{String(index + 1).padStart(2, "0")}</span>
                <span className="trending-text">
                  {article.title.length > 20 ? `${article.title.slice(0, 20)}…` : article.title}
                </span>
                {badge ? <span className={`trending-badge ${badgeClass}`}>{badge}</span> : null}
              </li>
            );
          })}
        </ol>
      </div>

      <div className="sidebar-card">
        <h3 className="sidebar-title">관심 분야 설정</h3>
        <div className="interest-tags">
          {interestCategories.map((category) => (
            <button
              key={category.value}
              className={`interest-btn ${activeInterests.includes(category.value) ? "on" : "off"}`}
              onClick={() => onInterestToggle(category.value)}
            >
              {CATEGORY_LABELS[category.value]}
            </button>
          ))}
        </div>
      </div>
    </aside>
  );
}
