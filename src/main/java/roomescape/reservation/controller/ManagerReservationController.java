package roomescape.reservation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import roomescape.auth.annotation.LoginManager;
import roomescape.member.domain.Member;
import roomescape.reservation.controller.dto.ReservationRequest;
import roomescape.reservation.controller.dto.ReservationResponse;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.service.ReservationService;

import java.util.List;

@Tag(name = "매니저 예약 API", description = "매장 매니저의 예약 조회, 수정, 취소 관련 API")
@RestController
@RequestMapping("/manager/reservations")
public class ManagerReservationController {

    private final ReservationService reservationService;

    public ManagerReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> readStoreReservations(
            @LoginManager Member manager
    ) {
        List<ReservationResponse> responses = reservationService.findByManager(manager.getId())
                .stream()
                .map(ReservationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @LoginManager Member manager
    ) {
        reservationService.cancelByIdAndManager(id, manager.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request,
            @LoginManager Member manager
    ) {
        Reservation reservation = reservationService.updateByManager(id, request, manager.getId());
        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }
}
