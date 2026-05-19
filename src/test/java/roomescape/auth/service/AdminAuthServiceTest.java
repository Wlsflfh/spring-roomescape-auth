package roomescape.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import roomescape.exception.ForbiddenException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AdminAuthServiceTest {

    @Autowired
    private AdminAuthService adminAuthService;

    @Nested
    @DisplayName("verifyAdminPassword 메서드는")
    class VerifyAdminPassword {

        @Test
        @DisplayName("올바른 관리자 비밀번호면 예외가 발생하지 않는다.")
        void verifySuccess() {
            assertThatCode(() -> adminAuthService.verifyAdminPassword("admin1234"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("틀린 관리자 비밀번호면 예외가 발생한다.")
        void verifyFailWhenWrongPassword() {
            assertThatThrownBy(() -> adminAuthService.verifyAdminPassword("wrongpassword"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("빈 문자열로 검증하면 예외가 발생한다.")
        void verifyFailWhenEmptyPassword() {
            assertThatThrownBy(() -> adminAuthService.verifyAdminPassword(""))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("null 로 검증하면 예외가 발생한다.")
        void verifyFailWhenNullPassword() {
            assertThatThrownBy(() -> adminAuthService.verifyAdminPassword(null))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
