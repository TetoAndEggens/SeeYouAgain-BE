package tetoandeggens.seeyouagainbe.notification.repository.custom;

import tetoandeggens.seeyouagainbe.notification.dto.request.KeywordCheckDto;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;

import java.util.List;

public interface NotificationKeywordRepositoryCustom {

    List<NotificationKeywordResponse> findAllDtoByMemberId(Long memberId);

    List<Long> deleteByIdsAndMemberId(List<Long> ids, Long memberId);

    long deleteByIdAndMemberId(Long keywordId, Long memberId);

    boolean existsByMemberIdAndKeyword(
            Long memberId,
            String keyword,
            KeywordType keywordType,
            KeywordCategoryType keywordCategoryType
    );

    List<NotificationKeywordResponse> findExistingKeywordsByMemberIdAndKeywords(
            Long memberId,
            List<KeywordCheckDto> keywordCheckDtos
    );
}