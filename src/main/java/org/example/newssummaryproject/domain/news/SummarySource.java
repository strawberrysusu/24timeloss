package org.example.newssummaryproject.domain.news;

/**
 * 요약의 출처를 구분하는 enum이다.
 *
 * AI가 생성한 요약인지, 사람이 직접 쓴 요약인지, 초기 샘플 데이터인지
 * 구분해서 화면에 다른 라벨을 보여주기 위해 사용한다.
 *
 * 예: AI_GENERATED → "AI 요약", MANUAL → "작성자 요약", SEED → "샘플 요약"
 */
public enum SummarySource {
    AI_GENERATED,   // AI 모델이 자동 생성한 요약
    MANUAL,         // 사용자가 기사 등록/수정 시 직접 입력한 요약
    SEED            // DataInitializer가 넣은 시연용 샘플 데이터
}
