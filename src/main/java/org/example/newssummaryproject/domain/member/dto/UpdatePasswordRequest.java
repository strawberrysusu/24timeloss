package org.example.newssummaryproject.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 4, max = 100, message = "비밀번호는 4자 이상 100자 이하여야 합니다.")
        String newPassword
) {
}
