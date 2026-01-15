package tetoandeggens.seeyouagainbe.violation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;

@Schema(name = "ViolationCreateRequest", description = "신고")
public record ViolationCreateRequest(
        @Schema(description = "게시물 아이디")
        Long boardId,

        @Schema(description = "채팅방 아이디")
        Long chatRoomId,

        @NotNull(message = "신고 사유는 필수입니다.")
        @Schema(description = "신고 사유", example = "SPAM, ABUSE, SEXUAL_CONTENT, PRIVACY_VIOLATION, ILLEGAL_CONTENT, FRAUD, VIOLENCE, ETC 중 선택")
        ReportReason reason,

        @Schema(description = "신고 상세 내용")
        String detailReason
) {
    public boolean isBoard() {
        return boardId != null;
    }
}