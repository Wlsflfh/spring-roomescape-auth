package roomescape.reservation.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.member.domain.Member;
import roomescape.member.domain.MemberRole;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.store.domain.Store;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcReservationRepository implements ReservationRepository {

    private final RowMapper<Reservation> reservationRowMapper = (rs, rowNum) -> {
        ReservationTime time = new ReservationTime(
                rs.getLong("time_id"),
                rs.getTime("time_start_at").toLocalTime()
        );

        Store store = new Store(
                rs.getLong("store_id"),
                rs.getString("store_name"),
                rs.getString("store_description")
        );

        Theme theme = new Theme(
                rs.getLong("theme_id"),
                rs.getString("theme_name"),
                rs.getString("theme_description"),
                rs.getString("theme_thumbnail_url"),
                store
        );

        Member member = new Member(
                rs.getLong("member_id"),
                rs.getString("member_login_id"),
                rs.getString("member_name"),
                rs.getString("member_password"),
                MemberRole.valueOf(rs.getString("member_role"))
        );

        return new Reservation(
                rs.getLong("reservation_id"),
                member,
                rs.getDate("reservation_date").toLocalDate(),
                time,
                theme,
                ReservationStatus.valueOf(rs.getString("reservation_status"))
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Reservation save(Reservation reservation) {
        String sql = "insert into reservation (member_id, reservation_date, time_id, theme_id, status) values (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, reservation.getMember().getId());
            ps.setDate(2, Date.valueOf(reservation.getDate()));
            ps.setLong(3, reservation.getTime().getId());
            ps.setLong(4, reservation.getTheme().getId());
            ps.setString(5, reservation.getStatus().name());
            return ps;
        }, keyHolder);

        long generatedId = keyHolder.getKey().longValue();
        return findById(generatedId)
                .orElseThrow(() -> new IllegalStateException("서버 오류: 데이터 저장 직후 조회가 실패했습니다. (ID: " + generatedId + ")"));
    }

    @Override
    public void update(Reservation reservation) {
        String sql = """
                update reservation
                   set reservation_date = ?,
                       time_id          = ?,
                       theme_id         = ?
                 where id = ?
                """;
        jdbcTemplate.update(
                sql,
                Date.valueOf(reservation.getDate()),
                reservation.getTime().getId(),
                reservation.getTheme().getId(),
                reservation.getId()
        );
    }

    @Override
    public void updateStatus(Long id, ReservationStatus status) {
        jdbcTemplate.update("update reservation set status = ? where id = ?", status.name(), id);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = """
                select
                    r.id               as reservation_id,
                    r.reservation_date,
                    r.status           as reservation_status,
                    t.id               as time_id,
                    t.start_at         as time_start_at,
                    h.id               as theme_id,
                    h.name             as theme_name,
                    h.description      as theme_description,
                    h.thumbnail_url    as theme_thumbnail_url,
                    s.id               as store_id,
                    s.name             as store_name,
                    s.description      as store_description,
                    m.id               as member_id,
                    m.login_id         as member_login_id,
                    m.name             as member_name,
                    m.password         as member_password,
                    m.role             as member_role
                from reservation r
                inner join reservation_time t on r.time_id   = t.id
                inner join theme            h on r.theme_id  = h.id
                inner join store            s on h.store_id  = s.id
                inner join member           m on r.member_id = m.id
                where r.id = ?
                """;
        List<Reservation> results = jdbcTemplate.query(sql, reservationRowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<Reservation> findByFilter(Long memberId, LocalDate from, LocalDate to, Long themeId) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                """
                select
                    r.id               as reservation_id,
                    r.reservation_date,
                    r.status           as reservation_status,
                    t.id               as time_id,
                    t.start_at         as time_start_at,
                    h.id               as theme_id,
                    h.name             as theme_name,
                    h.description      as theme_description,
                    h.thumbnail_url    as theme_thumbnail_url,
                    s.id               as store_id,
                    s.name             as store_name,
                    s.description      as store_description,
                    m.id               as member_id,
                    m.login_id         as member_login_id,
                    m.name             as member_name,
                    m.password         as member_password,
                    m.role             as member_role
                from reservation r
                inner join reservation_time t on r.time_id   = t.id
                inner join theme            h on r.theme_id  = h.id
                inner join store            s on h.store_id  = s.id
                inner join member           m on r.member_id = m.id
                where 1=1
                """
        );

        if (memberId != null) {
            sql.append(" and r.member_id = ?");
            params.add(memberId);
        }
        if (from != null) {
            sql.append(" and r.reservation_date >= ?");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" and r.reservation_date <= ?");
            params.add(Date.valueOf(to));
        }
        if (themeId != null) {
            sql.append(" and r.theme_id = ?");
            params.add(themeId);
        }
        sql.append(" order by r.reservation_date desc, t.start_at desc");

        return jdbcTemplate.query(sql.toString(), reservationRowMapper, params.toArray());
    }

    @Override
    public boolean existsByDateAndTimeIdAndThemeIdAndStatus(
            LocalDate date, Long timeId, Long themeId, ReservationStatus status
    ) {
        String sql = """
                select exists (
                    select 1 from reservation
                    where reservation_date = ?
                      and time_id  = ?
                      and theme_id = ?
                      and status   = ?
                )
                """;
        return jdbcTemplate.queryForObject(sql, Boolean.class, date, timeId, themeId, status.name());
    }

    @Override
    public boolean existsByDateAndTimeIdAndThemeIdAndStatusExcludingSelf(
            LocalDate date, Long timeId, Long themeId, Long excludeId, ReservationStatus status
    ) {
        String sql = """
                select exists (
                    select 1 from reservation
                    where reservation_date = ?
                      and time_id  = ?
                      and theme_id = ?
                      and id      != ?
                      and status   = ?
                )
                """;
        return jdbcTemplate.queryForObject(sql, Boolean.class, date, timeId, themeId, excludeId, status.name());
    }

    @Override
    public boolean existsByTimeId(Long timeId) {
        return jdbcTemplate.queryForObject(
                "select exists (select 1 from reservation where time_id = ?)", Boolean.class, timeId);
    }

    @Override
    public boolean existsByThemeId(Long themeId) {
        return jdbcTemplate.queryForObject(
                "select exists (select 1 from reservation where theme_id = ?)", Boolean.class, themeId);
    }

}
