package roomescape.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "관리자 비밀번호를 입력해야 합니다.")
        String password
) {
}
