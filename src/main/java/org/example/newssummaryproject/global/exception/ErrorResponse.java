package org.example.newssummaryproject.global.exception;

import java.time.LocalDateTime;

/**
 * 모든 에러 응답에 사용되는 통일된 형식이다.
 * 프론트엔드에서 status, code, message를 일관되게 파싱할 수 있다.
 */
public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, LocalDateTime.now());
    }
}
