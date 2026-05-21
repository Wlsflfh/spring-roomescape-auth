package roomescape.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.controller.dto.AdminLoginResponse;
import roomescape.auth.controller.dto.AdminLoginRequest;
import roomescape.auth.service.AdminAuthService;
import roomescape.auth.token.JwtTokenProvider;

@Tag(name = "관리자 인증 API", description = "관리자 모드 진입/종료 API")
@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthController(AdminAuthService adminAuthService, JwtTokenProvider jwtTokenProvider) {
        this.adminAuthService = adminAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> adminLogin(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        adminAuthService.verifyAdminPassword(request.password());
        String accessToken = jwtTokenProvider.createAdminToken();
        return ResponseEntity.ok(AdminLoginResponse.from(accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> adminLogout() {
        return ResponseEntity.noContent().build();
    }
}
