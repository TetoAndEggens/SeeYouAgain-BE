package tetoandeggens.seeyouagainbe.notification.dto.response;

import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;

import java.time.LocalDateTime;

public record NotificationKeywordResponse(
        Long id,
        String keyword,
        KeywordType keywordType,
        KeywordCategoryType keywordCategoryType,
        LocalDateTime createdAt
) {
    public static NotificationKeywordResponse from(NotificationKeyword keyword) {
        return new NotificationKeywordResponse(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getKeywordType(),
                keyword.getKeywordCategoryType(),
                keyword.getCreatedAt()
        );
    }
}