package roomescape.auth.controller.dto;

import roomescape.member.domain.Member;

public record LoginResponse(Long id, String loginId, String name, String role, String accessToken, String tokenType) {

    public static LoginResponse from(Member member, String accessToken) {
        return new LoginResponse(
                member.getId(),
                member.getLoginId(),
                member.getName(),
                member.getRole().name(),
                accessToken,
                "Bearer"
        );
    }
}
