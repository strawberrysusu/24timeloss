-- OAuth 로그인 성공 후 access token을 URL query로 노출시키는 대신,
-- 짧은 수명(1분)의 one-time code를 발급하고 프론트가 별도 API로 교환받도록 한다.
--
-- 기존 흐름의 위험: GET /?token=eyJhbGc...
--   → 서버 access log, 프록시 log, 브라우저 history, referrer 헤더에 노출 가능
--
-- 새 흐름:
--   1. OAuth 성공 → 서버가 랜덤 code 발급 + 해시 DB 저장
--   2. /?oauth_code=xxx 로 리다이렉트
--   3. 프론트가 POST /api/members/oauth/exchange { code }
--   4. 서버가 code 검증 + redeem(1회용) 후 access token + refresh cookie 발급
--
-- 보안:
--   - code 자체는 저장 안 함, SHA-256 해시만 저장 (DB 유출 시에도 활성 code 사용 불가)
--   - redeemed_at으로 1회용 보장 (이미 사용된 code 거부)
--   - expires_at으로 짧은 수명 강제 (60초)

CREATE TABLE oauth_exchange_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    code_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    redeemed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_oauth_codes_member FOREIGN KEY (member_id)
        REFERENCES members (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_oauth_codes_expires ON oauth_exchange_codes (expires_at);
