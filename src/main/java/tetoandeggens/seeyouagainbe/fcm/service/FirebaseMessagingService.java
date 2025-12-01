package tetoandeggens.seeyouagainbe.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseMessagingService {

    // Firebase SDK를 통해 FCM 토큰 유효성 검증
    public boolean isValidToken(String token) {
        try {
            FirebaseMessaging.getInstance().send(
                    com.google.firebase.messaging.Message.builder()
                            .setToken(token)
                            .build(),
                    true  // dry-run mode (실제 전송 안 함)
            );
            log.info("FCM 토큰 유효성 검증 성공 - Token: {}...", token.substring(0, 20));
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 토큰 유효성 검증 실패 - Error: {}", e.getMessage());
            return false;
        }
    }
}
