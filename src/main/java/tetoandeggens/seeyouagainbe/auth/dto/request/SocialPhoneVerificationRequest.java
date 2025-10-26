package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "SocialPhoneVerificationRequest", description = "소셜 로그인 휴대폰 인증 요청")
public record SocialPhoneVerificationRequest(
        @NotBlank(message = "휴대전화 번호는 필수 입력값입니다.")
        @Pattern(regexp = "^010\\d{8}$", message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다.")
        @Schema(description = "휴대전화 번호", example = "01012345678")
        String phone,

        @NotBlank(message = "소셜 제공자는 필수입니다.")
        @Pattern(regexp = "^(kakao|naver|google)$", message = "제공자는 kakao, naver, google 중 하나여야 합니다.")
        @Schema(description = "소셜 제공자", example = "kakao")
        String provider,

        @NotBlank(message = "소셜 ID는 필수입니다.")
        @Schema(description = "소셜 플랫폼 고유 ID", example = "1234567890")
        String socialId,

        @Schema(description = "프로필 이미지 URL (선택)", example = "https://example.com/profile.jpg")
        String profileImageUrl
) {}