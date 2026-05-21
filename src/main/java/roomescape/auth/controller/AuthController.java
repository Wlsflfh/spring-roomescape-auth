package roomescape.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.controller.dto.LoginRequest;
import roomescape.auth.controller.dto.LoginResponse;
import roomescape.auth.service.AuthService;
import roomescape.auth.token.JwtTokenProvider;
import roomescape.member.domain.Member;

@Tag(name = "인증 API", description = "로그인, 로그아웃 관련 API")
@RestController
@RequestMapping("/login")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Member member = authService.login(request);
        String accessToken = jwtTokenProvider.createMemberToken(member.getId());
        return ResponseEntity.ok(LoginResponse.from(member, accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
