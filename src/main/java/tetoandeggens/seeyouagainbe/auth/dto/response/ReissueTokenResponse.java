package tetoandeggens.seeyouagainbe.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReissueTokenResponse", description = "토큰 재발급 응답")
public record ReissueTokenResponse(
        @Schema(description = "새로운 Access Token")
        String accessToken
) {}