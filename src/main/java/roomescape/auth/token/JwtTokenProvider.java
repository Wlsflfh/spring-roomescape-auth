package roomescape.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import roomescape.exception.UnauthorizedException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";

    private final SecretKey secretKey;
    private final Duration expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-millis}") long expirationMillis
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMillis(expirationMillis);
    }

    public String createMemberToken(Long memberId) {
        return createToken(String.valueOf(memberId), TokenType.MEMBER);
    }

    public String createAdminToken() {
        return createToken("admin", TokenType.ADMIN);
    }

    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        validateType(claims, TokenType.MEMBER);
        return Long.valueOf(claims.getSubject());
    }

    public void validateAdminToken(String token) {
        Claims claims = parseClaims(token);
        validateType(claims, TokenType.ADMIN);
    }

    private String createToken(String subject, TokenType tokenType) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .setSubject(subject)
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }

    private void validateType(Claims claims, TokenType expectedType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedType.name().equals(tokenType)) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }
}
