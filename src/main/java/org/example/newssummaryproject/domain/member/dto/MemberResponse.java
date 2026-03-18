package org.example.newssummaryproject.domain.member.dto;

import org.example.newssummaryproject.domain.member.Member;

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
