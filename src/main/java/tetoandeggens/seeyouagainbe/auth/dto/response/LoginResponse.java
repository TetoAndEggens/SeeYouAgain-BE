package tetoandeggens.seeyouagainbe.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "LoginResponse", description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "회원 ID", example = "1")
        Long userId,

        @Schema(description = "권한", example = "ROLE_USER")
        String role,

        @Schema(description = "Access Token")
        String accessToken,

        @Schema(description = "Refresh Token")
        String refreshToken
) {}