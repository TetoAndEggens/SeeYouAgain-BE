package tetoandeggens.seeyouagainbe.fcm.dto.response;

import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;

import java.time.LocalDateTime;

public record FcmTokenResponse(
        Long id,
        String deviceId,
        DeviceType deviceType,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {
    public static FcmTokenResponse from(FcmToken fcmToken) {
        return new FcmTokenResponse(
                fcmToken.getId(),
                fcmToken.getDeviceId(),
                fcmToken.getDeviceType(),
                fcmToken.getLastUsedAt(),
                fcmToken.getCreatedAt()
        );
    }
}