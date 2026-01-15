package tetoandeggens.seeyouagainbe.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;

public record NotificationKeywordRequest(
        @NotBlank(message = "키워드는 필수입니다.")
        String keyword,

        @NotNull(message = "키워드 타입은 필수입니다.")
        KeywordType keywordType,

        @NotNull(message = "키워드 카테고리 타입은 필수입니다.")
        KeywordCategoryType keywordCategoryType
) {}