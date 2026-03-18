package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
