package tetoandeggens.seeyouagainbe.violation.dto.response;

import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;

import java.time.LocalDateTime;

public record ViolationResponse(
        Long violationId,
        ViolatedStatus status,
        ReportReason reason,
        String detailReason,
        Long reporterId,
        Long reportedMemberId,
        Long boardId,
        Long chatRoomId,
        LocalDateTime createdAt
) {
    public static ViolationResponse from(Violation violation) {
        return new ViolationResponse(
                violation.getId(),
                violation.getViolatedStatus(),
                violation.getReason(),
                violation.getDetailReason(),
                violation.getReporter() != null ? violation.getReporter().getId() : null,
                violation.getReportedMember() != null ? violation.getReportedMember().getId() : null,
                violation.getBoard() != null ? violation.getBoard().getId() : null,
                violation.getChatRoom() != null ? violation.getChatRoom().getId() : null,
                violation.getCreatedAt()
        );
    }
}