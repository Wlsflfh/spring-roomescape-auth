package roomescape.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.exception.ResourceNotFoundException;
import roomescape.member.controller.dto.MemberRequest;
import roomescape.member.domain.Member;
import roomescape.member.repository.MemberRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member save(MemberRequest request) {
        Member member = Member.create(request.loginId(), request.name(), request.password());
        return memberRepository.save(member);
    }

    public Member getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 회원이 존재하지 않습니다. ID: " + id));
    }

    public Member getByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 로그인 ID의 회원이 존재하지 않습니다."));
    }

    public List<Member> searchByName(String name) {
        return memberRepository.findAllByName(name);
    }
}
