package tetoandeggens.seeyouagainbe.notification.dto.response;

import java.util.List;

public record BulkUpdateKeywordsResponse(
        List<NotificationKeywordResponse> addedKeywords,
        List<Long> deletedKeywordIds,
        int addedCount,
        int deletedCount,
        String message
) {
    public static BulkUpdateKeywordsResponse of(
            List<NotificationKeywordResponse> addedKeywords,
            List<Long> deletedKeywordIds
    ) {
        int addedCount = addedKeywords != null ? addedKeywords.size() : 0;
        int deletedCount = deletedKeywordIds != null ? deletedKeywordIds.size() : 0;

        String message = String.format(
                "키워드 업데이트 완료 - 추가: %d개, 삭제: %d개",
                addedCount,
                deletedCount
        );

        return new BulkUpdateKeywordsResponse(
                addedKeywords,
                deletedKeywordIds,
                addedCount,
                deletedCount,
                message
        );
    }
}
