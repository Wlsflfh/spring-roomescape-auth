package roomescape.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.auth.interceptor.AdminAuthInterceptor;
import roomescape.auth.interceptor.AuthInterceptor;
import roomescape.auth.resolver.LoginMemberArgumentResolver;
import roomescape.auth.resolver.ManagerArgumentResolver;
import roomescape.auth.service.AuthTokenService;
import roomescape.member.service.MemberService;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MemberService memberService;
    private final AuthTokenService authTokenService;

    public WebMvcConfig(MemberService memberService, AuthTokenService authTokenService) {
        this.memberService = memberService;
        this.authTokenService = authTokenService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(authTokenService));

        registry.addInterceptor(new AdminAuthInterceptor(authTokenService))
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/login",
                        "/admin/logout",
                        "/admin/*.html",
                        "/admin/*.js"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver(memberService, authTokenService));
        resolvers.add(new ManagerArgumentResolver(memberService, authTokenService));
    }
}
