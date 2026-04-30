-- 자동수집 + 직접 등록 기사의 original_url 중복을 DB 레벨에서 막는다.
-- V5에서 보류했던 UNIQUE 제약을 추가한다 — 동일 URL이 동시에 두 번 INSERT되는 race condition 차단 목적.
-- MySQL은 NULL을 여러 행에 허용하므로 original_url 없는 직접 등록 기사는 영향받지 않는다.
-- 기존 중복 데이터가 남아 있으면 ALTER가 실패하므로 적용 전에 수동으로 dedup이 필요할 수 있다.

ALTER TABLE articles DROP INDEX idx_articles_original_url;

CREATE UNIQUE INDEX uk_articles_original_url
    ON articles (original_url);
