-- V3/V4에서 BaseTimeEntity의 updated_at 컬럼을 빠뜨려서 Hibernate schema 검증 실패.
-- (refresh_tokens / oauth_exchange_codes 엔티티는 BaseTimeEntity 상속 → created_at + updated_at 둘 다 가짐)
-- 이미 적용된 V3, V4를 수정하면 Flyway checksum 미스매치가 발생하므로 ALTER TABLE로 보강한다.

ALTER TABLE refresh_tokens
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE oauth_exchange_codes
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
