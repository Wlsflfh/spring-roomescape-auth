package roomescape.theme.service;

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
import roomescape.reservation.domain.ReservationStatus;
import roomescape.theme.controller.dto.ThemeRequest;
import roomescape.theme.domain.Theme;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ThemeServiceTest {

    @Autowired
    private ThemeService themeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LocalDate today;
    private Long memberId;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        memberId = insertMember("testuser", "테스터", "password");
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("새로운 테마 이름이면 정상적으로 저장한다.")
        void saveSuccess() {
            ThemeRequest request = new ThemeRequest("공포의 수랏간", "매우 무섭습니다.", "https://example.com/image.png");

            Theme saved = themeService.save(request);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("공포의 수랏간");
            assertThat(themeService.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("이미 존재하는 테마 이름이면 DuplicateResourceException 이 발생한다.")
        void saveFailWhenDuplicateName() {
            themeService.save(new ThemeRequest("중복 이름", "설명", "https://example.com/a.png"));

            assertThatThrownBy(() ->
                    themeService.save(new ThemeRequest("중복 이름", "다른 설명", "https://example.com/b.png"))
            )
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("이미 존재하는 테마 이름입니다.");
        }
    }

    @Nested
    @DisplayName("getById 메서드는")
    class GetById {

        @Test
        @DisplayName("존재하는 ID 면 해당 테마를 반환한다.")
        void getByIdSuccess() {
            Theme saved = themeService.save(new ThemeRequest("테마", "설명", "https://example.com/a.png"));

            Theme result = themeService.getById(saved.getId());

            assertThat(result.getId()).isEqualTo(saved.getId());
            assertThat(result.getName()).isEqualTo("테마");
        }

        @Test
        @DisplayName("존재하지 않는 ID 면 ResourceNotFoundException 이 발생한다.")
        void getByIdFailWhenNotFound() {
            assertThatThrownBy(() -> themeService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("해당 ID의 테마가 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("deleteById 메서드는")
    class DeleteById {

        @Test
        @DisplayName("참조되지 않는 테마면 정상적으로 삭제한다.")
        void deleteByIdSuccess() {
            Theme saved = themeService.save(new ThemeRequest("테마", "설명", "https://example.com/a.png"));

            themeService.deleteById(saved.getId());

            assertThat(themeService.findAll()).isEmpty();
        }

        @Test
        @DisplayName("예약에 사용 중인 테마는 BusinessRuleViolationException 이 발생하고 삭제되지 않는다.")
        void deleteByIdFailWhenInUse() {
            Long timeId = insertReservationTime(LocalTime.of(10, 0));
            Theme saved = themeService.save(new ThemeRequest("테마", "설명", "https://example.com/a.png"));
            insertReservation(memberId, today.plusDays(1), timeId, saved.getId(), ReservationStatus.RESERVED);

            assertThatThrownBy(() -> themeService.deleteById(saved.getId()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("이 테마를 참조하는 예약이 있어 삭제할 수 없습니다.");

            assertThat(themeService.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    class FindAll {

        @Test
        @DisplayName("저장된 모든 테마를 반환한다.")
        void findAllReturnsAll() {
            themeService.save(new ThemeRequest("A", "설명A", "https://example.com/a.png"));
            themeService.save(new ThemeRequest("B", "설명B", "https://example.com/b.png"));

            List<Theme> result = themeService.findAll();

            assertThat(result).extracting(Theme::getName).containsExactlyInAnyOrder("A", "B");
        }

        @Test
        @DisplayName("테마가 없으면 빈 목록을 반환한다.")
        void findAllReturnsEmpty() {
            assertThat(themeService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPopularThemes 메서드는")
    class FindPopularThemes {

        @Test
        @DisplayName("최근 7일 이내 예약 수가 많은 테마를 내림차순으로 반환한다. (오늘은 포함되지 않는다.)")
        void findPopularThemesReturnsTopByRecentReservations() {
            LocalDate day1Ago = today.minusDays(1);
            LocalDate day7Ago = today.minusDays(7);
            Theme themeA = themeService.save(new ThemeRequest("A", "설명A", "https://example.com/a.png"));
            Theme themeB = themeService.save(new ThemeRequest("B", "설명B", "https://example.com/b.png"));
            Theme themeC = themeService.save(new ThemeRequest("C", "설명C", "https://example.com/c.png"));

            Long t10 = insertReservationTime(LocalTime.of(10, 0));
            Long t11 = insertReservationTime(LocalTime.of(11, 0));
            Long t12 = insertReservationTime(LocalTime.of(12, 0));

            insertReservation(memberId, day1Ago, t10, themeA.getId(), ReservationStatus.RESERVED);
            insertReservation(memberId, day1Ago, t11, themeA.getId(), ReservationStatus.RESERVED);

            insertReservation(memberId, day7Ago, t11, themeB.getId(), ReservationStatus.RESERVED);
            insertReservation(memberId, day7Ago, t10, themeB.getId(), ReservationStatus.RESERVED);
            insertReservation(memberId, day7Ago, t12, themeB.getId(), ReservationStatus.RESERVED);

            List<Theme> popular = themeService.findPopularThemes();

            assertThat(popular).extracting(Theme::getName).containsExactly("B", "A");
            assertThat(popular).extracting(Theme::getName).doesNotContain("C");
        }

        @Test
        @DisplayName("7일 보다 더 이전의 예약은 인기 테마 집계에서 제외된다.")
        void findPopularThemesExcludesOldReservations() {
            LocalDate day8Ago = today.minusDays(8);
            Theme theme = themeService.save(new ThemeRequest("Old", "설명", "https://example.com/o.png"));
            Long timeId = insertReservationTime(LocalTime.of(10, 0));
            insertReservation(memberId, day8Ago, timeId, theme.getId(), ReservationStatus.RESERVED);

            List<Theme> popular = themeService.findPopularThemes();

            assertThat(popular).extracting(Theme::getName).doesNotContain("Old");
        }
    }

    @Nested
    @DisplayName("update 메서드는")
    class Update {

        @Test
        @DisplayName("존재하는 ID 의 테마를 수정한다.")
        void updateSuccess() {
            Theme saved = themeService.save(new ThemeRequest("OLD", "설명", "https://example.com/a.png"));

            Theme updated = themeService.update(
                    saved.getId(),
                    new ThemeRequest("NEW", "새 설명", "https://example.com/b.png")
            );

            assertThat(updated.getId()).isEqualTo(saved.getId());
            assertThat(updated.getName()).isEqualTo("NEW");
            assertThat(themeService.getById(saved.getId()).getName()).isEqualTo("NEW");
        }

        @Test
        @DisplayName("이름을 다른 테마와 같은 값으로 변경하려 하면 DuplicateResourceException 이 발생한다.")
        void updateFailWhenDuplicateName() {
            themeService.save(new ThemeRequest("이미있음", "설명1", "https://example.com/a.png"));
            Theme target = themeService.save(new ThemeRequest("바꿀것", "설명2", "https://example.com/b.png"));

            assertThatThrownBy(() -> themeService.update(
                    target.getId(),
                    new ThemeRequest("이미있음", "설명3", "https://example.com/c.png")
            ))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("존재하지 않는 ID 면 ResourceNotFoundException 이 발생한다.")
        void updateFailWhenNotFound() {
            assertThatThrownBy(() -> themeService.update(
                    999L,
                    new ThemeRequest("X", "설명", "https://example.com/x.png")
            ))
                    .isInstanceOf(ResourceNotFoundException.class);
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

    private Long insertReservationTime(LocalTime startAt) {
        jdbcTemplate.update("INSERT INTO reservation_time (start_at) VALUES (?)", startAt.toString());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation_time WHERE start_at = ?",
                Long.class,
                startAt.toString()
        );
    }

    private void insertReservation(Long memberId, LocalDate date, Long timeId, Long themeId, ReservationStatus status) {
        jdbcTemplate.update(
                "INSERT INTO reservation (member_id, reservation_date, time_id, theme_id, status) VALUES (?, ?, ?, ?, ?)",
                memberId, date.toString(), timeId, themeId, status.name()
        );
    }
}
