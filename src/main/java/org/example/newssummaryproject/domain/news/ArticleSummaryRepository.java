package org.example.newssummaryproject.domain.news;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * AI 요약 데이터를 저장하는 Repository다.
 */
public interface ArticleSummaryRepository extends JpaRepository<ArticleSummary, Long> {

    // 기사 ID로 요약을 찾는다. 요약이 아직 없을 수 있으므로 Optional이다.
    Optional<ArticleSummary> findByArticleId(Long articleId);
}
