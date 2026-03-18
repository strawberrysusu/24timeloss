package org.example.newssummaryproject.domain.member;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.newssummaryproject.domain.common.BaseTimeEntity;
import org.example.newssummaryproject.domain.news.Article;

/**
 * 회원이 저장 버튼을 눌러 보관한 기사 목록을 나타낸다.
 *
 * Member : Article = N : N 관계를 중간 테이블 형태로 풀어낸 엔티티다.
 * 같은 회원이 같은 기사를 두 번 저장하지 못하게 unique 제약을 걸었다.
 */
@Getter
@Entity
@Table(
        name = "saved_articles",
        uniqueConstraints = @UniqueConstraint(name = "uk_saved_article_member_article", columnNames = {"member_id", "article_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedArticle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 누가 저장했는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 어떤 기사를 저장했는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Builder
    private SavedArticle(Member member, Article article) {
        this.member = member;
        this.article = article;
    }
}
