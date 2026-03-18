package org.example.newssummaryproject.domain.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.newssummaryproject.domain.common.BaseTimeEntity;

import java.time.LocalDateTime;

/**
 * 원본 뉴스 기사 자체를 저장하는 엔티티다.
 *
 * 홈 화면 뉴스 카드, 상세 페이지, 저장 기능의 기준 데이터가 된다.
 * AI 요약은 이 Article을 기준으로 1:1 연결된다.
 */
@Getter
@Entity
@Table(name = "articles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기사 분류값. 홈 화면 카테고리 탭과 연결된다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    // 기사 제목
    @Column(nullable = false, length = 300)
    private String title;

    // 같은 기사를 중복 저장하지 않기 위해 원문 URL을 unique로 둔다.
    @Column(nullable = false, unique = true, length = 500)
    private String originalUrl;

    // 언론사 이름
    @Column(length = 100)
    private String source;

    // 카드/상세 페이지에서 쓸 대표 이미지 주소
    @Column(length = 500)
    private String thumbnailUrl;

    // 원문 본문 전체 또는 수집한 기사 텍스트
    @Column(columnDefinition = "TEXT")
    private String content;

    // 실제 기사 발행 시각
    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Builder
    private Article(Category category, String title, String originalUrl, String source, String thumbnailUrl,
                    String content, LocalDateTime publishedAt) {
        this.category = category;
        this.title = title;
        this.originalUrl = originalUrl;
        this.source = source;
        this.thumbnailUrl = thumbnailUrl;
        this.content = content;
        this.publishedAt = publishedAt;
    }
}
