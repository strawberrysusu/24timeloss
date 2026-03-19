package org.example.newssummaryproject.global.exception;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외다.
 * 예: 존재하지 않는 기사, 존재하지 않는 회원
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
