package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "UnifiedRegisterRequest", description = "통합 회원가입 요청")
public record UnifiedRegisterRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "로그인 아이디는 4자 이상 20자 이하여야 합니다.")
        @Schema(description = "로그인 아이디", example = "testuser123", required = true)
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        @Schema(description = "비밀번호", example = "Password123!", required = true)
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
        @Schema(description = "닉네임", example = "테스트유저", required = true)
        String nickName,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^010\\d{8}$", message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다.")
        @Schema(description = "전화번호", example = "01012345678", required = true)
        String phoneNumber,

        @Schema(description = "소셜 제공자 (선택: kakao, naver, google)", example = "kakao")
        String socialProvider,

        @Schema(description = "소셜 ID (선택)", example = "123456789")
        String socialId,

        @Schema(description = "프로필 이미지 URL (선택)", example = "https://example.com/profile.jpg")
        String profileImageUrl
) {
    // 소셜 정보가 있는지 확인
    public boolean hasSocialInfo() {
        return socialProvider != null && socialId != null;
    }
}
