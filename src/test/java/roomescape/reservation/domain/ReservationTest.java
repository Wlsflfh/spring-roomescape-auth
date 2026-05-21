package roomescape.reservation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.InvalidDomainStateException;
import roomescape.member.domain.Member;
import roomescape.store.domain.Store;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

class ReservationTest {

    private final Member member = new Member(1L, "brown", "브라운", "password");
    private final ReservationTime reservationTime = new ReservationTime(1L, LocalTime.of(15, 0));
    private final Store store = new Store(1L, "테스트 매장", "테스트 매장 설명");
    private final Theme theme = new Theme(1L, "테마", "설명", "url", store);
    private final LocalDateTime today = LocalDateTime.now();
    private final LocalDate futureDate = LocalDate.now().plusDays(1);
    private final LocalDate pastDate = LocalDate.now().minusDays(1);

    @Nested
    @DisplayName("생성 및 필수값 검증 테스트")
    class CreationTest {

        @Test
        @DisplayName("정상적인 데이터로 예약 객체를 생성한다.")
        void createSuccess() {
            assertThatCode(() -> new Reservation(1L, member, futureDate, reservationTime, theme, ReservationStatus.RESERVED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("회원 정보가 null이면 예외가 발생한다.")
        void failWhenMemberIsNull() {
            assertThatThrownBy(() -> new Reservation(1L, null, futureDate, reservationTime, theme, ReservationStatus.RESERVED))
                    .isInstanceOf(InvalidDomainStateException.class);
        }

        @Test
        @DisplayName("필수 데이터(날짜, 시간, 테마)가 누락되면 예외가 발생한다.")
        void failWhenRequiredFieldIsNull() {
            assertThatThrownBy(() -> new Reservation(1L, member, null, reservationTime, theme, ReservationStatus.RESERVED))
                    .isInstanceOf(InvalidDomainStateException.class);
        }
    }

    @Nested
    class CreateFactoryTest {

        @Test
        @DisplayName("미래 일시면 정상 생성한다.")
        void createSuccessWhenFuture() {
            assertThatCode(() -> Reservation.create(member, futureDate, reservationTime, theme, today))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("과거 일시로 생성하면 예외가 발생한다.")
        void createFailWhenPast() {
            assertThatThrownBy(() -> Reservation.create(member, pastDate, reservationTime, theme, today))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("오늘 날짜라도 예약 시간이 현재 시간보다 이전이면 예외가 발생한다.")
        void failWhenTodayButPastTime() {
            LocalDate date = today.toLocalDate();
            LocalTime time = today.toLocalTime().minusMinutes(1);

            assertThatThrownBy(() -> Reservation.create(member, date, new ReservationTime(1L, time), theme, today))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("회원 정보가 null이면 예외가 발생한다.")
        void createFailWhenMemberIsNull() {
            assertThatThrownBy(() -> Reservation.create(null, futureDate, reservationTime, theme, today))
                    .isInstanceOf(InvalidDomainStateException.class);
        }
    }

    @Nested
    class CancelTest {

        @Test
        @DisplayName("미래 시점의 예약은 취소 검증을 통과한다.")
        void validateCancelSuccess() {
            Reservation reservation = new Reservation(1L, member, futureDate, reservationTime, theme, ReservationStatus.RESERVED);

            assertThatCode(() -> reservation.validateCanCancel(today))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("시간이 지난 예약은 취소할 수 없다.")
        void pastDateCanceled() {
            Reservation reservation = new Reservation(1L, member, pastDate, reservationTime, theme, ReservationStatus.RESERVED);

            assertThatThrownBy(() -> reservation.validateCanCancel(today))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    class UpdateTest {

        @Test
        @DisplayName("미래 시점의 예약은 변경할 수 있다.")
        void updateSuccess() {
            Reservation reservation = new Reservation(1L, member, futureDate, reservationTime, theme, ReservationStatus.RESERVED);

            assertThatCode(() -> reservation.update(futureDate, reservationTime, theme, today))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 취소된 예약은 변경할 수 없다.")
        void alreadyCanceled() {
            Reservation reservation = new Reservation(1L, member, futureDate, reservationTime, theme, ReservationStatus.CANCELED);

            assertThatThrownBy(() -> reservation.update(futureDate, reservationTime, theme, today))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("이미 완료된 예약은 변경할 수 없다.")
        void alreadyCompleted() {
            Reservation reservation = new Reservation(1L, member, futureDate, reservationTime, theme, ReservationStatus.COMPLETED);

            assertThatThrownBy(() -> reservation.update(futureDate, reservationTime, theme, today))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    class ConvertStatusTest {

        @Test
        @DisplayName("시간이 지난 예약에 대해서 COMPLETED로 예약 상태가 변경된다.")
        void convertCompleted() {
            Reservation reservation = new Reservation(1L, member, pastDate, reservationTime, theme, ReservationStatus.RESERVED);

            Reservation updated = reservation.convertStatusByCurrentTime(today);

            assertThat(updated.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }

        @Test
        @DisplayName("CANCELED된 예약은 시간이 지나도 CANCELED 상태로 유지된다.")
        void canceledStaysCanceled() {
            Reservation reservation = new Reservation(1L, member, futureDate, reservationTime, theme, ReservationStatus.CANCELED);

            Reservation updated = reservation.convertStatusByCurrentTime(today);

            assertThat(updated.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        }
    }
}
