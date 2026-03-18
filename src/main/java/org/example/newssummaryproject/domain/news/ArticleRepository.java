package org.example.newssummaryproject.domain.news;

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

    // 전체 기사를 최신순으로 조회한다.
    List<Article> findAllByOrderByPublishedAtDesc();

    // 제목 또는 본문에서 키워드를 검색한다.
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.content LIKE %:keyword% ORDER BY a.publishedAt DESC")
    List<Article> searchByKeyword(@Param("keyword") String keyword);

    // 관심 카테고리 목록에 해당하는 기사를 최신순으로 가져온다. (추천용)
    List<Article> findByCategoryInOrderByPublishedAtDesc(List<Category> categories);
}
