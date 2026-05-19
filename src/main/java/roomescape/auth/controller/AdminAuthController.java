package roomescape.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.controller.dto.AdminLoginRequest;
import roomescape.auth.service.AdminAuthService;

@Tag(name = "관리자 인증 API", description = "관리자 모드 진입/종료 API")
@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    public static final String ADMIN_SESSION_KEY = "IS_ADMIN";

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> adminLogin(
            @Valid @RequestBody AdminLoginRequest request,
            HttpSession session
    ) {
        adminAuthService.verifyAdminPassword(request.password());
        session.setAttribute(ADMIN_SESSION_KEY, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> adminLogout(HttpSession session) {
        session.removeAttribute(ADMIN_SESSION_KEY);
        return ResponseEntity.noContent().build();
    }
}
