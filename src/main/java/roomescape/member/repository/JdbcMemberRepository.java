package roomescape.member.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.exception.DuplicateResourceException;
import roomescape.member.domain.Member;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMemberRepository implements MemberRepository {

    private final RowMapper<Member> memberMapper = (rs, rowNum) ->
            new Member(
                    rs.getLong("id"),
                    rs.getString("login_id"),
                    rs.getString("name"),
                    rs.getString("password")
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Member save(Member member) {
        String sql = "insert into member (login_id, name, password) values (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, member.getLoginId());
                ps.setString(2, member.getName());
                ps.setString(3, member.getPassword());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("이미 사용 중인 로그인 ID입니다.");
        }

        Long generatedId = keyHolder.getKey().longValue();
        return findById(generatedId)
                .orElseThrow(() -> new IllegalStateException("서버 오류: 데이터 저장 직후 조회가 실패했습니다. (ID: " + generatedId + ")"));
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "select id, login_id, name, password from member where id = ?";
        List<Member> results = jdbcTemplate.query(sql, memberMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public Optional<Member> findByLoginId(String loginId) {
        String sql = "select id, login_id, name, password from member where login_id = ?";
        List<Member> results = jdbcTemplate.query(sql, memberMapper, loginId);
        return results.stream().findFirst();
    }

    @Override
    public List<Member> findAllByName(String name) {
        String sql = "select id, login_id, name, password from member where name like ?";
        return jdbcTemplate.query(sql, memberMapper, "%" + name + "%");
    }
}
