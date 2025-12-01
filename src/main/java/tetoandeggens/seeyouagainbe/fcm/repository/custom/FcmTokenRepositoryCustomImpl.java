package tetoandeggens.seeyouagainbe.fcm.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;

import java.time.LocalDateTime;
import java.util.List;

import static tetoandeggens.seeyouagainbe.fcm.entity.QFcmToken.fcmToken;

@RequiredArgsConstructor
public class FcmTokenRepositoryCustomImpl implements FcmTokenRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<FcmToken> findExpiredTokens(LocalDateTime expirationDate) {
        return queryFactory
                .selectFrom(fcmToken)
                .where(fcmToken.lastUsedAt.lt(expirationDate))
                .fetch();
    }
}
