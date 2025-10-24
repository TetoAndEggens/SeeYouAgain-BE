package tetoandeggens.seeyouagainbe.member.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "유저 관련 API")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
}
