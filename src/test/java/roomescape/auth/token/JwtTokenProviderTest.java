package roomescape.auth.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import roomescape.exception.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("회원 토큰은")
    class MemberToken {

        @Test
        @DisplayName("회원 ID를 담아 발급되고 검증된다.")
        void createAndParseMemberToken() {
            String token = jwtTokenProvider.createMemberToken(1L);

            Long memberId = jwtTokenProvider.getMemberId(token);

            assertThat(memberId).isEqualTo(1L);
        }

        @Test
        @DisplayName("관리자 토큰으로 검증할 수 없다.")
        void memberTokenCannotBeUsedAsAdminToken() {
            String token = jwtTokenProvider.createMemberToken(1L);

            assertThatThrownBy(() -> jwtTokenProvider.validateAdminToken(token))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("관리자 토큰은")
    class AdminToken {

        @Test
        @DisplayName("관리자 권한으로 검증된다.")
        void createAndValidateAdminToken() {
            String token = jwtTokenProvider.createAdminToken();

            assertThatCode(() -> jwtTokenProvider.validateAdminToken(token))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("회원 토큰으로 검증할 수 없다.")
        void adminTokenCannotBeUsedAsMemberToken() {
            String token = jwtTokenProvider.createAdminToken();

            assertThatThrownBy(() -> jwtTokenProvider.getMemberId(token))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}
