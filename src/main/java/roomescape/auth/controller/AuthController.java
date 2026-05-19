package roomescape.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.controller.dto.LoginRequest;
import roomescape.auth.controller.dto.LoginResponse;
import roomescape.auth.service.AuthService;
import roomescape.member.domain.Member;

@Tag(name = "인증 API", description = "로그인, 로그아웃 관련 API")
@RestController
@RequestMapping("/login")
public class AuthController {

    public static final String SESSION_KEY = "MEMBER_ID";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session
    ) {
        Member member = authService.login(request);
        session.setAttribute(SESSION_KEY, member.getId());
        return ResponseEntity.ok(LoginResponse.from(member));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
