-- V2: OAuth2 소셜 로그인 지원
-- provider: 가입 경로 (LOCAL, GOOGLE)
-- provider_id: 소셜 서비스의 고유 사용자 ID

ALTER TABLE members ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE members ADD COLUMN provider_id VARCHAR(100) NULL;
