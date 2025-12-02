package tetoandeggens.seeyouagainbe.notification.repository.custom;

import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;

import java.util.List;

public interface NotificationKeywordRepositoryCustom {

    List<NotificationKeywordResponse> findAllDtoByMemberId(Long memberId);

    List<NotificationKeyword> findAllByIdInAndMemberIdOptimized(List<Long> ids, Long memberId);

    boolean existsByMemberIdAndKeywordOptimized(
            Long memberId,
            String keyword,
            KeywordType keywordType,
            KeywordCategoryType keywordCategoryType
    );
}