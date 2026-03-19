package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 저장 기사 목록을 조회/저장하는 Repository다.
 */
public interface SavedArticleRepository extends JpaRepository<SavedArticle, Long> {

    // 마이페이지에서 최근 저장한 기사 목록을 보여줄 때 사용할 수 있다.
    List<SavedArticle> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 이미 저장한 기사인지 확인
    boolean existsByMemberIdAndArticleId(Long memberId, Long articleId);

    // 저장 취소 시 해당 기록을 찾기 위해 사용
    Optional<SavedArticle> findByMemberIdAndArticleId(Long memberId, Long articleId);

    // 회원의 저장 기사 수
    long countByMemberId(Long memberId);

    // 회원이 저장한 기사 ID 목록만 빠르게 조회 (프론트에서 저장 버튼 상태 표시용)
    @Query("SELECT sa.article.id FROM SavedArticle sa WHERE sa.member.id = :memberId")
    List<Long> findArticleIdsByMemberId(@Param("memberId") Long memberId);

    // 기사 삭제 시 관련 저장 기록도 함께 삭제
    void deleteByArticleId(Long articleId);
}
