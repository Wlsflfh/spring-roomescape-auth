package roomescape.reservation.domain;

import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.InvalidDomainStateException;
import roomescape.member.domain.Member;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Reservation {

    private final Long id;
    private final Member member;
    private final LocalDate date;
    private final ReservationTime time;
    private final Theme theme;
    private final ReservationStatus status;

    public Reservation(
            Long id,
            Member member,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            ReservationStatus status
    ) {
        validateNotNull(member, "예약자는 필수입니다.");
        validateNotNull(date, "예약 날짜는 필수입니다.");
        validateNotNull(time, "예약 시간은 필수입니다.");
        validateNotNull(theme, "예약 테마는 필수입니다.");
        validateNotNull(status, "예약 상태는 필수입니다.");

        this.id = id;
        this.member = member;
        this.date = date;
        this.time = time;
        this.theme = theme;
        this.status = status;
    }

    public static Reservation create(
            Member member,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            LocalDateTime now
    ) {
        Reservation reservation = new Reservation(null, member, date, time, theme, ReservationStatus.RESERVED);
        reservation.validateNotPast(now);
        return reservation;
    }

    public Reservation update(
            LocalDate date,
            ReservationTime time,
            Theme theme,
            LocalDateTime now
    ) {
        if (!isReserved()) {
            throw new BusinessRuleViolationException("이미 취소되었거나 완료된 예약은 수정할 수 없습니다.");
        }

        Reservation updated = new Reservation(this.id, this.member, date, time, theme, this.status);
        updated.validateNotPast(now);
        return updated;
    }

    public Reservation convertStatusByCurrentTime(LocalDateTime now) {
        if (isReserved() && isCompleted(now)) {
            return new Reservation(this.id, this.member, this.date, this.time, this.theme, ReservationStatus.COMPLETED);
        }

        return this;
    }

    public boolean isCanceled() {
        return this.status == ReservationStatus.CANCELED;
    }

    public void validateCanCancel(LocalDateTime now) {
        if (isCompleted(now)) {
            throw new BusinessRuleViolationException("이미 이용 완료된 예약은 취소할 수 없습니다.");
        }
    }

    public boolean isEqualId(Long id) {
        return member.isEquals(id);
    }

    private boolean isReserved() {
        return this.status == ReservationStatus.RESERVED;
    }

    private boolean isCompleted(LocalDateTime now) {
        validateNotNull(now, "현재 시각은 반드시 입력해야 합니다.");
        LocalDateTime reservationDateTime = LocalDateTime.of(this.date, this.time.getStartAt());
        return !reservationDateTime.isAfter(now);
    }

    private void validateNotPast(LocalDateTime now) {
        if (isCompleted(now)) {
            throw new BusinessRuleViolationException("과거 시각으로는 예약할 수 없습니다.");
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

    public Member getMember() {
        return member;
    }

    public LocalDate getDate() {
        return date;
    }

    public ReservationTime getTime() {
        return time;
    }

    public Theme getTheme() {
        return theme;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
