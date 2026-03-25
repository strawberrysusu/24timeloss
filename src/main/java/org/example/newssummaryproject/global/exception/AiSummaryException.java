package org.example.newssummaryproject.global.exception;

/**
 * AI 요약 생성 중 발생하는 예외다.
 *
 * 외부 AI API 호출 실패, 응답 파싱 실패, 빈 응답 등의 상황에서 던진다.
 * GlobalExceptionHandler에서 503(Service Unavailable)으로 처리된다.
 */
public class AiSummaryException extends RuntimeException {

    public AiSummaryException(String message) {
        super(message);
    }

    public AiSummaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
