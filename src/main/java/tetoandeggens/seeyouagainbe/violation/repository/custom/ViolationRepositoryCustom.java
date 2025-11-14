package tetoandeggens.seeyouagainbe.violation.repository.custom;

import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.violation.dto.response.ViolationResponse;

import java.util.List;

public interface ViolationRepositoryCustom {
    // 특정 게시글에 대한 신고 건수
    Long countByBoard(Long boardId);

    // 특정 채팅방에 대한 신고 건수
    Long countByChatRoom(Long chatRoomId);

    // 중복 신고 확인 - 게시글
    boolean existsByReporterAndBoard(Long reporterId, Long boardId);

    // 중복 신고 확인 - 채팅방
    boolean existsByReporterAndChatRoom(Long reporterId, Long chatRoomId);

    // 특정 회원이 신고 당한 횟수 조회
    Long countByReportedMember(Long reportedMemberId);

    // 관리자용: 대기 중인 신고 목록
    List<ViolationResponse> findPendingViolations(CursorPageRequest request);
}