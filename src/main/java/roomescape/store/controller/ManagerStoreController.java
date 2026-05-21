package roomescape.store.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.annotation.LoginManager;
import roomescape.member.domain.Member;
import roomescape.store.controller.dto.StoreResponse;
import roomescape.store.repository.ManagerStoreRepository;
import roomescape.store.service.StoreService;

import java.util.List;

@Tag(name = "매니저 매장 API", description = "매니저가 관리하는 매장 목록 조회 API")
@RestController
@RequestMapping("/manager/stores")
public class ManagerStoreController {

    private final StoreService storeService;
    private final ManagerStoreRepository managerStoreRepository;

    public ManagerStoreController(StoreService storeService, ManagerStoreRepository managerStoreRepository) {
        this.storeService = storeService;
        this.managerStoreRepository = managerStoreRepository;
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> readManagerStores(@LoginManager Member manager) {
        List<Long> storeIds = managerStoreRepository.findStoreIdsByManagerId(manager.getId());
        List<StoreResponse> responses = storeIds.stream()
                .map(storeService::getById)
                .map(StoreResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
