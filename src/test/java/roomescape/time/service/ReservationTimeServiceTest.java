package roomescape.time.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import roomescape.exception.BusinessRuleViolationException;
import roomescape.exception.DuplicateResourceException;
import roomescape.exception.ResourceNotFoundException;
import roomescape.time.controller.dto.ReservationTimeRequest;
import roomescape.time.domain.ReservationTime;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationTimeServiceTest {

    @Autowired
    private ReservationTimeService reservationTimeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = insertMember("testuser", "테스터", "password");
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("새로운 예약 시간이면 정상적으로 저장한다.")
        void saveSuccess() {
            ReservationTimeRequest request = new ReservationTimeRequest(LocalTime.of(10, 0));

            ReservationTime saved = reservationTimeService.save(request);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getStartAt()).isEqualTo(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("이미 존재하는 시간이면 DuplicateResourceException 이 발생한다.")
        void saveFailWhenDuplicate() {
            reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(10, 0)));

            assertThatThrownBy(() ->
                    reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(10, 0)))
            )
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("해당 시간이 이미 존재합니다.");
        }
    }

    @Nested
    @DisplayName("getById 메서드는")
    class GetById {

        @Test
        @DisplayName("존재하는 ID 면 해당 시간을 반환한다.")
        void getByIdSuccess() {
            ReservationTime saved = reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(10, 0)));

            ReservationTime result = reservationTimeService.getById(saved.getId());

            assertThat(result.getStartAt()).isEqualTo(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("존재하지 않는 ID 면 ResourceNotFoundException 이 발생한다.")
        void getByIdFailWhenNotFound() {
            assertThatThrownBy(() -> reservationTimeService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("해당 ID의 예약 시간이 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("deleteById 메서드는")
    class DeleteById {

        @Test
        @DisplayName("참조되지 않는 시간은 정상적으로 삭제한다.")
        void deleteByIdSuccess() {
            ReservationTime saved = reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(10, 0)));

            reservationTimeService.deleteById(saved.getId());

            assertThat(reservationTimeService.findAll()).isEmpty();
        }

        @Test
        @DisplayName("예약에 사용 중인 시간은 BusinessRuleViolationException 이 발생하고 삭제되지 않는다.")
        void deleteByIdFailWhenInUse() {
            ReservationTime savedTime = reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(10, 0)));
            Long themeId = insertTheme("테마", "설명", "https://example.com/a.png");
            insertReservation(memberId, LocalDate.of(2026, 12, 31), savedTime.getId(), themeId);

            assertThatThrownBy(() -> reservationTimeService.deleteById(savedTime.getId()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("이 시간을 참조하는 예약이 있어 삭제할 수 없습니다.");

            assertThat(reservationTimeService.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    class FindAll {

        @Test
        @DisplayName("저장된 모든 시간을 시작 시각 오름차순으로 반환한다.")
        void findAllReturnsSorted() {
            reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(13, 0)));
            reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(10, 0)));

            List<ReservationTime> result = reservationTimeService.findAll();

            assertThat(result).extracting(ReservationTime::getStartAt)
                    .containsExactly(LocalTime.of(10, 0), LocalTime.of(13, 0));
        }

        @Test
        @DisplayName("등록된 시간이 없으면 빈 목록을 반환한다.")
        void findAllReturnsEmpty() {
            assertThat(reservationTimeService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAvailableTimes 메서드는")
    class FindAvailableTimes {

        @Test
        @DisplayName("주어진 날짜/테마에 이미 예약된 시간은 제외하고 반환한다.")
        void findAvailableTimesExcludesBooked() {
            ReservationTime t10 = reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(10, 0)));
            ReservationTime t11 = reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(11, 0)));
            ReservationTime t12 = reservationTimeService.save(new ReservationTimeRequest(LocalTime.of(12, 0)));

            Long themeId = insertTheme("테마", "설명", "https://example.com/a.png");
            LocalDate date = LocalDate.of(2026, 12, 31);
            insertReservation(memberId, date, t11.getId(), themeId);

            List<ReservationTime> available = reservationTimeService.findAvailableTimes(themeId, date);

            assertThat(available).extracting(ReservationTime::getStartAt)
                    .containsExactly(LocalTime.of(10, 0), LocalTime.of(12, 0));
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Long insertMember(String loginId, String name, String password) {
        String sql = "INSERT INTO member (login_id, name, password) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, loginId);
            ps.setString(2, name);
            ps.setString(3, password);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private Long insertTheme(String name, String description, String thumbnailUrl) {
        jdbcTemplate.update(
                "INSERT INTO theme (name, description, thumbnail_url) VALUES (?, ?, ?)",
                name, description, thumbnailUrl
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class,
                name
        );
    }

    private void insertReservation(Long memberId, LocalDate date, Long timeId, Long themeId) {
        jdbcTemplate.update(
                "INSERT INTO reservation (member_id, reservation_date, time_id, theme_id) VALUES (?, ?, ?, ?)",
                memberId, date.toString(), timeId, themeId
        );
    }
}
