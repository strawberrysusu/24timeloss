package org.example.newssummaryproject.domain.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 기사 데이터를 조회/저장하는 Repository다.
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 카테고리별 최신 기사 목록을 뽑을 때 사용한다.
    List<Article> findByCategoryOrderByPublishedAtDesc(Category category);
    Page<Article> findByCategoryOrderByPublishedAtDesc(Category category, Pageable pageable);

    // 전체 기사를 최신순으로 조회한다.
    List<Article> findAllByOrderByPublishedAtDesc();
    Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable);

    // 제목 또는 본문에서 키워드를 검색한다.
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.content LIKE %:keyword% ORDER BY a.publishedAt DESC")
    List<Article> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.content LIKE %:keyword% ORDER BY a.publishedAt DESC")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 관심 카테고리 목록에 해당하는 기사를 최신순으로 가져온다. (추천용)
    List<Article> findByCategoryInOrderByPublishedAtDesc(List<Category> categories);
    Page<Article> findByCategoryInOrderByPublishedAtDesc(List<Category> categories, Pageable pageable);

    // 같은 카테고리의 관련 기사를 가져온다. (상세 페이지 관련 뉴스용, 현재 기사 제외)
    Page<Article> findByCategoryAndIdNotOrderByPublishedAtDesc(Category category, Long id, Pageable pageable);

    // ── 트렌딩: 조회수 높은 순 + 같은 조회수면 최신순 ──
    @Query("SELECT a FROM Article a ORDER BY a.viewCount DESC, a.publishedAt DESC")
    Page<Article> findTrending(Pageable pageable);

    // ── 관련 뉴스: 제목에 특정 키워드가 포함된 기사 (현재 기사 제외) ──
    @Query("SELECT a FROM Article a WHERE a.id != :excludeId AND a.title LIKE %:keyword% ORDER BY a.publishedAt DESC")
    List<Article> findRelatedByTitleKeyword(@Param("excludeId") Long excludeId, @Param("keyword") String keyword, Pageable pageable);
}
