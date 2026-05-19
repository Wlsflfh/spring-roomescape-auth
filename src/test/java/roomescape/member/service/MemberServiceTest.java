package roomescape.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import roomescape.exception.DuplicateResourceException;
import roomescape.exception.ResourceNotFoundException;
import roomescape.member.controller.dto.MemberRequest;
import roomescape.member.domain.Member;

import java.sql.PreparedStatement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("정상 요청이면 회원을 저장하고 반환한다.")
        void saveSuccess() {
            MemberRequest request = new MemberRequest("brown", "브라운", "password1234");

            Member saved = memberService.save(request);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getLoginId()).isEqualTo("brown");
            assertThat(saved.getName()).isEqualTo("브라운");
        }

        @Test
        @DisplayName("이미 사용 중인 loginId 면 예외가 발생한다.")
        void saveFailWhenDuplicateLoginId() {
            insertMember("brown", "브라운", "password1234");

            assertThatThrownBy(() -> memberService.save(new MemberRequest("brown", "다른이름", "pw")))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("getById 메서드는")
    class GetById {

        @Test
        @DisplayName("존재하는 ID 면 해당 회원을 반환한다.")
        void getByIdSuccess() {
            Long id = insertMember("brown", "브라운", "pw");

            Member result = memberService.getById(id);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getName()).isEqualTo("브라운");
        }

        @Test
        @DisplayName("존재하지 않는 ID 면 예외가 발생한다.")
        void getByIdFailWhenNotFound() {
            assertThatThrownBy(() -> memberService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getByLoginId 메서드는")
    class GetByLoginId {

        @Test
        @DisplayName("존재하는 loginId 면 해당 회원을 반환한다.")
        void getByLoginIdSuccess() {
            insertMember("brown", "브라운", "pw");

            Member result = memberService.getByLoginId("brown");

            assertThat(result.getLoginId()).isEqualTo("brown");
            assertThat(result.getName()).isEqualTo("브라운");
        }

        @Test
        @DisplayName("존재하지 않는 loginId 면 예외가 발생한다.")
        void getByLoginIdFailWhenNotFound() {
            assertThatThrownBy(() -> memberService.getByLoginId("ghost"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchByName 메서드는")
    class SearchByName {

        @Test
        @DisplayName("이름에 키워드가 포함된 회원 목록을 반환한다.")
        void searchByNameReturnsMatches() {
            insertMember("brown", "브라운", "pw1");
            insertMember("james", "제임스", "pw2");
            insertMember("pobi", "포비", "pw3");

            List<Member> result = memberService.searchByName("브라운");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("브라운");
        }

        @Test
        @DisplayName("부분 이름으로도 검색된다.")
        void searchByNamePartialMatch() {
            insertMember("brown", "브라운", "pw1");
            insertMember("brownie", "브라우니", "pw2");
            insertMember("james", "제임스", "pw3");

            List<Member> result = memberService.searchByName("브라");

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Member::getName)
                    .containsExactlyInAnyOrder("브라운", "브라우니");
        }

        @Test
        @DisplayName("일치하는 회원이 없으면 빈 목록을 반환한다.")
        void searchByNameReturnsEmpty() {
            insertMember("brown", "브라운", "pw1");

            List<Member> result = memberService.searchByName("없는이름");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열로 검색하면 전체 회원 목록을 반환한다.")
        void searchByNameWithEmptyReturnsAll() {
            insertMember("brown", "브라운", "pw1");
            insertMember("james", "제임스", "pw2");

            List<Member> result = memberService.searchByName("");

            assertThat(result).hasSize(2);
        }
    }

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
}
