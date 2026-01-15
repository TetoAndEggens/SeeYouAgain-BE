package tetoandeggens.seeyouagainbe.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;

import java.time.LocalDateTime;

@Schema(name = "ViolationListResponse", description = "신고 목록 응답")
public record ViolationListResponse(
        @Schema(description = "신고 ID", example = "1")
        Long violationId,

        @Schema(description = "신고 상태", example = "WAITING")
        ViolatedStatus violatedStatus,

        @Schema(description = "신고 사유", example = "SPAM")
        ReportReason reason,

        @Schema(description = "신고자 닉네임", example = "사용자1")
        String reporterNickName,

        @Schema(description = "피신고자 닉네임", example = "사용자2")
        String reportedMemberNickName,

        @Schema(description = "신고 대상 타입", example = "BOARD")
        String targetType,

        @Schema(description = "신고 대상 ID", example = "1")
        Long targetId,

        @Schema(description = "신고 일시", example = "2025-01-15T10:30:00")
        LocalDateTime createdAt
) {}