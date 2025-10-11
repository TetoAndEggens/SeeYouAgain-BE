package tetoandeggens.seeyouagainbe.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "ReissueTokenResponse", description = "토큰 재발급 응답")
public record ReissueTokenResponse(
        @Schema(description = "새로 발급된 Access Token")
        String accessToken
) {}