package tetoandeggens.seeyouagainbe.violation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ViolationErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.violation.dto.request.ViolationCreateRequest;
import tetoandeggens.seeyouagainbe.violation.dto.response.ViolationResponse;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.ViolationRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViolationService {
    private final ViolationRepository violationRepository;
    private final MemberRepository memberRepository;
//    private final BoardRepository boardRepository;
//    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public ViolationResponse createViolation(UUID reporterUuid, ViolationCreateRequest request) {
        // 1. 신고자 조회
        Member reporter = memberRepository.findByUuidAndIsDeletedFalse(reporterUuid.toString())
                .orElseThrow(() -> new CustomException(ViolationErrorCode.REPORTER_NOT_FOUND));

        Member reportedMember;
        Board board = null;
        ChatRoom chatRoom = null;

        // 2. Board 신고인 경우
        if (request.isBoard()) {
            // 중복 신고 확인
            if (violationRepository.existsByReporterAndBoard(reporter.getId(), request.boardId())) {
                throw new CustomException(ViolationErrorCode.DUPLICATE_VIOLATION);
            }

//            board = boardRepository.findById(request.boardId())
//                    .orElseThrow(() -> new CustomException(ViolationErrorCode.BOARD_NOT_FOUND));
//            reportedMember = board.getMember();

            throw new CustomException(ViolationErrorCode.BOARD_NOT_FOUND);

            // 3. ChatRoom 신고인 경우
        } else {
            // 중복 신고 확인
            if (violationRepository.existsByReporterAndChatRoom(reporter.getId(), request.chatRoomId())) {
                throw new CustomException(ViolationErrorCode.DUPLICATE_VIOLATION);
            }

//            chatRoom = chatRoomRepository.findById(request.chatRoomId())
//                    .orElseThrow(() -> new CustomException(ViolationErrorCode.CHATROOM_NOT_FOUND));

            // 채팅방에서 신고자가 아닌 상대방을 피신고자로 설정
//            reportedMember = determineReportedMemberInChat(chatRoom, reporter);

            throw new CustomException(ViolationErrorCode.CHATROOM_NOT_FOUND);
        }

        // 4. 자기 자신 신고 방지
//        if (reporter.getId().equals(reportedMember.getId())) {
//            throw new CustomException(ViolationErrorCode.SELF_REPORT_NOT_ALLOWED);
//        }
//
//        // 5. Violation 엔티티 생성
//        Violation violation = Violation.builder()
//                .reporter(reporter)
//                .reportedMember(reportedMember)
//                .board(board)
//                .chatRoom(chatRoom)
//                .violatedStatus(ViolatedStatus.WAITING)
//                .reason(request.reason())
//                .detailReason(request.detailReason())
//                .build();
//
//        Violation saved = violationRepository.save(violation);
//
//        // 6. 신고 횟수 체크 및 계정 정지 처리 -> 내 생각에는 이 부분에서도 먼저 대기 후, 관리자가 승인하면 그 때 작업해야 할 듯
//        reportedMember.increaseViolatedCount();
//        memberRepository.save(reportedMember);
//
//        return ViolationResponse.from(saved);
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
}