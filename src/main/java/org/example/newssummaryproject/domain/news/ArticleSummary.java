package org.example.newssummaryproject.domain.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.newssummaryproject.domain.common.BaseTimeEntity;

/**
 * 기사에 대한 AI 요약 결과를 저장하는 엔티티다.
 *
 * 현재 기획 기준으로 "3줄 요약 + 핵심 포인트 3개"를 저장하도록 만들었다.
 * 기사 1개당 요약 1개가 붙는 구조라서 Article과 1:1 관계다.
 */
@Getter
@Entity
@Table(name = "article_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @OneToOne: 기사 1개당 요약 1개. Article과 1:1 관계.
    // fetch = LAZY: 요약을 조회할 때 기사를 즉시 로딩하지 않는다 (성능 최적화).
    // optional = false: 요약은 반드시 기사와 연결되어야 한다 (기사 없는 요약은 불가).
    // unique = true: 하나의 기사에 두 개 이상의 요약이 생기는 것을 DB 레벨에서 방지한다.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false, unique = true)
    private Article article;

    // 상세 화면 상단에 보여줄 3줄 요약
    @Column(nullable = false, length = 500)
    private String summaryLine1;

    @Column(nullable = false, length = 500)
    private String summaryLine2;

    @Column(nullable = false, length = 500)
    private String summaryLine3;

    // 사용자가 빠르게 파악할 수 있도록 핵심 포인트를 따로 저장한다.
    @Column(nullable = false, length = 500)
    private String keyPoint1;

    @Column(nullable = false, length = 500)
    private String keyPoint2;

    @Column(nullable = false, length = 500)
    private String keyPoint3;

    @Builder
    private ArticleSummary(Article article, String summaryLine1, String summaryLine2, String summaryLine3,
                           String keyPoint1, String keyPoint2, String keyPoint3) {
        this.article = article;
        this.summaryLine1 = summaryLine1;
        this.summaryLine2 = summaryLine2;
        this.summaryLine3 = summaryLine3;
        this.keyPoint1 = keyPoint1;
        this.keyPoint2 = keyPoint2;
        this.keyPoint3 = keyPoint3;
    }

    /**
     * 요약 내용을 수정한다.
     */
    public void update(String summaryLine1, String summaryLine2, String summaryLine3,
                       String keyPoint1, String keyPoint2, String keyPoint3) {
        if (summaryLine1 != null) this.summaryLine1 = summaryLine1;
        if (summaryLine2 != null) this.summaryLine2 = summaryLine2;
        if (summaryLine3 != null) this.summaryLine3 = summaryLine3;
        if (keyPoint1 != null) this.keyPoint1 = keyPoint1;
        if (keyPoint2 != null) this.keyPoint2 = keyPoint2;
        if (keyPoint3 != null) this.keyPoint3 = keyPoint3;
    }
}
