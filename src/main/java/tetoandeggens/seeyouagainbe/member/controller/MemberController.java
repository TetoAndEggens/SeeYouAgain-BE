package tetoandeggens.seeyouagainbe.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;
import tetoandeggens.seeyouagainbe.member.dto.request.UpdatePushEnabledRequest;
import tetoandeggens.seeyouagainbe.member.dto.response.MyInfoResponse;
import tetoandeggens.seeyouagainbe.member.service.MemberService;

@Tag(name = "Member", description = "유저 관련 API")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/me")
    @Operation(
            summary = "마이페이지 정보 조회 API",
            description = "로그인한 사용자의 닉네임과 프로필 이미지를 조회합니다."
    )
    public ApiResponse<MyInfoResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MyInfoResponse response = memberService.getMyInfo(userDetails.getMemberId());
        return ApiResponse.ok(response);
    }

    @PutMapping("/push")
    @Operation(
            summary = "푸시 알림 토글 업데이트 API",
            description = "사용자의 푸시 알림 설정을 ON/OFF 합니다."
    )
    public ApiResponse<Void> updatePushEnabled(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdatePushEnabledRequest request
    ) {
        memberService.updatePushEnabled(userDetails.getMemberId(), request);
        return ApiResponse.noContent();
    }
}
