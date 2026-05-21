package roomescape.store.repository;

import java.util.List;

public interface ManagerStoreRepository {

    /**
     * 매니저가 관리하는 매장 ID 목록을 반환한다.
     */
    List<Long> findStoreIdsByManagerId(Long managerId);

    /**
     * 매니저-매장 매핑을 저장한다.
     */
    void save(Long managerId, Long storeId);

    /**
     * 매니저-매장 매핑을 삭제한다.
     */
    void delete(Long managerId, Long storeId);
}
