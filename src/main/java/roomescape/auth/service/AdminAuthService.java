package roomescape.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import roomescape.exception.ForbiddenException;

@Service
public class AdminAuthService {

    private final String adminPasswordHash;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthService(
            @Value("${admin.password-hash}") String adminPasswordHash,
            PasswordEncoder passwordEncoder
    ) {
        this.adminPasswordHash = adminPasswordHash;
        this.passwordEncoder = passwordEncoder;
    }

    public void verifyAdminPassword(String password) {
        if (password == null || !passwordEncoder.matches(password, adminPasswordHash)) {
            throw new ForbiddenException("관리자 비밀번호가 올바르지 않습니다.");
        }
    }
}
