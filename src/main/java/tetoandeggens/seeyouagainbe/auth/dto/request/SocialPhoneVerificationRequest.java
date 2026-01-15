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

        @NotBlank(message = "임시 UUID는 필수입니다.")
        @Schema(description = "임시 UUID (소셜 로그인 성공 시 발급받은 UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String tempUuid
) {}