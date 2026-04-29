-- 자주 쓰이는 조회 쿼리에 맞춘 인덱스 추가.
-- 데이터가 쌓일수록 효과가 커진다 (현재는 미미하지만 retention 켜면 누적되므로 미리 깔아둠).

-- articles.original_url: 자동수집 시 중복 체크 SELECT 빈번. 일반 인덱스로 가속.
-- (UNIQUE 제약은 기존 데이터의 중복 가능성 때문에 보류 — 현재는 Java 레벨 dedup으로 충분)
CREATE INDEX idx_articles_original_url ON articles (original_url);

-- 카테고리별 최신순 (홈, 카테고리 페이지)
CREATE INDEX idx_articles_category_published
    ON articles (category, published_at DESC);

-- 트렌딩 (조회수 ↓, 같으면 최신순)
CREATE INDEX idx_articles_view_published
    ON articles (view_count DESC, published_at DESC);

-- 검색 로그: 인기 검색어 집계용
CREATE INDEX idx_search_logs_keyword_searched
    ON search_logs (searched_at, keyword);

-- 마이페이지 — 회원별 최근 읽은/저장 기사 조회
CREATE INDEX idx_read_histories_member_created
    ON article_read_histories (member_id, created_at DESC);

CREATE INDEX idx_saved_articles_member_created
    ON saved_articles (member_id, created_at DESC);

-- Retention 정리: writer IS NULL AND created_at < cutoff 조회
CREATE INDEX idx_articles_writer_created
    ON articles (writer_id, created_at);
