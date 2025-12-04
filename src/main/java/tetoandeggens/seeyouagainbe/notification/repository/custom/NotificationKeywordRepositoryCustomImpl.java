package tetoandeggens.seeyouagainbe.notification.repository.custom;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.notification.dto.request.KeywordCheckDto;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;

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
                        notificationKeyword.keywordCategoryType
                ))
                .from(notificationKeyword)
                .where(notificationKeyword.member.id.eq(memberId))
                .fetch();
    }

    @Override
    public List<Long> deleteByIdsAndMemberId(List<Long> ids, Long memberId) {

        List<Long> existingIds = queryFactory
                .select(notificationKeyword.id)
                .from(notificationKeyword)
                .where(
                        notificationKeyword.id.in(ids),
                        notificationKeyword.member.id.eq(memberId)
                )
                .fetch();

        if (existingIds.isEmpty()) {
            return List.of();
        }

        queryFactory
                .delete(notificationKeyword)
                .where(notificationKeyword.id.in(existingIds))
                .execute();

        return existingIds;
    }

    @Override
    public long deleteByIdAndMemberId(Long keywordId, Long memberId) {
        return queryFactory
                .delete(notificationKeyword)
                .where(
                        notificationKeyword.id.eq(keywordId),
                        notificationKeyword.member.id.eq(memberId)
                )
                .execute();
    }

    @Override
    public boolean existsByMemberIdAndKeyword(
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

    @Override
    public List<NotificationKeywordResponse> findExistingKeywordsByMemberIdAndKeywords(
            Long memberId,
            List<KeywordCheckDto> keywordCheckDtos
    ) {
        if (keywordCheckDtos == null || keywordCheckDtos.isEmpty()) {
            return List.of();
        }

        BooleanBuilder orBuilder = new BooleanBuilder();

        for (KeywordCheckDto dto : keywordCheckDtos) {
            orBuilder.or(
                    notificationKeyword.keyword.eq(dto.keyword())
                            .and(notificationKeyword.keywordType.eq(dto.keywordType()))
                            .and(notificationKeyword.keywordCategoryType.eq(dto.keywordCategoryType()))
            );
        }

        return queryFactory
                .select(Projections.constructor(
                        NotificationKeywordResponse.class,
                        notificationKeyword.id,
                        notificationKeyword.keyword,
                        notificationKeyword.keywordType,
                        notificationKeyword.keywordCategoryType
                ))
                .from(notificationKeyword)
                .where(
                        notificationKeyword.member.id.eq(memberId),
                        orBuilder
                )
                .fetch();
    }
}
