package tetoandeggens.seeyouagainbe.fcm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.fcm.dto.request.FcmTokenRequest;
import tetoandeggens.seeyouagainbe.fcm.dto.response.FcmTokenResponse;
import tetoandeggens.seeyouagainbe.fcm.service.FcmTokenService;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

import java.util.List;

@Tag(name = "Fcm", description = "FCM 등록 관련 API")
@RestController
@RequestMapping("/fcm/tokens")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    @Operation(
            summary = "FCM 토큰 등록 API",
            description = "클라이언트로부터 FCM 토큰을 받아 저장 또는 업데이트"
    )
    public ApiResponse<FcmTokenResponse> registerToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        FcmTokenResponse response = fcmTokenService.saveOrUpdateToken(
                userDetails.getMemberId(),
                request,
                userAgent
        );
        return ApiResponse.created(response);
    }

    @PutMapping("/{deviceId}/refresh")
    @Operation(
            summary = "FCM 토큰 갱신 API",
            description = "FCM 토큰의 마지막 사용 시간을 갱신 (30일 지났을 경우)"
    )
    public ApiResponse<Void> refreshToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String deviceId
    ) {
        fcmTokenService.refreshTokenIfNeeded(userDetails.getMemberId(), deviceId);
        return ApiResponse.noContent();
    }

    @DeleteMapping("/{deviceId}")
    @Operation(
            summary = "FCM 토큰 삭제 API",
            description = "로그아웃 시 기기의 FCM 토큰을 삭제"
    )
    public ApiResponse<Void> deleteToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String deviceId
    ) {
        fcmTokenService.deleteToken(userDetails.getMemberId(), deviceId);
        return ApiResponse.noContent();
    }

    @GetMapping
    @Operation(
            summary = "FCM 토큰 목록 조회 API",
            description = "사용자의 모든 FCM 토큰 목록을 조회"
    )
    public ApiResponse<List<FcmTokenResponse>> getTokens(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FcmTokenResponse> tokens = fcmTokenService.getTokensByMemberId(userDetails.getMemberId());
        return ApiResponse.ok(tokens);
    }
}