package tetoandeggens.seeyouagainbe.notification.dto.request;

import jakarta.validation.Valid;

import java.util.List;

public record BulkUpdateKeywordsRequest(
        @Valid
        List<NotificationKeywordRequest> keywordsToAdd,

        List<Long> keywordIdsToDelete
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
