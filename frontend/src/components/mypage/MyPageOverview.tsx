import type { MyPageData } from "../../shared/types/member";

interface MyPageOverviewProps {
  data: MyPageData;
}

export function MyPageOverview({ data }: MyPageOverviewProps) {
  return (
    <div className="stats-grid" id="section-feed">
      <div className="stat-card">
        <p className="stat-value">{data.readArticleCount}</p>
        <p className="stat-label">읽은 기사 수</p>
      </div>
      <div className="stat-card">
        <p className="stat-value">{data.savedArticles.length}</p>
        <p className="stat-label">저장한 기사</p>
      </div>
      <div className="stat-card-dark">
        <p className="stat-value-dark">{data.streak}일</p>
        <p className="stat-label-dark">연속 방문</p>
      </div>
    </div>
  );
}
