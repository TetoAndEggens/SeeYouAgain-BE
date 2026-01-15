package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "WithdrawalRequest", description = "회원 탈퇴 요청")
public record WithdrawalRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "비밀번호 확인", example = "Password123!", required = true)
        String password,

        @Schema(description = "탈퇴 사유 (선택)", example = "서비스 이용 불편")
        String reason
) {}