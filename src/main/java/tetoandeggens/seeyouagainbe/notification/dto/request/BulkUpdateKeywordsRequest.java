package tetoandeggens.seeyouagainbe.notification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkUpdateKeywordsRequest(
        @Valid
        List<NotificationKeywordRequest> keywordsToAdd,

        List<@NotNull Long> keywordIdsToDelete
) {
    public boolean hasKeywordsToAdd() {
        return keywordsToAdd != null && !keywordsToAdd.isEmpty();
    }

    public boolean hasKeywordsToDelete() {
        return keywordIdsToDelete != null && !keywordIdsToDelete.isEmpty();
    }

    public boolean isEmpty() {
        return !hasKeywordsToAdd() && !hasKeywordsToDelete();
    }
}
