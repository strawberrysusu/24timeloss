package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 읽은 기사 기록을 조회/저장하는 Repository다.
 */
public interface ArticleReadHistoryRepository extends JpaRepository<ArticleReadHistory, Long> {

    // 회원이 지금까지 읽은 기사 수를 빠르게 집계할 때 사용한다.
    long countByMemberId(Long memberId);

    // 같은 기사를 중복 기록하지 않기 위한 체크
    boolean existsByMemberIdAndArticleId(Long memberId, Long articleId);

    // 읽은 기사 목록을 최신순으로 조회한다.
    List<ArticleReadHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
