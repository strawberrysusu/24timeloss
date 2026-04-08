-- V1: 초기 스키마 생성
-- 기존 ddl-auto=update로 만들어진 테이블 구조를 그대로 SQL로 옮긴 것이다.

CREATE TABLE IF NOT EXISTS members (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(200) NOT NULL,
    nickname   VARCHAR(30)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS articles (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    category        VARCHAR(30)  NOT NULL,
    title           VARCHAR(300) NOT NULL,
    original_url    VARCHAR(500),
    source          VARCHAR(100),
    thumbnail_url   VARCHAR(500),
    has_video       BIT          NOT NULL DEFAULT 0,
    video_embed_url VARCHAR(500),
    content         TEXT,
    published_at    DATETIME(6)  NOT NULL,
    view_count      INT          NOT NULL DEFAULT 0,
    writer_id       BIGINT,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_articles_writer FOREIGN KEY (writer_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS article_summaries (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    article_id     BIGINT       NOT NULL,
    summary_line1  VARCHAR(500) NOT NULL,
    summary_line2  VARCHAR(500) NOT NULL,
    summary_line3  VARCHAR(500) NOT NULL,
    key_point1     VARCHAR(500) NOT NULL,
    key_point2     VARCHAR(500) NOT NULL,
    key_point3     VARCHAR(500) NOT NULL,
    summary_source VARCHAR(20),
    model_name     VARCHAR(100),
    generated_at   DATETIME(6),
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_summaries_article (article_id),
    CONSTRAINT fk_article_summaries_article FOREIGN KEY (article_id) REFERENCES articles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS search_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    keyword     VARCHAR(200) NOT NULL,
    searched_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS saved_articles (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    article_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saved_article_member_article (member_id, article_id),
    CONSTRAINT fk_saved_articles_member  FOREIGN KEY (member_id)  REFERENCES members (id),
    CONSTRAINT fk_saved_articles_article FOREIGN KEY (article_id) REFERENCES articles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS member_interests (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    category   VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_interest_member_category (member_id, category),
    CONSTRAINT fk_member_interests_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS article_read_histories (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    article_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_article_read_histories_member  FOREIGN KEY (member_id)  REFERENCES members (id),
    CONSTRAINT fk_article_read_histories_article FOREIGN KEY (article_id) REFERENCES articles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
