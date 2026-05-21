package roomescape.auth.controller.dto;

public record AdminLoginResponse(String accessToken, String tokenType) {

    public static AdminLoginResponse from(String accessToken) {
        return new AdminLoginResponse(accessToken, "Bearer");
    }
}
