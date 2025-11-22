package tetoandeggens.seeyouagainbe.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;

import java.time.LocalDateTime;

@Schema(name = "ViolationDetailResponse", description = "신고 상세 응답")
public record ViolationDetailResponse(
        @Schema(description = "신고 ID", example = "1")
        Long violationId,

        @Schema(description = "신고 상태", example = "WAITING")
        ViolatedStatus violatedStatus,

        @Schema(description = "신고 사유", example = "SPAM")
        ReportReason reason,

        @Schema(description = "신고 상세 내용", example = "스팸 게시글입니다.")
        String detailReason,

        @Schema(description = "신고자 정보")
        MemberInfo reporter,

        @Schema(description = "피신고자 정보")
        MemberInfo reportedMember,

        @Schema(description = "게시물 정보 (게시물 신고인 경우)")
        BoardInfo boardInfo,

        @Schema(description = "채팅방 정보 (채팅방 신고인 경우)")
        ChatRoomInfo chatRoomInfo,

        @Schema(description = "신고 일시", example = "2025-01-15T10:30:00")
        LocalDateTime createdAt
) {}