import { formatDate } from "../../shared/utils/format";
import { CATEGORY_LABELS } from "../../shared/constants/categories";
import type { ArticleCardData } from "../../shared/types/article";

interface SavedArticlesSectionProps {
  articles: ArticleCardData[];
  onArticleClick: (articleId: number) => void;
  onRemove: (articleId: number) => void;
}

export function SavedArticlesSection({
  articles,
  onArticleClick,
  onRemove,
}: SavedArticlesSectionProps) {
  return (
    <section className="settings-card" id="section-saved">
      <div className="settings-card-header">
        <h3 className="settings-card-title">최근 저장된 뉴스</h3>
      </div>
      <div className="saved-grid">
        {articles.length === 0 ? <p className="loading-msg">저장한 기사가 없습니다.</p> : null}
        {articles.map((article) => (
          <article
            key={article.id}
            className="saved-card"
            onClick={() => onArticleClick(article.id)}
          >
            <span className="cat-badge">{CATEGORY_LABELS[article.category]}</span>
            <p className="saved-card-title">{article.title}</p>
            <div className="saved-card-footer">
              <span className="saved-card-date">{formatDate(article.publishedAt)}</span>
            <button
              type="button"
              className="saved-delete-btn"
              onClick={(event) => {
                event.stopPropagation();
                onRemove(article.id);
              }}
            >
              삭제
            </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
