import { formatDateTime } from "../../shared/utils/format";
import { CATEGORY_LABELS } from "../../shared/constants/categories";
import type { ReadHistoryItem } from "../../shared/types/article";

interface ReadHistorySectionProps {
  history: ReadHistoryItem[];
  onArticleClick: (articleId: number) => void;
}

export function ReadHistorySection({
  history,
  onArticleClick,
}: ReadHistorySectionProps) {
  return (
    <section className="settings-card" id="section-history">
      <div className="settings-card-header">
        <h3 className="settings-card-title">읽기 기록</h3>
      </div>
      <div className="saved-grid">
        {history.length === 0 ? <p className="loading-msg">읽은 기사가 없습니다.</p> : null}
        {history.map((item) => (
          <article
            key={item.articleId}
            className="saved-card"
            onClick={() => onArticleClick(item.articleId)}
          >
            <span className="cat-badge">{CATEGORY_LABELS[item.category]}</span>
            <p className="saved-card-title">{item.title}</p>
            <div className="saved-card-footer">
              <span className="saved-card-date">{formatDateTime(item.readAt)} 읽음</span>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
