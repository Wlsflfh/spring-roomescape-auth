package roomescape.store.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.exception.ResourceNotFoundException;
import roomescape.store.domain.Store;
import roomescape.store.repository.StoreRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public Store save(String name, String description) {
        Store store = Store.create(name, description);
        return storeRepository.save(store);
    }

    public Store getById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 매장이 존재하지 않습니다. ID: " + id));
    }

    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    @Transactional
    public void deleteById(Long id) {
        storeRepository.deleteById(id);
    }
}
