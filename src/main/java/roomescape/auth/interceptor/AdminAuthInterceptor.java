package roomescape.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.auth.service.AuthTokenService;

public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AuthTokenService authTokenService;

    public AdminAuthInterceptor(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        authTokenService.validateAdmin(request);
        return true;
    }
}
