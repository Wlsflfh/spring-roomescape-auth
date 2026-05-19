package roomescape.member.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.member.controller.dto.MemberRequest;
import roomescape.member.controller.dto.MemberResponse;
import roomescape.member.domain.Member;
import roomescape.member.service.MemberService;

import java.net.URI;

@Tag(name = "회원 API", description = "회원 가입 관련 API")
@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        Member member = memberService.save(request);
        MemberResponse response = MemberResponse.from(member);
        return ResponseEntity
                .created(URI.create("/members/" + response.id()))
                .body(response);
    }
}
