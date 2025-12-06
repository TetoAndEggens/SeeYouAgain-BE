package tetoandeggens.seeyouagainbe.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;
import tetoandeggens.seeyouagainbe.member.dto.request.UpdatePushEnabledRequest;
import tetoandeggens.seeyouagainbe.member.dto.response.UpdatePushEnabledResponse;
import tetoandeggens.seeyouagainbe.member.service.MemberService;

@Tag(name = "Member", description = "유저 관련 API")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PutMapping("/push")
    @Operation(
            summary = "푸시 알림 토글 업데이트 API",
            description = "사용자의 푸시 알림 설정을 ON/OFF 합니다."
    )
    public ApiResponse<UpdatePushEnabledResponse> updatePushEnabled(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
            @Valid @RequestBody UpdatePushEnabledRequest request
    ) {
        UpdatePushEnabledResponse response = memberService.updatePushEnabled(
                userDetails.getMemberId(),
                request
        );

        return ApiResponse.ok(response);
    }
}
