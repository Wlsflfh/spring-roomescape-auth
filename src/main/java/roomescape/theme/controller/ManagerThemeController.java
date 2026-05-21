package roomescape.theme.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import roomescape.auth.annotation.LoginManager;
import roomescape.exception.ForbiddenException;
import roomescape.member.domain.Member;
import roomescape.store.repository.ManagerStoreRepository;
import roomescape.theme.controller.dto.ThemeRequest;
import roomescape.theme.controller.dto.ThemeResponse;
import roomescape.theme.domain.Theme;
import roomescape.theme.service.ThemeService;

import java.net.URI;
import java.util.List;

@Tag(name = "매니저 테마 API", description = "매니저의 매장 테마 생성, 수정, 삭제 관련 API")
@RestController
@RequestMapping("/manager/themes")
public class ManagerThemeController {

    private final ThemeService themeService;
    private final ManagerStoreRepository managerStoreRepository;

    public ManagerThemeController(ThemeService themeService, ManagerStoreRepository managerStoreRepository) {
        this.themeService = themeService;
        this.managerStoreRepository = managerStoreRepository;
    }

    @GetMapping
    public ResponseEntity<List<ThemeResponse>> readManagerThemes(@LoginManager Member manager) {
        List<Long> storeIds = managerStoreRepository.findStoreIdsByManagerId(manager.getId());
        List<ThemeResponse> responses = themeService.findByStoreIds(storeIds)
                .stream()
                .map(ThemeResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<ThemeResponse> create(
            @Valid @RequestBody ThemeRequest request,
            @LoginManager Member manager
    ) {
        List<Long> storeIds = managerStoreRepository.findStoreIdsByManagerId(manager.getId());
        if (!storeIds.contains(request.storeId())) {
            throw new ForbiddenException("해당 매장에 대한 테마 생성 권한이 없습니다.");
        }
        Theme theme = themeService.save(request);
        ThemeResponse response = ThemeResponse.from(theme);
        return ResponseEntity
                .created(URI.create("/themes/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThemeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ThemeRequest request,
            @LoginManager Member manager
    ) {
        List<Long> storeIds = managerStoreRepository.findStoreIdsByManagerId(manager.getId());
        Theme existing = themeService.getById(id);
        if (!storeIds.contains(existing.getStore().getId())) {
            throw new ForbiddenException("해당 테마에 대한 수정 권한이 없습니다.");
        }
        if (!storeIds.contains(request.storeId())) {
            throw new ForbiddenException("해당 매장에 대한 권한이 없습니다.");
        }
        Theme theme = themeService.update(id, request);
        return ResponseEntity.ok(ThemeResponse.from(theme));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @LoginManager Member manager
    ) {
        List<Long> storeIds = managerStoreRepository.findStoreIdsByManagerId(manager.getId());
        Theme existing = themeService.getById(id);
        if (!storeIds.contains(existing.getStore().getId())) {
            throw new ForbiddenException("해당 테마에 대한 삭제 권한이 없습니다.");
        }
        themeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
