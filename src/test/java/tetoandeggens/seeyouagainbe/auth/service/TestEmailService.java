package tetoandeggens.seeyouagainbe.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@Profile("test")
@Primary
public class TestEmailService extends EmailService{

    private static final String TEST_SERVER_EMAIL = "test@seeyouagain.com";
    private static final int VERIFICATION_TIME_MINUTES = 10;

    public TestEmailService() {
        super(null, null);
    }

    @Override
    public String getServerEmail() {
        log.debug("[TEST] 테스트 서버 이메일 반환: {}", TEST_SERVER_EMAIL);
        return TEST_SERVER_EMAIL;
    }

    @Override
    public boolean extractCodeByPhoneNumber(String code, String phone, LocalDateTime since) {
        log.debug("[TEST] 인증 코드 자동 검증 - 전화번호: {}, 코드: {}", phone, code);

        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = ChronoUnit.MINUTES.between(since, now);

        boolean isValid = minutesDiff <= VERIFICATION_TIME_MINUTES;

        log.debug("[TEST] 인증 코드 검증 결과: {} (경과 시간: {}분, 제한 시간: {}분)",
                isValid, minutesDiff, VERIFICATION_TIME_MINUTES);

        if (!isValid) {
            log.warn("[TEST] 인증 코드 만료 - 전화번호: {}, 경과 시간: {}분", phone, minutesDiff);
        }

        return isValid;
    }
}
