package roomescape.store.repository;

import roomescape.store.domain.Store;

import java.util.List;
import java.util.Optional;

public interface StoreRepository {

    Store save(Store store);

    Optional<Store> findById(Long id);

    List<Store> findAll();

    void deleteById(Long id);
}
