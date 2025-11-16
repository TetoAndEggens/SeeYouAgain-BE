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
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.violation.dto.request.ViolationCreateRequest;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.ViolationRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViolationService {
    private final ViolationRepository violationRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void createViolation(UUID reporterUuid, ViolationCreateRequest request) {
        validateViolationTarget(request);

        Member reporter = memberRepository.findByUuidAndIsDeletedFalse(reporterUuid.toString())
                .orElseThrow(() -> new CustomException(ViolationErrorCode.REPORTER_NOT_FOUND));

        Member reportedMember;
        Board board = null;
        ChatRoom chatRoom = null;

        if (request.isBoard()) {
            if (violationRepository.existsByReporterAndBoard(reporter.getId(), request.boardId())) {
                throw new CustomException(ViolationErrorCode.DUPLICATE_VIOLATION);
            }

            board = boardRepository.findById(request.boardId())
                    .orElseThrow(() -> new CustomException(ViolationErrorCode.BOARD_NOT_FOUND));

            reportedMember = board.getMember();
        } else {
            if (violationRepository.existsByReporterAndChatRoom(reporter.getId(), request.chatRoomId())) {
                throw new CustomException(ViolationErrorCode.DUPLICATE_VIOLATION);
            }

            chatRoom = chatRoomRepository.findById(request.chatRoomId())
                    .orElseThrow(() -> new CustomException(ViolationErrorCode.CHATROOM_NOT_FOUND));

            reportedMember = determineReportedMemberInChat(chatRoom, reporter);
        }

        if (reporter.getId().equals(reportedMember.getId())) {
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

    private Member determineReportedMemberInChat(ChatRoom chatRoom, Member reporter) {
        if (chatRoom.getSender().getId().equals(reporter.getId())) {
            return chatRoom.getReceiver();
        } else if (chatRoom.getReceiver().getId().equals(reporter.getId())) {
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