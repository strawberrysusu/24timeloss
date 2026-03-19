package org.example.newssummaryproject.domain.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.newssummaryproject.domain.common.BaseTimeEntity;
import org.example.newssummaryproject.domain.member.Member;

import java.time.LocalDateTime;

/**
 * 원본 뉴스 기사를 저장하는 엔티티다.
 *
 * 이 프로젝트에서 가장 중심이 되는 테이블이다.
 * 홈 화면 뉴스 카드, 상세 페이지, 저장/검색/추천 기능이 모두 이 엔티티를 기준으로 동작한다.
 *
 * 연결 관계:
 *   articles (1) ──── (1) article_summaries    → AI 요약 (ArticleSummary)
 *   articles (1) ──── (N) saved_articles        → 저장한 사용자들
 *   articles (1) ──── (N) article_read_histories → 읽은 사용자들
 *   articles (N) ──── (1) members (writer_id)   → 작성자
 *
 * 어노테이션 설명은 Member.java 참고 (@Entity, @Table, @Getter, @NoArgsConstructor 등).
 */
@Getter
@Entity
@Table(name = "articles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Enumerated(STRING): Java의 enum(Category)을 DB에 문자열로 저장한다.
    // 예: Category.IT_SCIENCE → DB에 "IT_SCIENCE" 문자열로 저장.
    // ORDINAL(숫자)보다 STRING이 안전하다 — enum 순서가 바뀌어도 DB 데이터가 깨지지 않는다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false, length = 300)
    private String title;

    // 원문 기사 URL. nullable(기본값) — 사용자가 직접 등록한 기사는 원문이 없을 수 있다.
    @Column(length = 500)
    private String originalUrl;

    @Column(length = 100)
    private String source;

    @Column(length = 500)
    private String thumbnailUrl;

    // columnDefinition = "TEXT": 일반 VARCHAR보다 훨씬 긴 텍스트를 저장할 수 있다.
    // 기사 본문은 수천 자가 될 수 있으므로 TEXT 타입을 사용한다.
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    // 조회수 — 기사 상세 페이지에 들어올 때마다 +1. 트렌딩 정렬에 사용된다.
    @Column(nullable = false)
    private int viewCount = 0;

    // @ManyToOne: 여러 기사(Article)가 한 명의 작성자(Member)에게 연결된다.
    // fetch = LAZY: 기사를 조회할 때 작성자 정보를 즉시 가져오지 않고,
    //               writer.getNickname() 등 실제로 접근할 때 비로소 DB를 조회한다.
    //               → 불필요한 JOIN을 줄여 성능을 높인다.
    // @JoinColumn(name = "writer_id"): DB의 외래키 컬럼 이름을 "writer_id"로 지정한다.
    // null이면 시스템이 자동 등록한 기사다 (DataInitializer 등에서 생성).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private Member writer;

    @Builder
    private Article(Category category, String title, String originalUrl, String source, String thumbnailUrl,
                    String content, LocalDateTime publishedAt, Member writer) {
        this.category = category;
        this.title = title;
        this.originalUrl = originalUrl;
        this.source = source;
        this.thumbnailUrl = thumbnailUrl;
        this.content = content;
        this.publishedAt = publishedAt;
        this.writer = writer;
    }

    /**
     * 기사 내용을 수정한다.
     * null → 안 건드림, 빈 문자열("") → 값 삭제, 값 있음 → 덮어쓰기.
     */
    public void update(Category category, String title, String content, String source, String originalUrl) {
        if (category != null) this.category = category;
        if (title != null && !title.isBlank()) this.title = title;
        if (content != null && !content.isBlank()) this.content = content;
        if (source != null) this.source = source.isBlank() ? null : source;
        if (originalUrl != null) this.originalUrl = originalUrl.isBlank() ? null : originalUrl;
    }

    /**
     * 조회수를 1 올린다.
     * 기사 상세 페이지에 들어올 때마다 호출된다.
     * JPA의 dirty checking이 자동으로 UPDATE 쿼리를 날려준다.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }
}
