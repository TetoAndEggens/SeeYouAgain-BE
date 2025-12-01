package tetoandeggens.seeyouagainbe.fcm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;

public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String token,

        @NotBlank(message = "기기 ID는 필수입니다.")
        String deviceId,

        @NotNull(message = "기기 타입은 필수입니다.")
        DeviceType deviceType
) {}