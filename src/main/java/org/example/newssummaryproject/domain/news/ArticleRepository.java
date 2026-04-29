package org.example.newssummaryproject.domain.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기사 데이터를 조회/저장하는 Repository다.
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 같은 URL로 이미 등록된 기사가 있는지 확인한다. (중복 등록 방지)
    Optional<Article> findByOriginalUrl(String originalUrl);

    // 카테고리별 최신 기사 목록을 뽑을 때 사용한다.
    Page<Article> findByCategoryOrderByPublishedAtDesc(Category category, Pageable pageable);

    // 전체 기사를 최신순으로 조회한다.
    Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable);

    // 제목 또는 본문에서 키워드를 검색한다.
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.content LIKE %:keyword% ORDER BY a.publishedAt DESC")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 관심 카테고리 목록에 해당하는 기사를 최신순으로 가져온다. (추천용)
    Page<Article> findByCategoryInOrderByPublishedAtDesc(List<Category> categories, Pageable pageable);

    // 같은 카테고리의 관련 기사를 가져온다. (상세 페이지 관련 뉴스용, 현재 기사 제외)
    Page<Article> findByCategoryAndIdNotOrderByPublishedAtDesc(Category category, Long id, Pageable pageable);

    // ── 트렌딩: 조회수 높은 순 + 같은 조회수면 최신순 ──
    @Query("SELECT a FROM Article a ORDER BY a.viewCount DESC, a.publishedAt DESC")
    Page<Article> findTrending(Pageable pageable);

    // ── 관련 뉴스: 제목에 특정 키워드가 포함된 기사 (현재 기사 제외) ──
    @Query("SELECT a FROM Article a WHERE a.id != :excludeId AND a.title LIKE %:keyword% ORDER BY a.publishedAt DESC")
    List<Article> findRelatedByTitleKeyword(@Param("excludeId") Long excludeId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * Retention 정리 대상 기사 ID를 조회한다.
     *
     * 조건 (모두 만족해야 함):
     *   - writer IS NULL : 시스템이 자동 수집한 기사 (사용자 직접 작성 기사는 보존)
     *   - createdAt < cutoff : 우리 DB에 들어온 지 N일 지난 기사
     *   - 누구도 저장하지 않은 기사 (saved_articles에 없는 것만)
     *
     * 읽기 기록(read_history)이 있어도 저장 안 했으면 정리 대상이다.
     * "한 번 본 것"과 "의도적으로 보관한 것"은 다른 의미라고 보기 때문.
     */
    @Query("SELECT a.id FROM Article a " +
            "WHERE a.writer IS NULL " +
            "  AND a.createdAt < :cutoff " +
            "  AND a.id NOT IN (SELECT s.article.id FROM SavedArticle s)")
    List<Long> findOldUnsavedSystemArticleIds(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);
}
