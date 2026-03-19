package org.example.newssummaryproject.domain.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 검색 로그를 저장하는 엔티티다.
 *
 * 사용자가 기사를 검색할 때마다 키워드를 기록해서,
 * "인기 검색어" 목록을 만드는 데 사용한다.
 *
 * 예를 들어 "AI"를 10명이 검색하면 search_logs 테이블에 10개 행이 쌓이고,
 * GROUP BY keyword + COUNT(*) 로 "AI → 10회"를 뽑을 수 있다.
 */
@Getter
@Entity
@Table(name = "search_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자가 검색한 키워드 (예: "AI", "반도체", "손흥민")
    @Column(nullable = false, length = 200)
    private String keyword;

    // 검색한 시각 — 최근 7일 인기 검색어 집계 시 필터 조건으로 쓴다.
    @Column(nullable = false)
    private LocalDateTime searchedAt;

    /**
     * 새 검색 로그를 만든다.
     * searchedAt은 자동으로 현재 시각이 들어간다.
     */
    public SearchLog(String keyword) {
        this.keyword = keyword;
        this.searchedAt = LocalDateTime.now();
    }
}
