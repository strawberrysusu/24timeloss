package org.example.newssummaryproject.domain.news;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 검색 로그를 저장/조회하는 Repository다.
 *
 * 핵심 기능: "최근 N일간 가장 많이 검색된 키워드"를 집계한다.
 *
 * SQL로 풀어 쓰면 이런 느낌이다:
 *   SELECT keyword, COUNT(*) as cnt
 *   FROM search_logs
 *   WHERE searched_at >= '2026-03-12 00:00:00'   -- 최근 7일
 *   GROUP BY keyword
 *   ORDER BY cnt DESC
 */
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 특정 시점(since) 이후의 검색 로그를 키워드별로 묶어서,
     * 많이 검색된 순서대로 반환한다.
     *
     * 반환 형태: Object[] 배열의 List
     *   row[0] = 키워드 (String)
     *   row[1] = 검색 횟수 (Long)
     *
     * 예: [["AI", 25], ["반도체", 18], ["손흥민", 12], ...]
     */
    @Query("SELECT s.keyword, COUNT(s) as cnt FROM SearchLog s " +
            "WHERE s.searchedAt >= :since " +
            "GROUP BY s.keyword ORDER BY cnt DESC")
    List<Object[]> findPopularKeywords(@Param("since") LocalDateTime since);
}
