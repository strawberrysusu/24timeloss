package org.example.newssummaryproject.domain.member.dto;

/**
 * 로그인/회원가입 응답 DTO.
 * Access Token + 사용자 기본 정보를 JSON으로 반환한다.
 * Refresh Token은 httpOnly 쿠키로 별도 전달된다.
 */
public record LoginResponse(
        String token,
        Long id,
        String email,
        String nickname
) {
    public static LoginResponse of(String token, MemberResponse member) {
        return new LoginResponse(token, member.id(), member.email(), member.nickname());
    }
}
