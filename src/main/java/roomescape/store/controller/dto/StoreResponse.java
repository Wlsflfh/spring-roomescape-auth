package roomescape.store.controller.dto;

import roomescape.store.domain.Store;

public record StoreResponse(Long id, String name, String description) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getName(), store.getDescription());
    }
}
