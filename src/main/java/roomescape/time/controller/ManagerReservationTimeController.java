package roomescape.time.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import roomescape.auth.annotation.LoginManager;
import roomescape.member.domain.Member;
import roomescape.time.controller.dto.ReservationTimeRequest;
import roomescape.time.controller.dto.ReservationTimeResponse;
import roomescape.time.domain.ReservationTime;
import roomescape.time.service.ReservationTimeService;

import java.net.URI;
import java.util.List;

@Tag(name = "매니저 예약 시간 API", description = "매니저의 예약 시간 슬롯 관리 API")
@RestController
@RequestMapping("/manager/times")
public class ManagerReservationTimeController {

    private final ReservationTimeService reservationTimeService;

    public ManagerReservationTimeController(ReservationTimeService reservationTimeService) {
        this.reservationTimeService = reservationTimeService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationTimeResponse>> readAll(@LoginManager Member manager) {
        List<ReservationTimeResponse> responses = reservationTimeService.findAll()
                .stream()
                .map(ReservationTimeResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<ReservationTimeResponse> create(
            @Valid @RequestBody ReservationTimeRequest requestDto,
            @LoginManager Member manager
    ) {
        ReservationTime reservationTime = reservationTimeService.save(requestDto);
        ReservationTimeResponse response = ReservationTimeResponse.from(reservationTime);
        return ResponseEntity
                .created(URI.create("/times/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @LoginManager Member manager) {
        reservationTimeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
