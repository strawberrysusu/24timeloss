package org.example.newssummaryproject.domain.member.dto;

public record SignupRequest(
        String email,
        String password,
        String nickname
) {
}
