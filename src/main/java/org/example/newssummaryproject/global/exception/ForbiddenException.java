package org.example.newssummaryproject.global.exception;

/**
 * 권한이 없는 리소스에 접근할 때 발생하는 예외다.
 * 예: 다른 사람이 쓴 기사를 수정/삭제하려고 할 때
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
