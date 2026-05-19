package roomescape.reservation.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AdminReservationRequest(
        @NotNull(message = "예약 날짜는 필수입니다.")
        LocalDate date,

        @NotNull(message = "예약 시간 ID는 필수입니다.")
        @Positive(message = "예약 시간 ID는 양수여야 합니다.")
        Long timeId,

        @NotNull(message = "테마 ID는 필수입니다.")
        @Positive(message = "테마 ID는 양수여야 합니다.")
        Long themeId,

        @NotNull(message = "회원 ID는 필수입니다.")
        @Positive(message = "회원 ID는 양수여야 합니다.")
        Long memberId
) {
}
