package tetoandeggens.seeyouagainbe.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "SocialTempInfoResponse",description = "소셜 로그인 임시 정보 응답")
public record SocialTempInfoResponse(
        @Schema(description = "프로필 이미지 URL", example = "https://k.kakaocdn.net/dn/profile.jpg")
        String profileImageUrl,

        @Schema(description = "임시 UUID - 서버에서 소셜 정보 조회에 사용")
        String tempUuid
) {}