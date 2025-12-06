package tetoandeggens.seeyouagainbe.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "푸시 알림 토글 업데이트 요청 DTO")
public record UpdatePushEnabledRequest(

        @Schema(description = "푸시 알림 활성화 여부", example = "true")
        @NotNull(message = "푸시 알림 활성화 여부는 필수입니다.")
        Boolean isPushEnabled
) {}