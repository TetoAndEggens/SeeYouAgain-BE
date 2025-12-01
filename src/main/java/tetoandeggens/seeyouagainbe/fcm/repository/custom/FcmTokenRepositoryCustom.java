package tetoandeggens.seeyouagainbe.fcm.repository.custom;

import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;

import java.time.LocalDateTime;
import java.util.List;

public interface FcmTokenRepositoryCustom {

    // 특정 날짜 이전에 사용된 만료된 토큰 조회
    List<FcmToken> findExpiredTokens(LocalDateTime expirationDate);
}