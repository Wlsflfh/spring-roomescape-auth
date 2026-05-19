package roomescape.reservation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import roomescape.auth.annotation.LoginMember;
import roomescape.auth.annotation.LoginRequired;
import roomescape.member.domain.Member;
import roomescape.reservation.controller.dto.ReservationRequest;
import roomescape.reservation.controller.dto.ReservationResponse;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.service.ReservationService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "사용자 예약 API", description = "사용자 예약 생성, 조회 및 취소 관련 API")
@RestController
@RequestMapping("/reservations")
public class UserReservationController {

    private final ReservationService reservationService;

    public UserReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @LoginRequired
    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            @LoginMember Member member
    ) {
        Reservation reservation = reservationService.save(request, member);
        ReservationResponse response = ReservationResponse.from(reservation);
        return ResponseEntity
                .created(URI.create("/reservations/" + response.id()))
                .body(response);
    }

    @LoginRequired
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> readMyReservations(
            @LoginMember Member member,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long themeId
    ) {
        List<ReservationResponse> responses = reservationService.findByFilter(member.getId(), from, to, themeId)
                .stream()
                .map(ReservationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @LoginRequired
    @PatchMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @LoginMember Member member
    ) {
        reservationService.cancelByIdAndMember(id, member.getId());
        return ResponseEntity.noContent().build();
    }

    @LoginRequired
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request
    ) {
        Reservation reservation = reservationService.update(id, request);
        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }
}
