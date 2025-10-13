package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "PhoneVerificationRequest", description = "휴대폰 인증번호 요청 DTO")
public record PhoneVerificationRequest(
        @NotBlank(message = "휴대전화 번호는 필수 입력값입니다.")
        @Pattern(regexp = "^010\\d{8}$", message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다.")
        @Schema(description = "휴대전화 번호", example = "01012341234")
        String phone
) {}