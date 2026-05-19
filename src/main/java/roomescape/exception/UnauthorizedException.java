package roomescape.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends RoomescapeException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}
