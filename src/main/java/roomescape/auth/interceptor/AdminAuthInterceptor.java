package roomescape.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.auth.controller.AdminAuthController;
import roomescape.exception.ForbiddenException;
import roomescape.exception.UnauthorizedException;

public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        if (session.getAttribute(AdminAuthController.ADMIN_SESSION_KEY) == null) {
            throw new ForbiddenException("관리자 권한이 필요합니다.");
        }

        return true;
    }
}
