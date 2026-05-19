package roomescape.reservation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.DuplicateResourceException;
import roomescape.exception.ForbiddenException;
import roomescape.exception.ResourceNotFoundException;
import roomescape.member.domain.Member;
import roomescape.member.service.MemberService;
import roomescape.reservation.controller.dto.AdminReservationRequest;
import roomescape.reservation.controller.dto.ReservationRequest;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.theme.domain.Theme;
import roomescape.theme.service.ThemeService;
import roomescape.time.domain.ReservationTime;
import roomescape.time.service.ReservationTimeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeService reservationTimeService;
    private final ThemeService themeService;
    private final MemberService memberService;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeService reservationTimeService,
            ThemeService themeService,
            MemberService memberService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeService = reservationTimeService;
        this.themeService = themeService;
        this.memberService = memberService;
    }

    @Transactional
    public Reservation saveByAdmin(AdminReservationRequest request) {
        Member member = memberService.getById(request.memberId());
        validateDuplicateReservation(request.date(), request.timeId(), request.themeId());

        ReservationTime time = reservationTimeService.getById(request.timeId());
        Theme theme = themeService.getById(request.themeId());

        Reservation reservation = Reservation.create(member, request.date(), time, theme, LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation save(ReservationRequest request, Member member) {
        validateDuplicateReservation(request.date(), request.timeId(), request.themeId());

        ReservationTime time = reservationTimeService.getById(request.timeId());
        Theme theme = themeService.getById(request.themeId());

        Reservation reservation = Reservation.create(member, request.date(), time, theme, LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation update(Long id, ReservationRequest request) {
        Reservation existing = getById(id);
        ReservationTime time = reservationTimeService.getById(request.timeId());
        Theme theme = themeService.getById(request.themeId());

        validateDuplicateReservationExcludingSelf(id, request);

        Reservation updated = existing.update(request.date(), time, theme, LocalDateTime.now());
        reservationRepository.update(updated);
        return updated;
    }

    @Transactional
    public void cancelById(Long id) {
        Reservation reservation = getById(id);
        if (reservation.isCanceled()) {
            return;
        }

        reservation.validateCanCancel(LocalDateTime.now());
        reservationRepository.updateStatus(id, ReservationStatus.CANCELED);
    }

    @Transactional
    public void cancelByIdAndMember(Long id, Long memberId) {
        Reservation reservation = getById(id);

        if (!reservation.getMember().getId().equals(memberId)) {
            throw new ForbiddenException("본인의 예약만 취소할 수 있습니다.");
        }

        if (reservation.isCanceled()) {
            return;
        }

        reservation.validateCanCancel(LocalDateTime.now());
        reservationRepository.updateStatus(id, ReservationStatus.CANCELED);
    }

    public List<Reservation> findByFilter(Long memberId, LocalDate from, LocalDate to, Long themeId) {
        validatePeriodOrder(from, to);
        return reservationRepository.findByFilter(memberId, from, to, themeId)
                .stream()
                .map(r -> r.convertStatusByCurrentTime(LocalDateTime.now()))
                .toList();
    }

    private void validatePeriodOrder(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolationException("from 은 to 보다 이전이어야 합니다.");
        }
    }

    private void validateDuplicateReservation(LocalDate date, Long timeId, Long themeId) {
        if (reservationRepository.existsByDateAndTimeIdAndThemeIdAndStatus(
                date, timeId, themeId, ReservationStatus.RESERVED)) {
            throw new DuplicateResourceException("이미 해당 날짜와 시간에 예약이 존재합니다.");
        }
    }

    private void validateDuplicateReservationExcludingSelf(Long id, ReservationRequest request) {
        if (reservationRepository.existsByDateAndTimeIdAndThemeIdAndStatusExcludingSelf(
                request.date(), request.timeId(), request.themeId(), id, ReservationStatus.RESERVED)) {
            throw new DuplicateResourceException("이미 해당 날짜와 시간에 예약이 존재합니다.");
        }
    }

    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 예약이 존재하지 않습니다. ID: " + id));
    }
}
