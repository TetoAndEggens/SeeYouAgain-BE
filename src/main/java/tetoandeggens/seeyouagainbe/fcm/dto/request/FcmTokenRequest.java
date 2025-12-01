package tetoandeggens.seeyouagainbe.fcm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String token,

        @NotBlank(message = "기기 ID는 필수입니다.(client가 직접 생성 필요)")
        String deviceId
) {}