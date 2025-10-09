package tetoandeggens.seeyouagainbe.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.auth.dto.request.RegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.request.ReissueTokenRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.service.AuthService;

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
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token으로 새로운 Access Token 발급"
    )
    @PostMapping("/reissue")
    public ResponseEntity<ReissueTokenResponse> reissueToken(@Valid @RequestBody ReissueTokenRequest request) {
        ReissueTokenResponse response = authService.reissueToken(request);
        return ResponseEntity.ok(response);
    }

//    회원 탈퇴시 delete필드 하나 더 추가해야 하지 않나?
//    @Operation(
//            summary = "회원 탈퇴",
//            description = "회원 탈퇴 및 Refresh Token 삭제",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    @DeleteMapping("/withdraw")
//    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
//        authService.withdraw(userDetails.getUserId());
//        return ResponseEntity.ok().build();
//    }

    @Operation(
            summary = "loginId 중복 체크",
            description = "로그인 아이디 중복 확인"
    )
    @GetMapping("/check/loginId")
    public ResponseEntity<Boolean> checkLoginId(@RequestParam String loginId) {
        boolean available = authService.checkLoginIdAvailable(loginId);
        return ResponseEntity.ok(available);
    }

    @Operation(
            summary = "phoneNumber 중복 체크",
            description = "휴대폰 번호 중복 확인"
    )
    @GetMapping("/check/phone")
    public ResponseEntity<Boolean> checkPhoneNumber(@RequestParam String phoneNumber) {
        boolean available = authService.checkPhoneNumberAvailable(phoneNumber);
        return ResponseEntity.ok(available);
    }
}
