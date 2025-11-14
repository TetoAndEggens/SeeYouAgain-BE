package tetoandeggens.seeyouagainbe.violation.dto.request;

import jakarta.validation.constraints.NotNull;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;

public record ViolationCreateRequest(
        Long boardId,                     // nullable
        Long chatRoomId,                  // nullable

        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReason reason,
        String detailReason
) {
    // Validation: 둘 중 하나는 반드시 있어야 함
    public ViolationCreateRequest {
        if (boardId == null && chatRoomId == null) {
            throw new IllegalArgumentException("boardId 또는 chatRoomId 중 하나는 필수입니다.");
        }
        if (boardId != null && chatRoomId != null) {
            throw new IllegalArgumentException("boardId와 chatRoomId는 동시에 입력할 수 없습니다.");
        }
    }

    // 헬퍼 메서드
    public boolean isBoard() {
        return boardId != null;
    }

    public boolean isChatRoom() {
        return chatRoomId != null;
    }
}