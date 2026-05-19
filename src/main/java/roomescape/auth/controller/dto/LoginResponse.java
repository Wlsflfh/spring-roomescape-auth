package roomescape.auth.controller.dto;

import roomescape.member.domain.Member;

public record LoginResponse(Long id, String loginId, String name) {

    public static LoginResponse from(Member member) {
        return new LoginResponse(member.getId(), member.getLoginId(), member.getName());
    }
}
