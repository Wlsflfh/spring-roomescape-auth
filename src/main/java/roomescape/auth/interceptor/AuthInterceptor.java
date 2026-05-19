package roomescape.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.auth.annotation.LoginRequired;
import roomescape.auth.controller.AuthController;
import roomescape.exception.UnauthorizedException;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!handlerMethod.hasMethodAnnotation(LoginRequired.class)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthController.SESSION_KEY) == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return true;
    }
}
