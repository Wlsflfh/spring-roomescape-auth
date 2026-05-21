package roomescape.reservation.service;

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
import roomescape.exception.ForbiddenException;
import roomescape.member.domain.Member;
import roomescape.member.domain.MemberRole;
import roomescape.reservation.controller.dto.ReservationRequest;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LocalDateTime futureDate;
    private LocalDateTime today;
    private LocalDateTime pastDate;
    private Long themeId;
    private Member member;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        today = LocalDateTime.now().withNano(0);
        futureDate = today.plusDays(1);
        pastDate = today.minusDays(1);
        Long storeId = insertStore("테스트 매장", "테스트용 매장");
        themeId = insertTheme("우테코", "우테코 전용 테마", "https://example.com/thumb.jpg", storeId);

        Long memberId = insertMember("brown", "브라운", "password1");
        Long otherMemberId = insertMember("james", "제임스", "password2");
        member = new Member(memberId, "brown", "브라운", "password1", MemberRole.MEMBER);
        otherMember = new Member(otherMemberId, "james", "제임스", "password2", MemberRole.MEMBER);
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("정상 요청이면 예약을 저장하고 생성된 예약을 반환한다.")
        void saveSuccess() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            ReservationRequest request = new ReservationRequest(reservationDate, timeId, themeId);

            Reservation saved = reservationService.save(request, member);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getMember().getId()).isEqualTo(member.getId());
            assertThat(saved.getDate()).isEqualTo(reservationDate);
            assertThat(saved.getTime().getId()).isEqualTo(timeId);
            assertThat(saved.getTheme().getId()).isEqualTo(themeId);
        }

        @Test
        @DisplayName("예약 일자가 과거이면 BusinessRuleViolationException 이 발생하고 저장되지 않는다.")
        void saveFailWhenPastDate() {
            LocalDate reservationDate = pastDate.toLocalDate();
            Long timeId = insertReservationTime(pastDate.toLocalTime());
            ReservationRequest request = new ReservationRequest(reservationDate, timeId, themeId);

            assertThatThrownBy(() -> reservationService.save(request, member))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("오늘 날짜라도 예약 시간이 현재 시각보다 이전이면 BusinessRuleViolationException 예외가 발생한다.")
        void saveFailWhenTodayButPastTime() {
            LocalDate reservationDate = today.toLocalDate();
            Long timeId = insertReservationTime(today.toLocalTime().minusMinutes(30));
            ReservationRequest request = new ReservationRequest(reservationDate, timeId, themeId);

            assertThatThrownBy(() -> reservationService.save(request, member))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("동일한 날짜/시간/테마 조합의 예약이 이미 존재하면 DuplicateResourceException 이 발생한다.")
        void saveFailWhenDuplicate() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            ReservationRequest request = new ReservationRequest(reservationDate, timeId, themeId);
            reservationService.save(request, member);

            ReservationRequest duplicate = new ReservationRequest(reservationDate, timeId, themeId);

            assertThatThrownBy(() -> reservationService.save(duplicate, otherMember))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("cancelById 메서드는")
    class CancelById {

        @Test
        @DisplayName("ID에 해당하는 예약의 상태를 CANCELED 로 변경한다.")
        void cancelByIdRemovesReservation() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            Reservation saved = reservationService.save(new ReservationRequest(reservationDate, timeId, themeId), member);

            reservationService.cancelById(saved.getId());
            List<Reservation> results = reservationService.findByFilter(null, null, null, null);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(ReservationStatus.CANCELED);
        }

        @Test
        @DisplayName("이미 완료된 예약에 대해서 취소 요청 시, 예외를 발생한다.")
        void cancelCompleted() {
            LocalDate reservationDate = pastDate.toLocalDate();
            Long timeId = insertReservationTime(pastDate.toLocalTime());
            Long id = insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            assertThatThrownBy(() -> reservationService.cancelById(id))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("이미 CANCELED 상태인 예약을 취소 요청 시, 멱등성을 보장한다.")
        void alreadyCanceled() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            Reservation saved = reservationService.save(new ReservationRequest(reservationDate, timeId, themeId), member);

            reservationService.cancelById(saved.getId());
            reservationService.cancelById(saved.getId());
            reservationService.cancelById(saved.getId());

            List<Reservation> results = reservationService.findByFilter(null, null, null, null);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(ReservationStatus.CANCELED);
        }
    }

    @Nested
    @DisplayName("findByFilter 메서드는")
    class FindByFilter {

        @Test
        @DisplayName("검색 조건을 입력하지 않으면 전체 데이터를 반환한다.")
        void findAll() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            reservationService.save(new ReservationRequest(reservationDate, timeId, themeId), member);

            Long storeId2 = insertStore("테스트 매장2", "테스트용 매장2");
            Long themeId2 = insertTheme("테마2", "설명2", "https://example.com/2.jpg", storeId2);
            reservationService.save(new ReservationRequest(reservationDate.plusDays(1), timeId, themeId2), member);
            reservationService.save(new ReservationRequest(reservationDate.plusDays(2), timeId, themeId), otherMember);

            List<Reservation> results = reservationService.findByFilter(null, null, null, null);

            assertThat(results).hasSize(3);
        }

        @Test
        @DisplayName("memberId 조건으로 해당 회원의 예약만 필터링하여 반환한다.")
        void searchByMember() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());

            reservationService.save(new ReservationRequest(reservationDate, timeId, themeId), member);
            reservationService.save(new ReservationRequest(reservationDate.plusDays(1), timeId, themeId), member);

            Long storeId2 = insertStore("테스트 매장2-b", "테스트용 매장2-b");
            Long themeId2 = insertTheme("테마2", "설명2", "https://example.com/2.jpg", storeId2);
            reservationService.save(new ReservationRequest(reservationDate.plusDays(2), timeId, themeId2), otherMember);

            List<Reservation> results = reservationService.findByFilter(member.getId(), null, null, null);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(r -> r.getMember().getId()).containsOnly(member.getId());
        }

        @Test
        @DisplayName("모든 검색 조건이 다 맞아야 해당하는 예약을 필터링하여 반환한다.")
        void searchWithAllFilters() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());

            reservationService.save(new ReservationRequest(reservationDate, timeId, themeId), member);
            reservationService.save(new ReservationRequest(reservationDate.plusDays(1), timeId, themeId), otherMember);

            List<Reservation> results = reservationService.findByFilter(
                    member.getId(), reservationDate, reservationDate.plusDays(1), themeId);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMember().getId()).isEqualTo(member.getId());
            assertThat(results.get(0).getTheme().getId()).isEqualTo(themeId);
        }

        @Test
        @DisplayName("시작 날짜(from)가 종료 날짜(to)보다 미래이면 BusinessRuleViolationException 이 발생한다.")
        void searchFailWhenFromAfterTo() {
            LocalDate todayDate = futureDate.toLocalDate();
            LocalDate tomorrowDate = futureDate.toLocalDate().plusDays(1);

            assertThatThrownBy(() -> reservationService.findByFilter(null, tomorrowDate, todayDate, null))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("update 메서드는")
    class Update {

        @Test
        @DisplayName("정상 요청이면 예약을 변경하고 변경된 예약을 반환한다.")
        void updateSuccess() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            Long generatedId = insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            ReservationRequest request = new ReservationRequest(reservationDate, timeId, themeId);
            Reservation updated = reservationService.update(generatedId, request);

            assertThat(updated.getId()).isEqualTo(generatedId);
            assertThat(updated.getMember().getId()).isEqualTo(member.getId());
            assertThat(updated.getDate()).isEqualTo(reservationDate);
            assertThat(updated.getTime().getId()).isEqualTo(timeId);
            assertThat(updated.getTheme().getId()).isEqualTo(themeId);
        }

        @Test
        @DisplayName("예약 일자가 과거이면 BusinessRuleViolationException 이 발생하고 변경되지 않는다.")
        void updateFailWhenPastDate() {
            LocalDate reservationDate = pastDate.toLocalDate();
            Long timeId = insertReservationTime(pastDate.toLocalTime());
            Long generatedId = insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            ReservationRequest request = new ReservationRequest(reservationDate, timeId, themeId);

            assertThatThrownBy(() -> reservationService.update(generatedId, request))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("동일한 날짜/시간/테마 조합의 예약이 이미 존재하면 DuplicateResourceException 이 발생한다.")
        void updateFailWhenDuplicate() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId1 = insertReservationTime(futureDate.toLocalTime());
            Long brownId = insertReservation(member.getId(), reservationDate, timeId1, themeId, ReservationStatus.RESERVED);

            Long timeId2 = insertReservationTime(futureDate.toLocalTime().minusHours(1));
            Long jamesId = insertReservation(otherMember.getId(), reservationDate, timeId2, themeId, ReservationStatus.RESERVED);

            ReservationRequest updateToBrownTime = new ReservationRequest(reservationDate, timeId1, themeId);

            assertThatThrownBy(() -> reservationService.update(jamesId, updateToBrownTime))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("취소된 슬롯은 예약 가능하다.")
        void updateWhenSlotIsCanceled() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId1 = insertReservationTime(futureDate.toLocalTime());
            Long brownId = insertReservation(member.getId(), reservationDate, timeId1, themeId, ReservationStatus.CANCELED);

            Long timeId2 = insertReservationTime(futureDate.toLocalTime().minusHours(1));
            Long jamesId = insertReservation(otherMember.getId(), reservationDate, timeId2, themeId, ReservationStatus.RESERVED);

            ReservationRequest updateToBrownTime = new ReservationRequest(reservationDate, timeId1, themeId);

            assertThatCode(() -> reservationService.update(jamesId, updateToBrownTime))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("findByManager 메서드는")
    class FindByManager {

        @Test
        @DisplayName("매니저가 관리하는 매장의 예약 목록을 반환한다.")
        void returnsReservationsForManagerStores() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());

            // 매니저 — 테스트 매장(themeId 소속)만 관리
            Long managerId = insertManagerMember("mgr1", "매니저1", "pw");
            Long storeId = jdbcTemplate.queryForObject(
                    "SELECT store_id FROM theme WHERE id = ?", Long.class, themeId);
            insertManagerStore(managerId, storeId);

            insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            // 다른 매장 예약 — 매니저가 관리하지 않음
            Long otherStoreId = insertStore("다른 매장", "다른 설명");
            Long otherThemeId = insertTheme("다른 테마", "설명", "https://example.com/t.jpg", otherStoreId);
            insertReservation(member.getId(), reservationDate.plusDays(1), timeId, otherThemeId, ReservationStatus.RESERVED);

            List<Reservation> results = reservationService.findByManager(managerId);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTheme().getStore().getId()).isEqualTo(storeId);
        }

        @Test
        @DisplayName("관리하는 매장이 없으면 빈 목록을 반환한다.")
        void returnsEmptyWhenNoStores() {
            Long managerId = insertManagerMember("mgr2", "매니저2", "pw");

            List<Reservation> results = reservationService.findByManager(managerId);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("cancelByIdAndManager 메서드는")
    class CancelByIdAndManager {

        @Test
        @DisplayName("매니저가 관리하는 매장의 예약을 취소할 수 있다.")
        void cancelSuccess() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            Long reservationId = insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            Long managerId = insertManagerMember("mgr3", "매니저3", "pw");
            Long storeId = jdbcTemplate.queryForObject(
                    "SELECT store_id FROM theme WHERE id = ?", Long.class, themeId);
            insertManagerStore(managerId, storeId);

            reservationService.cancelByIdAndManager(reservationId, managerId);

            Reservation canceled = reservationService.getById(reservationId);
            assertThat(canceled.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        }

        @Test
        @DisplayName("관리하지 않는 매장의 예약을 취소하려 하면 ForbiddenException 이 발생한다.")
        void cancelFailWhenNotManagerStore() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            Long reservationId = insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            Long otherStoreId = insertStore("다른 매장2", "설명");
            Long managerId = insertManagerMember("mgr4", "매니저4", "pw");
            insertManagerStore(managerId, otherStoreId); // 다른 매장만 관리

            assertThatThrownBy(() -> reservationService.cancelByIdAndManager(reservationId, managerId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("updateByManager 메서드는")
    class UpdateByManager {

        @Test
        @DisplayName("매니저가 관리하는 매장의 예약을 수정할 수 있다.")
        void updateSuccess() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            Long reservationId = insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            Long managerId = insertManagerMember("mgr5", "매니저5", "pw");
            Long storeId = jdbcTemplate.queryForObject(
                    "SELECT store_id FROM theme WHERE id = ?", Long.class, themeId);
            insertManagerStore(managerId, storeId);

            Long newTimeId = insertReservationTime(futureDate.toLocalTime().plusHours(2));
            ReservationRequest request = new ReservationRequest(reservationDate.plusDays(1), newTimeId, themeId);

            Reservation updated = reservationService.updateByManager(reservationId, request, managerId);

            assertThat(updated.getDate()).isEqualTo(reservationDate.plusDays(1));
            assertThat(updated.getTime().getId()).isEqualTo(newTimeId);
        }

        @Test
        @DisplayName("관리하지 않는 매장의 예약을 수정하려 하면 ForbiddenException 이 발생한다.")
        void updateFailWhenNotManagerStore() {
            LocalDate reservationDate = futureDate.toLocalDate();
            Long timeId = insertReservationTime(futureDate.toLocalTime());
            Long reservationId = insertReservation(member.getId(), reservationDate, timeId, themeId, ReservationStatus.RESERVED);

            Long otherStoreId = insertStore("다른 매장3", "설명");
            Long managerId = insertManagerMember("mgr6", "매니저6", "pw");
            insertManagerStore(managerId, otherStoreId);

            ReservationRequest request = new ReservationRequest(reservationDate.plusDays(1), timeId, themeId);

            assertThatThrownBy(() -> reservationService.updateByManager(reservationId, request, managerId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    private Long insertReservation(Long memberId, LocalDate date, Long timeId, Long themeId, ReservationStatus status) {
        String sql = "INSERT INTO reservation (member_id, reservation_date, time_id, theme_id, status) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, memberId);
            ps.setDate(2, Date.valueOf(date));
            ps.setLong(3, timeId);
            ps.setLong(4, themeId);
            ps.setString(5, status.name());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private Long insertMember(String loginId, String name, String password) {
        String sql = "INSERT INTO member (login_id, name, password, role) VALUES (?, ?, ?, 'MEMBER')";
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

    private Long insertStore(String name, String description) {
        String sql = "INSERT INTO store (name, description) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, name);
            ps.setString(2, description);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private Long insertReservationTime(LocalTime startAt) {
        jdbcTemplate.update("INSERT INTO reservation_time (start_at) VALUES (?)", startAt);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation_time WHERE start_at = ?",
                Long.class,
                startAt
        );
    }

    private Long insertTheme(String name, String description, String thumbnailUrl, Long storeId) {
        jdbcTemplate.update(
                "INSERT INTO theme (name, description, thumbnail_url, store_id) VALUES (?, ?, ?, ?)",
                name, description, thumbnailUrl, storeId
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class,
                name
        );
    }

    private Long insertManagerMember(String loginId, String name, String password) {
        String sql = "INSERT INTO member (login_id, name, password, role) VALUES (?, ?, ?, 'MANAGER')";
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

    private void insertManagerStore(Long managerId, Long storeId) {
        jdbcTemplate.update(
                "INSERT INTO manager_store (manager_id, store_id) VALUES (?, ?)",
                managerId, storeId
        );
    }
}
