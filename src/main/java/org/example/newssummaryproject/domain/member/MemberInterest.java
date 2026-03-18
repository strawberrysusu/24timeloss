package org.example.newssummaryproject.domain.member;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.newssummaryproject.domain.common.BaseTimeEntity;
import org.example.newssummaryproject.domain.news.Category;

/**
 * 회원이 관심 분야로 선택한 카테고리를 저장한다.
 *
 * 마이페이지에서 토글로 켜고 끄는 관심 분야 설정을 DB로 표현한 구조다.
 * 한 회원이 같은 카테고리를 중복 등록하지 못하도록 unique 제약을 둔다.
 */
@Getter
@Entity
@Table(
        name = "member_interests",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_interest_member_category", columnNames = {"member_id", "category"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberInterest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관심 분야를 설정한 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 관심 카테고리 값
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Builder
    private MemberInterest(Member member, Category category) {
        this.member = member;
        this.category = category;
    }
}
