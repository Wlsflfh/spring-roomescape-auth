package roomescape.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.controller.dto.LoginRequest;
import roomescape.exception.UnauthorizedException;
import roomescape.member.domain.Member;

import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        insertMember("brown", "브라운", "password1234");
    }

    @Nested
    @DisplayName("login 메서드는")
    class Login {

        @Test
        @DisplayName("아이디와 비밀번호가 일치하면 해당 회원을 반환한다.")
        void loginSuccess() {
            LoginRequest request = new LoginRequest("brown", "password1234");

            Member result = authService.login(request);

            assertThat(result.getLoginId()).isEqualTo("brown");
            assertThat(result.getName()).isEqualTo("브라운");
        }

        @Test
        @DisplayName("존재하지 않는 아이디로 로그인하면 예외가 발생한다.")
        void loginFailWhenLoginIdNotFound() {
            LoginRequest request = new LoginRequest("ghost", "password1234");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 예외가 발생한다.")
        void loginFailWhenWrongPassword() {
            LoginRequest request = new LoginRequest("brown", "wrongpassword");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 아이디와 틀린 비밀번호 모두 동일한 메시지로 응답한다. (아이디 존재 여부 노출 방지)")
        void loginFailMessageIsIdentical() {
            String notFoundMsg = null;
            String wrongPwMsg  = null;

            try { authService.login(new LoginRequest("ghost", "pw")); }
            catch (UnauthorizedException e) { notFoundMsg = e.getMessage(); }

            try { authService.login(new LoginRequest("brown", "wrongpw")); }
            catch (UnauthorizedException e) { wrongPwMsg = e.getMessage(); }

            assertThat(notFoundMsg).isEqualTo(wrongPwMsg);
        }
    }

    private void insertMember(String loginId, String name, String password) {
        String sql = "INSERT INTO member (login_id, name, password) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, loginId);
            ps.setString(2, name);
            ps.setString(3, password);
            return ps;
        }, keyHolder);
    }
}
