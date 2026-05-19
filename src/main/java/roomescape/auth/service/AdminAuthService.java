package roomescape.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import roomescape.exception.ForbiddenException;

@Service
public class AdminAuthService {

    private final String adminPassword;

    public AdminAuthService(@Value("${admin.password}") String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void verifyAdminPassword(String password) {
        if (!adminPassword.equals(password)) {
            throw new ForbiddenException("관리자 비밀번호가 올바르지 않습니다.");
        }
    }
}
