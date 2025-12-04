package tetoandeggens.seeyouagainbe.notification.dto.request;

import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;

public record KeywordCheckDto(
        String keyword,
        KeywordType keywordType,
        KeywordCategoryType keywordCategoryType
) {}