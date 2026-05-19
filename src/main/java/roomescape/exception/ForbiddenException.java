package roomescape.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RoomescapeException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
