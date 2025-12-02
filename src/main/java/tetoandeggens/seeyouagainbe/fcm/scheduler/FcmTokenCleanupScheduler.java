package tetoandeggens.seeyouagainbe.fcm.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.fcm.service.FcmTokenService;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmTokenCleanupScheduler {

    private static final int EXPIRATION_DAYS = 60;
    private final FcmTokenService fcmTokenService;

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        try {
            fcmTokenService.cleanupExpiredTokens(EXPIRATION_DAYS);
            log.info("FCM 토큰 정리 스케줄러 완료");
        } catch (Exception e) {
            log.error("FCM 토큰 정리 스케줄러 실패: ", e);
        }
    }
}