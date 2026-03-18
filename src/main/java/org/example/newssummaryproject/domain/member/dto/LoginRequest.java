package org.example.newssummaryproject.domain.member.dto;

public record LoginRequest(
        String email,
        String password
) {
}
