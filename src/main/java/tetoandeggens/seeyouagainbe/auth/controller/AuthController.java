package tetoandeggens.seeyouagainbe.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.auth.dto.request.*;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialLoginResultResponse;
import tetoandeggens.seeyouagainbe.auth.service.AuthService;
import tetoandeggens.seeyouagainbe.auth.service.OAuth2Service;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oAuth2Service;

    @Operation(
            summary = "loginId 중복 체크",
            description = "로그인 아이디 중복 확인"
    )
    @GetMapping("/check/loginId")
    public ApiResponse<Void> checkLoginId(@RequestParam @NotBlank(message = "loginId는 비어 있을 수 없습니다.") String loginId) {
        authService.checkLoginIdAvailable(loginId);
        return ApiResponse.noContent();
    }

    @Operation(
            summary = "휴대폰 번호 중복 체크",
            description = "휴대폰 번호 중복 확인"
    )
    @PostMapping("/check/phone")
    public ApiResponse<Void> checkPhoneNumber(@Valid @RequestBody PhoneVerificationRequest request) {
        authService.checkPhoneNumberDuplicate(request.phone());
        return ApiResponse.noContent();
    }

    @Operation(
            summary = "휴대폰 인증 코드 요청",
            description = "휴대폰 인증을 위한 코드 생성. 응답으로 받은 code를 서버 이메일 주소로 SMS 전송 필요"
    )
    @PostMapping("/phone/send-code")
    public ApiResponse<PhoneVerificationResultResponse> sendPhoneVerificationCode(
            @Valid @RequestBody PhoneVerificationRequest request) {
        PhoneVerificationResultResponse response = authService.sendPhoneVerificationCode(request.phone());
        return ApiResponse.ok(response);
    }

    @Operation(
            summary = "휴대폰 인증 코드 검증",
            description = "사용자가 SMS로 보낸 인증 코드를 서버가 이메일로 확인하여 검증"
    )
    @PostMapping("/phone/verify-code")
    public ApiResponse<Void> verifyPhoneCode(@Valid @RequestBody PhoneVerificationRequest request) {
        authService.verifyPhoneCode(request.phone());
        return ApiResponse.noContent();
    }

    @Operation(
            summary = "통합 회원가입",
            description = "일반 회원가입 (loginId, password, nickName, phoneNumber 필수)\n\n" +
                    "소셜 로그인으로 시작한 경우에도 이 API를 사용하며, " +
                    "socialProvider, socialId, profileImageUrl은 자동으로 Redis에서 가져옵니다."
    )
    @PostMapping("/signup")
    public ApiResponse<Void> register(@Valid @RequestBody UnifiedRegisterRequest request) {
        authService.unifiedRegister(request);
        return ApiResponse.created();
    }

    @Operation(
            summary = "[소셜] 휴대폰 인증 코드 요청",
            description = "소셜 로그인 시 휴대폰 인증을 위한 코드 생성. provider, socialId, profileImageUrl 포함"
    )
    @PostMapping("/social/phone/send-code")
    public ApiResponse<PhoneVerificationResultResponse> sendSocialPhoneVerificationCode(
            @Valid @RequestBody SocialPhoneVerificationRequest request) {
        PhoneVerificationResultResponse response = oAuth2Service.sendSocialPhoneVerificationCode(
                request.phone(),
                request.provider(),
                request.socialId(),
                request.profileImageUrl()
        );
        return ApiResponse.ok(response);
    }

    @Operation(
            summary = "[소셜] 휴대폰 인증 코드 검증",
            description = "소셜 로그인 시 인증 코드 검증 후 상태 반환 (LOGIN/LINK/SIGNUP)"
    )
    @PostMapping("/social/phone/verify-code")
    public ApiResponse<SocialLoginResultResponse> verifySocialPhoneCode(
            @Valid @RequestBody PhoneVerificationRequest request,
            HttpServletResponse response) {
        SocialLoginResultResponse result = oAuth2Service.verifySocialPhoneCode(request.phone(), response);
        return ApiResponse.ok(result);
    }

    @Operation(
            summary = "[소셜] 기존 계정에 소셜 연동",
            description = "기존 계정에 소셜 계정 연동"
    )
    @PostMapping("/social/link")
    public ApiResponse<SocialLoginResultResponse> linkSocialAccount(
            @Valid @RequestBody SocialLinkRequest request,
            HttpServletResponse response) {
        SocialLoginResultResponse result = oAuth2Service.linkSocialAccount(request.phoneNumber(), response);
        return ApiResponse.ok(result);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token으로 새로운 Access Token 발급"
    )
    @PostMapping("/reissue")
    public ApiResponse<ReissueTokenResponse> reissueToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ReissueTokenResponse reissueResponse = authService.reissueToken(request, response);
        return ApiResponse.ok(reissueResponse);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "회원 탈퇴 처리. 소셜 연동이 있는 경우 자동으로 연동 해제 후 soft delete 수행",
            security = @SecurityRequirement(name = "jwt")
    )
    @DeleteMapping("/withdrawal")
    public ApiResponse<Void> withdrawMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WithdrawalRequest request
    ) {
        authService.withdrawMember(userDetails.getUuid(), request);
        return ApiResponse.noContent();
    }
}
