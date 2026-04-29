-- Refresh Token DB 저장소.
-- 보안:
--   token 자체는 저장하지 않고 SHA-256 해시(64자 hex)만 저장한다.
--   → DB가 유출돼도 활성 토큰을 직접 탈취할 수 없다.
--
-- 동작:
--   - 로그인/가입/OAuth 성공 시 새 행 INSERT
--   - /refresh 시 해시 조회 후 revoked_at 채우고 새 행 INSERT
--   - 로그아웃 시 해당 행 revoked_at 채움
--   - 이미 revoked인 토큰이 다시 들어오면 → 해당 회원 모든 토큰 일괄 폐기 (탈취 의심)

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_refresh_tokens_member FOREIGN KEY (member_id)
        REFERENCES members (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 회원별 토큰 일괄 폐기/조회용
CREATE INDEX idx_refresh_tokens_member ON refresh_tokens (member_id);

-- 만료된 토큰 정리(필요 시)용
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
