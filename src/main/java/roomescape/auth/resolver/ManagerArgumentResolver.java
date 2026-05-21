package roomescape.auth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.auth.annotation.LoginManager;
import roomescape.auth.service.AuthTokenService;
import roomescape.exception.ForbiddenException;
import roomescape.member.domain.Member;
import roomescape.member.service.MemberService;

public class ManagerArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberService memberService;
    private final AuthTokenService authTokenService;

    public ManagerArgumentResolver(MemberService memberService, AuthTokenService authTokenService) {
        this.memberService = memberService;
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginManager.class)
                && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Member resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        Long memberId = authTokenService.extractMemberId(request);
        Member member = memberService.getById(memberId);

        if (!member.isManager()) {
            throw new ForbiddenException("매장 매니저 권한이 필요합니다.");
        }

        return member;
    }
}
