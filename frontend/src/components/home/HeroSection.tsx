interface HeroSectionProps {
  keyword: string;
  trendingKeywords: string[];
  onKeywordChange: (value: string) => void;
  onSearch: () => void;
  onKeywordClick: (keyword: string) => void;
}

export function HeroSection({
  keyword,
  trendingKeywords,
  onKeywordChange,
  onSearch,
  onKeywordClick,
}: HeroSectionProps) {
  return (
    <section className="hero">
      <div className="hero-inner">
        <span className="hero-badge">
          <span className="hero-badge-dot" />
          AI 뉴스 요약 서비스
        </span>
        <h1 className="hero-title">
          오늘의 뉴스,
          <br />
          <em>3줄</em>로 끝내세요
        </h1>
        <p className="hero-desc">
          AI가 복잡한 기사를 핵심만 추려드립니다.
          <br />
          관심 분야를 설정하고 맞춤 뉴스를 받아보세요.
        </p>

        <div className="search-wrap">
          <label className="search-label">
            <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <circle cx="11" cy="11" r="8" />
              <path d="m21 21-4.35-4.35" />
            </svg>
            <input
              className="search-input"
              type="text"
              value={keyword}
              placeholder="뉴스 키워드 검색..."
              onChange={(event) => onKeywordChange(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  onSearch();
                }
              }}
            />
          </label>
          <button className="search-btn" onClick={onSearch}>
            검색
          </button>
        </div>

        <div className="trending-keywords">
          <span className="label">인기 검색어:</span>
          {trendingKeywords.map((item) => (
            <button
              key={item}
              className="trending-keyword"
              onClick={() => onKeywordClick(item)}
            >
              {item}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
