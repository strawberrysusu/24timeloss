package org.example.newssummaryproject.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * 회원가입 요청 DTO (Data Transfer Object).
 *
 * record란? Java 16에서 추가된 불변(immutable) 데이터 클래스다.
 * 아래처럼 선언하면 컴파일러가 자동으로:
 *   - 생성자, getter(email(), password(), nickname()), equals, hashCode, toString을 만든다.
 *   - 필드는 final이라 한번 생성하면 값을 변경할 수 없다.
 * → DTO처럼 "데이터를 담아 전달하는 용도"에 딱 맞는다.
 *
 * @Valid 검증 어노테이션:
 *   Controller에서 @Valid @RequestBody SignupRequest request라고 쓰면
 *   Spring이 JSON → SignupRequest 변환 후 자동으로 아래 검증을 실행한다.
 *   실패하면 MethodArgumentNotValidException → GlobalExceptionHandler가 400 응답을 만든다.
 */
public record SignupRequest(
        // @NotBlank: null, "", " " 모두 불허. 이메일은 반드시 입력해야 한다.
        // @Email: 이메일 형식(xxx@xxx.xxx) 검증. 잘못된 형식이면 400 에러.
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        // @Size(min, max): 문자열 길이 제한. 보안상 너무 짧거나 긴 비밀번호를 방지.
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 100, message = "비밀번호는 4자 이상 100자 이하여야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 30, message = "닉네임은 1자 이상 30자 이하여야 합니다.")
        String nickname
) {
}
