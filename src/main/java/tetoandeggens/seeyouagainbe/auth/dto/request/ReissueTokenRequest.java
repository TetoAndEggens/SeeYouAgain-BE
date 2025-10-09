package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ReissueTokenRequest", description = "토큰 재발급 요청")
public record ReissueTokenRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        @Schema(description = "Refresh Token")
        String refreshToken
) {}