package org.example.newssummaryproject.domain.member.dto;

import org.example.newssummaryproject.domain.member.Member;

/**
 * 회원 정보 응답 DTO.
 *
 * 엔티티(Member)를 그대로 JSON으로 내보내지 않고 DTO로 변환하는 이유:
 *   1. 보안: password 같은 민감 정보가 응답에 포함되는 것을 방지
 *   2. 유연성: API 응답 형태를 DB 구조와 독립적으로 변경 가능
 *   3. 순환 참조 방지: 엔티티 간 양방향 관계가 있으면 JSON 변환 시 무한 루프 발생 가능
 *
 * from() 팩토리 메서드: Member 엔티티 → MemberResponse 변환을 한 곳에서 관리한다.
 */
public record MemberResponse(
        Long id,
        String email,
        String nickname
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname()
        );
    }
}
