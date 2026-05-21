package roomescape.store.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcManagerStoreRepository implements ManagerStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcManagerStoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Long> findStoreIdsByManagerId(Long managerId) {
        String sql = "select store_id from manager_store where manager_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, managerId);
    }

    @Override
    public void save(Long managerId, Long storeId) {
        jdbcTemplate.update(
                "insert into manager_store (manager_id, store_id) values (?, ?)",
                managerId, storeId
        );
    }

    @Override
    public void delete(Long managerId, Long storeId) {
        jdbcTemplate.update(
                "delete from manager_store where manager_id = ? and store_id = ?",
                managerId, storeId
        );
    }
}
