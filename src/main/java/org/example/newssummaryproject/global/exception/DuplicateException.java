package org.example.newssummaryproject.global.exception;

/**
 * 중복된 데이터가 이미 존재할 때 발생하는 예외다.
 * 예: 이메일 중복, 이미 저장한 기사, 이미 등록한 관심 분야
 */
public class DuplicateException extends RuntimeException {

    public DuplicateException(String message) {
        super(message);
    }
}
