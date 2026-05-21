package roomescape.store.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.exception.DuplicateResourceException;
import roomescape.store.domain.Store;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcStoreRepository implements StoreRepository {

    private final RowMapper<Store> storeMapper = (resultSet, rowNum) ->
            new Store(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getString("description")
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcStoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Store save(Store store) {
        String sql = "insert into store (name, description) values (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, store.getName());
                ps.setString(2, store.getDescription());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("이미 존재하는 매장 이름입니다.");
        }

        Long generatedId = keyHolder.getKey().longValue();
        return findById(generatedId)
                .orElseThrow(() -> new IllegalStateException("서버 오류: 데이터 저장 직후 조회가 실패했습니다. (ID: " + generatedId + ")"));
    }

    @Override
    public Optional<Store> findById(Long id) {
        String sql = "select id, name, description from store where id = ?";
        List<Store> results = jdbcTemplate.query(sql, storeMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<Store> findAll() {
        return jdbcTemplate.query(
                "select id, name, description from store order by id",
                storeMapper
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("delete from store where id = ?", id);
    }
}
