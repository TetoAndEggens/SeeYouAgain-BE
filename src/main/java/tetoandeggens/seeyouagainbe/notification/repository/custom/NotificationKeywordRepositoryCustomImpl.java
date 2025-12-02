package tetoandeggens.seeyouagainbe.notification.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;

import java.util.List;

import static tetoandeggens.seeyouagainbe.notification.entity.QNotificationKeyword.notificationKeyword;

@RequiredArgsConstructor
public class NotificationKeywordRepositoryCustomImpl implements NotificationKeywordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NotificationKeywordResponse> findAllDtoByMemberId(Long memberId) {
        return queryFactory
                .select(Projections.constructor(
                        NotificationKeywordResponse.class,
                        notificationKeyword.id,
                        notificationKeyword.keyword,
                        notificationKeyword.keywordType,
                        notificationKeyword.keywordCategoryType,
                        notificationKeyword.createdAt
                ))
                .from(notificationKeyword)
                .where(notificationKeyword.member.id.eq(memberId))
                .orderBy(notificationKeyword.createdAt.desc())
                .fetch();
    }

    @Override
    public List<NotificationKeyword> findAllByIdInAndMemberIdOptimized(List<Long> ids, Long memberId) {
        return queryFactory
                .selectFrom(notificationKeyword)
                .where(
                        notificationKeyword.id.in(ids),
                        notificationKeyword.member.id.eq(memberId)
                )
                .fetch();
    }

    @Override
    public boolean existsByMemberIdAndKeywordOptimized(
            Long memberId,
            String keyword,
            KeywordType keywordType,
            KeywordCategoryType keywordCategoryType
    ) {
        Integer count = queryFactory
                .selectOne()
                .from(notificationKeyword)
                .where(
                        notificationKeyword.member.id.eq(memberId),
                        notificationKeyword.keyword.eq(keyword),
                        notificationKeyword.keywordType.eq(keywordType),
                        notificationKeyword.keywordCategoryType.eq(keywordCategoryType)
                )
                .fetchFirst();

        return count != null;
    }
}
