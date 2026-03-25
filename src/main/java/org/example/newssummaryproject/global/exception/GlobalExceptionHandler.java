package org.example.newssummaryproject.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 예외를 통일된 ErrorResponse 형식으로 반환하는 전역 예외 핸들러다.
 *
 * 어떻게 동작하나?
 *   1. Controller/Service 어디서든 예외가 throw되면
 *   2. Spring이 이 클래스의 @ExceptionHandler 중 매칭되는 것을 찾아서
 *   3. 해당 메서드가 ErrorResponse JSON을 만들어 클라이언트에 반환한다.
 *
 * @RestControllerAdvice: 모든 @RestController에 공통으로 적용되는 예외 처리기.
 *   이게 없으면 각 Controller마다 try-catch를 써야 한다 → 코드 중복이 심해진다.
 *   이 한 곳에서 예외→HTTP 응답 변환을 통일적으로 처리하는 패턴이다.
 *
 * 응답 예시:
 * {
 *   "status": 404,
 *   "code": "NOT_FOUND",
 *   "message": "기사를 찾을 수 없습니다.",
 *   "timestamp": "2026-03-19T12:00:00"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 — 리소스를 찾을 수 없음 (회원, 기사 등)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "NOT_FOUND", e.getMessage()));
    }

    // 409 — 중복 데이터 (이메일 중복, 이미 저장한 기사 등)
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "DUPLICATE", e.getMessage()));
    }

    // 400 — 잘못된 요청 (비밀번호 불일치 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "BAD_REQUEST", e.getMessage()));
    }

    // 401 — 인증 필요 (로그인 안 한 상태에서 접근)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "UNAUTHORIZED", e.getMessage()));
    }

    // 403 — 권한 없음 (다른 사람의 기사 수정/삭제 시도)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "FORBIDDEN", e.getMessage()));
    }

    // 503 — AI 요약 서비스 오류 (외부 API 호출 실패)
    @ExceptionHandler(AiSummaryException.class)
    public ResponseEntity<ErrorResponse> handleAiSummary(AiSummaryException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(503, "AI_SUMMARY_FAILED", e.getMessage()));
    }

    // 400 — @Valid 검증 실패 (이메일 형식, 비밀번호 길이 등)
    // Controller에서 @Valid가 붙은 DTO의 @NotBlank, @Size 등이 실패하면 이 예외가 발생한다.
    // getFieldErrors()에서 첫 번째 검증 실패 메시지를 꺼내서 응답에 담는다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "VALIDATION_ERROR", message));
    }
}
