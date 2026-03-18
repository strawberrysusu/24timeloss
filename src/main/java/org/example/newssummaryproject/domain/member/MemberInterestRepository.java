package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import org.example.newssummaryproject.domain.news.Category;

import java.util.List;
import java.util.Optional;

/**
 * 회원 관심 카테고리 설정을 다루는 Repository다.
 */
public interface MemberInterestRepository extends JpaRepository<MemberInterest, Long> {

    // 특정 회원이 고른 관심 분야를 한 번에 조회한다.
    List<MemberInterest> findByMemberId(Long memberId);

    // 이미 등록한 관심 분야인지 확인
    boolean existsByMemberIdAndCategory(Long memberId, Category category);

    // 관심 분야 삭제 시 해당 기록을 찾기 위해 사용
    Optional<MemberInterest> findByMemberIdAndCategory(Long memberId, Category category);
}
