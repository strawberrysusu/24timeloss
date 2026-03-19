package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 읽은 기사 기록을 조회/저장하는 Repository다.
 */
public interface ArticleReadHistoryRepository extends JpaRepository<ArticleReadHistory, Long> {

    // 전체 기록 수 (중복 포함 — 같은 기사를 다른 날에 읽으면 각각 카운트)
    long countByMemberId(Long memberId);

    // 읽은 "고유 기사" 수 (같은 기사를 여러 날 읽어도 1로 센다)
    @Query("SELECT COUNT(DISTINCT h.article.id) FROM ArticleReadHistory h WHERE h.member.id = :memberId")
    long countDistinctArticlesByMemberId(@Param("memberId") Long memberId);

    // 같은 기사를 같은 날에 중복 기록하지 않기 위한 체크
    @Query("SELECT COUNT(h) > 0 FROM ArticleReadHistory h " +
            "WHERE h.member.id = :memberId AND h.article.id = :articleId " +
            "AND h.createdAt >= :startOfDay AND h.createdAt < :endOfDay")
    boolean existsByMemberIdAndArticleIdAndDate(
            @Param("memberId") Long memberId,
            @Param("articleId") Long articleId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    List<ArticleReadHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 기사 삭제 시 관련 읽기 기록도 함께 삭제
    void deleteByArticleId(Long articleId);
}
