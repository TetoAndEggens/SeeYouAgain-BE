package tetoandeggens.seeyouagainbe.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            return true;
        } catch (FirebaseMessagingException e) {
            return false;
        }
    }
}