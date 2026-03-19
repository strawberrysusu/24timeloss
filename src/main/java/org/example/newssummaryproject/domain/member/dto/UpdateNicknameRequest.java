package org.example.newssummaryproject.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 30, message = "닉네임은 1자 이상 30자 이하여야 합니다.")
        String nickname
) {
}
