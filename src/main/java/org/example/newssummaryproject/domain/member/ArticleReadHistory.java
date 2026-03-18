package org.example.newssummaryproject.domain.member;

import jakarta.persistence.Entity;
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
import org.example.newssummaryproject.domain.news.Article;

/**
 * 회원이 읽은 기사 기록을 남기는 엔티티다.
 *
 * 마이페이지의 "읽은 기사 수", 연속 방문 같은 통계를 만들 때 기초 데이터로 사용한다.
 * 지금은 단순 구조만 만들었고, 이후 상세 페이지 조회 시 저장하도록 연결하면 된다.
 */
@Getter
@Entity
@Table(name = "article_read_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleReadHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 누가 읽었는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 어떤 기사를 읽었는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Builder
    private ArticleReadHistory(Member member, Article article) {
        this.member = member;
        this.article = article;
    }
}
