package roomescape.store.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.store.controller.dto.StoreResponse;
import roomescape.store.service.StoreService;

import java.util.List;

@Tag(name = "매장 API", description = "매장(지점) 목록 조회 API")
@RestController
@RequestMapping("/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> readAll() {
        List<StoreResponse> responses = storeService.findAll()
                .stream()
                .map(StoreResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
