package roomescape.member.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import roomescape.member.controller.dto.MemberResponse;
import roomescape.member.service.MemberService;

import java.util.List;

@Tag(name = "관리자 회원 API", description = "관리자용 회원 검색 API")
@RestController
@RequestMapping("/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> searchByName(
            @RequestParam(defaultValue = "") String name
    ) {
        List<MemberResponse> responses = memberService.searchByName(name)
                .stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
