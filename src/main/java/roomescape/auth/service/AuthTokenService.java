package roomescape.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import roomescape.auth.token.JwtTokenProvider;
import roomescape.exception.UnauthorizedException;

@Service
public class AuthTokenService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public AuthTokenService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Long extractMemberId(HttpServletRequest request) {
        String token = extractBearerToken(request);
        return jwtTokenProvider.getMemberId(token);
    }

    public void validateAdmin(HttpServletRequest request) {
        String token = extractBearerToken(request);
        jwtTokenProvider.validateAdminToken(token);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return authorization.substring(BEARER_PREFIX.length());
    }
}
