package roomescape.theme.repository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.DuplicateResourceException;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.store.domain.Store;
import roomescape.theme.domain.Theme;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcThemeRepository implements ThemeRepository {

    private final RowMapper<Theme> ThemeMapper = (resultSet, rowNum) -> {
        Store store = new Store(
                resultSet.getLong("store_id"),
                resultSet.getString("store_name"),
                resultSet.getString("store_description")
        );
        return new Theme(
                resultSet.getLong("theme_id"),
                resultSet.getString("theme_name"),
                resultSet.getString("theme_description"),
                resultSet.getString("thumbnail_url"),
                store
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcThemeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Theme save(Theme theme) {
        String sql = "insert into theme (name, description, thumbnail_url, store_id) values (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, theme.getName());
                ps.setString(2, theme.getDescription());
                ps.setString(3, theme.getThumbnailUrl());
                ps.setLong(4, theme.getStore().getId());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("이미 존재하는 테마 이름입니다.");
        }

        Long generatedId = keyHolder.getKey().longValue();
        return findById(generatedId)
                .orElseThrow(() -> new IllegalStateException("서버 오류: 데이터 저장 직후 조회가 실패했습니다. (ID: " + generatedId + ")"));
    }

    @Override
    public void deleteById(Long id) {
        try {
            jdbcTemplate.update("delete from theme where id = ?", id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("예약에 사용 중인 테마는 삭제할 수 없습니다.");
        }
    }

    private static final String SELECT_WITH_STORE = """
            select
                t.id          as theme_id,
                t.name        as theme_name,
                t.description as theme_description,
                t.thumbnail_url,
                s.id          as store_id,
                s.name        as store_name,
                s.description as store_description
            from theme t
            inner join store s on t.store_id = s.id
            """;

    @Override
    public Optional<Theme> findById(Long id) {
        String sql = SELECT_WITH_STORE + "where t.id = ?";
        List<Theme> results = jdbcTemplate.query(sql, ThemeMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<Theme> findAll() {
        return jdbcTemplate.query(SELECT_WITH_STORE + "order by t.id", ThemeMapper);
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "select exists (select 1 from theme where name = ?)";
        return jdbcTemplate.queryForObject(sql, Boolean.class, name);
    }

    @Override
    public List<Theme> findPopularThemes(LocalDate startDate, LocalDate endDate, ReservationStatus status, int limit) {
        String sql = """
            select
                t.id          as theme_id,
                t.name        as theme_name,
                t.description as theme_description,
                t.thumbnail_url,
                s.id          as store_id,
                s.name        as store_name,
                s.description as store_description
            from reservation r
            inner join theme t on r.theme_id = t.id
            inner join store s on t.store_id = s.id
            where r.reservation_date >= ?
            and r.reservation_date < ?
            and r.status = ?
            group by t.id, t.name, t.description, t.thumbnail_url, s.id, s.name, s.description
            order by count(r.id) desc, t.id asc
            limit ?
            """;

        return jdbcTemplate.query(
                sql,
                ThemeMapper,
                Date.valueOf(startDate),
                Date.valueOf(endDate),
                status.name(),
                limit
        );
    }

    @Override
    public List<Theme> findByStoreIds(List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(", ", Collections.nCopies(storeIds.size(), "?"));
        String sql = SELECT_WITH_STORE + "where t.store_id in (" + placeholders + ") order by t.id";
        return jdbcTemplate.query(sql, ThemeMapper, storeIds.toArray());
    }

    @Override
    public void update(Theme theme) {
        String sql = "update theme set name = ?, description = ?, thumbnail_url = ?, store_id = ? where id = ?";
        jdbcTemplate.update(
                sql,
                theme.getName(),
                theme.getDescription(),
                theme.getThumbnailUrl(),
                theme.getStore().getId(),
                theme.getId()
        );
    }
}
