package tetoandeggens.seeyouagainbe.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.auth.dto.request.RegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.service.AuthService;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "일반 회원가입 (loginId, password, nickName, phoneNumber)"
    )
    @PostMapping("/signup")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ApiResponse.created(null);
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
            summary = "loginId 중복 체크",
            description = "로그인 아이디 중복 확인"
    )
    @GetMapping("/check/loginId")
    public ApiResponse<Boolean> checkLoginId(@RequestParam String loginId) {
        boolean available = authService.checkLoginIdAvailable(loginId);
        return ApiResponse.ok(available);
    }

    @Operation(
            summary = "phoneNumber 중복 체크",
            description = "휴대폰 번호 중복 확인"
    )
    @GetMapping("/check/phone")
    public ApiResponse<Boolean> checkPhoneNumber(@RequestParam String phoneNumber) {
        boolean available = authService.checkPhoneNumberAvailable(phoneNumber);
        return ApiResponse.ok(available);
    }
}
