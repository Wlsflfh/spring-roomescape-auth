package roomescape.member.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberRequest(
        @NotBlank(message = "로그인 ID는 비어있을 수 없습니다.")
        @Size(max = 255, message = "로그인 ID는 255자 이하여야 합니다.")
        String loginId,

        @NotBlank(message = "이름은 비어있을 수 없습니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
        @Size(max = 255, message = "비밀번호는 255자 이하여야 합니다.")
        String password
) {
}
