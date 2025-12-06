package tetoandeggens.seeyouagainbe.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "푸시 알림 토글 업데이트 응답 DTO")
public record UpdatePushEnabledResponse(

        @Schema(description = "푸시 알림 활성화 여부", example = "true")
        Boolean isPushEnabled
) {
    public static UpdatePushEnabledResponse from(Boolean isPushEnabled) {
        return new UpdatePushEnabledResponse(isPushEnabled);
    }
}