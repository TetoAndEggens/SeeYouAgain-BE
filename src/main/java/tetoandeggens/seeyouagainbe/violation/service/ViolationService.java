package tetoandeggens.seeyouagainbe.violation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ViolationErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.violation.dto.request.ViolationCreateRequest;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.ViolationRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViolationService {
    private final ViolationRepository violationRepository;
    private final BoardRepository boardRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void createViolation(Long reporterMemberId, ViolationCreateRequest request) {
        validateViolationTarget(request);

        Member reporter = new Member(reporterMemberId);

        Member reportedMember;
        Board board = null;
        ChatRoom chatRoom = null;

        if (request.isBoard()) {
            if (violationRepository.existsByReporterAndBoard(reporterMemberId, request.boardId())) {
                throw new CustomException(ViolationErrorCode.DUPLICATE_VIOLATION);
            }

            board = boardRepository.findById(request.boardId())
                    .orElseThrow(() -> new CustomException(ViolationErrorCode.BOARD_NOT_FOUND));

            reportedMember = board.getMember();
        } else {
            if (violationRepository.existsByReporterAndChatRoom(reporterMemberId, request.chatRoomId())) {
                throw new CustomException(ViolationErrorCode.DUPLICATE_VIOLATION);
            }

            // fetch join으로 ChatRoom 조회 시 Member도 함께 로드하여 N+1 해결시도
            chatRoom = chatRoomRepository.findByIdWithMembers(request.chatRoomId())
                    .orElseThrow(() -> new CustomException(ViolationErrorCode.CHATROOM_NOT_FOUND));

            reportedMember = determineReportedMemberInChat(chatRoom, reporterMemberId);
        }

        if (reporterMemberId.equals(reportedMember.getId())) {
            throw new CustomException(ViolationErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        Violation violation = Violation.builder()
                .reporter(reporter)
                .reportedMember(reportedMember)
                .board(board)
                .chatRoom(chatRoom)
                .violatedStatus(ViolatedStatus.WAITING)
                .reason(request.reason())
                .detailReason(request.detailReason())
                .build();

        violationRepository.save(violation);
    }

    private Member determineReportedMemberInChat(ChatRoom chatRoom, Long reporterMemberId) {
        if (chatRoom.getSender().getId().equals(reporterMemberId)) {
            return chatRoom.getReceiver();
        } else if (chatRoom.getReceiver().getId().equals(reporterMemberId)) {
            return chatRoom.getSender();
        } else {
            throw new CustomException(ViolationErrorCode.UNAUTHORIZED_CHAT_REPORT);
        }
    }

    private void validateViolationTarget(ViolationCreateRequest request) {
        if (request.boardId() == null && request.chatRoomId() == null) {
            throw new CustomException(ViolationErrorCode.VIOLATION_TARGET_REQUIRED);
        }
        if (request.boardId() != null && request.chatRoomId() != null) {
            throw new CustomException(ViolationErrorCode.VIOLATION_TARGET_CONFLICT);
        }
    }
}