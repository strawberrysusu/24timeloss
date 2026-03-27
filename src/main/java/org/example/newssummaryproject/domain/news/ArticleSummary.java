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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.newssummaryproject.domain.common.BaseTimeEntity;

import java.time.LocalDateTime;

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

    // ── 요약 출처 정보 (포트폴리오용으로 중요!) ──

    // 이 요약이 어떻게 만들어졌는지 구분한다 (AI_GENERATED / SEED)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SummarySource summarySource;

    // AI로 생성한 경우, 어떤 모델을 사용했는지 기록한다 (예: "meta/llama-3.3-70b-instruct")
    @Column(length = 100)
    private String modelName;

    // AI가 요약을 생성한 시각을 기록한다
    private LocalDateTime generatedAt;

    @Builder
    private ArticleSummary(Article article, String summaryLine1, String summaryLine2, String summaryLine3,
                           String keyPoint1, String keyPoint2, String keyPoint3,
                           SummarySource summarySource, String modelName, LocalDateTime generatedAt) {
        this.article = article;
        this.summaryLine1 = summaryLine1;
        this.summaryLine2 = summaryLine2;
        this.summaryLine3 = summaryLine3;
        this.keyPoint1 = keyPoint1;
        this.keyPoint2 = keyPoint2;
        this.keyPoint3 = keyPoint3;
        this.summarySource = summarySource;
        this.modelName = modelName;
        this.generatedAt = generatedAt;
    }

    /**
     * 요약 내용을 수정한다.
     * 출처 정보도 함께 업데이트한다.
     */
    public void update(String summaryLine1, String summaryLine2, String summaryLine3,
                       String keyPoint1, String keyPoint2, String keyPoint3,
                       SummarySource summarySource, String modelName, LocalDateTime generatedAt) {
        if (summaryLine1 != null) this.summaryLine1 = summaryLine1;
        if (summaryLine2 != null) this.summaryLine2 = summaryLine2;
        if (summaryLine3 != null) this.summaryLine3 = summaryLine3;
        if (keyPoint1 != null) this.keyPoint1 = keyPoint1;
        if (keyPoint2 != null) this.keyPoint2 = keyPoint2;
        if (keyPoint3 != null) this.keyPoint3 = keyPoint3;
        if (summarySource != null) this.summarySource = summarySource;
        if (modelName != null) this.modelName = modelName;
        if (generatedAt != null) this.generatedAt = generatedAt;
    }
}
