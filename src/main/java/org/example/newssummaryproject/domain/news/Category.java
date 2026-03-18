package org.example.newssummaryproject.domain.news;

/**
 * 뉴스 카테고리 목록이다.
 *
 * 홈 화면 탭, 관심 분야 설정, 기사 분류에서 공통으로 사용한다.
 * DB에는 문자열 그대로 저장해서 숫자보다 읽기 쉽게 유지한다.
 */
public enum Category {
    POLITICS,
    ECONOMY,
    SOCIETY,
    IT_SCIENCE,
    WORLD,
    SPORTS,
    ENTERTAINMENT
}
