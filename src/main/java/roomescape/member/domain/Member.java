package roomescape.member.domain;

import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.InvalidDomainStateException;

public class Member {

    private static final int MAX_LOGIN_ID_LENGTH = 255;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_PASSWORD_LENGTH = 255;

    private final Long id;
    private final String loginId;
    private final String name;
    private final String password;

    public Member(Long id, String loginId, String name, String password) {
        validateText(loginId, "아이디", MAX_LOGIN_ID_LENGTH);
        validateText(name, "이름", MAX_NAME_LENGTH);
        validateText(password, "비밀번호", MAX_PASSWORD_LENGTH);
        this.id = id;
        this.loginId = loginId;
        this.name = name;
        this.password = password;
    }

    public static Member create(String loginId, String name, String password) {
        return new Member(null, loginId, name, password);
    }

    public boolean isEquals(Long id) {
        return this.id.equals(id);
    }

    private void validateText(String value, String obj, int maxSize) {
        validateNotNull(value, String.format("%s는 반드시 입력해야 합니다.", obj));
        if (value.isBlank()) {
            throw new InvalidDomainStateException(String.format("%s는 반드시 입력해야 합니다.", obj));
        }
        if (value.length() > maxSize) {
            throw new BusinessRuleViolationException(
                    String.format("%s는 %d자 이하여야 합니다.", obj, maxSize)
            );
        }
    }

    private void validateNotNull(Object obj, String message) {
        if (obj == null) {
            throw new InvalidDomainStateException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }
}
