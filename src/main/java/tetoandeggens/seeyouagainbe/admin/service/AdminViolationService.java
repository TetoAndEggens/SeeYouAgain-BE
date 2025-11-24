package tetoandeggens.seeyouagainbe.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.admin.dto.request.ViolationProcessRequest;
import tetoandeggens.seeyouagainbe.admin.dto.response.*;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AdminErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.ViolationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminViolationService {

    private final ViolationRepository violationRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ChatRoomRepository chatRoomRepository;

    public Page<ViolationListResponse> getViolationList(
            ViolatedStatus status,
            Pageable pageable
    ) {
        return violationRepository.findViolationList(status, pageable);
    }

    public ViolationDetailResponse getViolationDetail(Long violationId) {
        Violation violation = violationRepository.findByIdWithAllFetch(violationId)
                .orElseThrow(() -> new CustomException(AdminErrorCode.VIOLATION_NOT_FOUND));

        return buildViolationDetailResponse(violation);
    }

    @Transactional
    public void processViolation(Long violationId, ViolationProcessRequest request) {
        Violation violation = violationRepository.findByIdWithAllFetch(violationId)
                .orElseThrow(() -> new CustomException(AdminErrorCode.VIOLATION_NOT_FOUND));

        boolean shouldDeleteContent = determineDeleteContent(request);

        violation.updateViolatedStatus(request.violatedStatus());

        if (request.violatedStatus() == ViolatedStatus.VIOLATED) {  // 위반으로 판단된 경우
            handleViolatedCase(violation, shouldDeleteContent);
        } else { // 위반 아님으로 처리된 경우
            handleNormalCase(violation);
        }

        log.info("신고 처리 완료 - violationId: {}, status: {}, deleteContent: {}",
                violationId, request.violatedStatus(), shouldDeleteContent);
    }

    private boolean determineDeleteContent(ViolationProcessRequest request) {
        if (request.deleteContent() != null) {
            return request.deleteContent();
        }

        // deleteContent가 null인 경우 기본값 설정
        return request.violatedStatus() == ViolatedStatus.VIOLATED;
    }

    private void handleViolatedCase(Violation violation, boolean deleteContent) {
        // 피신고자 위반 횟수 증가 및 자동 정지 처리
        Member reportedMember = violation.getReportedMember();
        reportedMember.increaseViolatedCount();
        memberRepository.save(reportedMember);

        log.info("위반 횟수 증가 - memberId: {}, violatedCount: {}, isBanned: {}",
                reportedMember.getId(),
                reportedMember.getViolatedCount(),
                reportedMember.getIsBanned());

        if (deleteContent) { // 콘텐츠 삭제 및 위반 상태 업데이트
            deleteViolatedContent(violation);
        } else { // 삭제하지 않더라도 위반 상태는 업데이트
            updateContentViolatedStatus(violation, ViolatedStatus.VIOLATED);
        }
    }

    private void handleNormalCase(Violation violation) {
        updateContentViolatedStatus(violation, ViolatedStatus.NORMAL);
    }

    // 위반 콘텐츠 삭제 (isDeleted = true, violatedStatus = VIOLATED)
    private void deleteViolatedContent(Violation violation) {
        if (violation.getBoard() != null) {
            Board board = violation.getBoard();
            board.updateIsDeleted(true);
            board.updateViolatedStatus(ViolatedStatus.VIOLATED);
            boardRepository.save(board);
            log.info("게시물 삭제 처리 - boardId: {}, violatedStatus: VIOLATED", board.getId());
        } else if (violation.getChatRoom() != null) {
            ChatRoom chatRoom = violation.getChatRoom();
            chatRoom.updateIsDeleted(true);
            chatRoom.updateViolatedStatus(ViolatedStatus.VIOLATED);
            chatRoomRepository.save(chatRoom);
            log.info("채팅방 삭제 처리 - chatRoomId: {}, violatedStatus: VIOLATED", chatRoom.getId());
        }
    }

    // VIOLATED 상태를 다시 NORMAL로 바꾸거나 위반이라도 삭제는 하지 않는 경우에 사용
    private void updateContentViolatedStatus(Violation violation, ViolatedStatus status) {
        if (violation.getBoard() != null) {
            Board board = violation.getBoard();
            board.updateViolatedStatus(status);
            board.updateIsDeleted(false);
            boardRepository.save(board);
            log.info("게시물 위반 상태 수정 - boardId: {}, violatedStatus: {}",
                    board.getId(), status);
        } else if (violation.getChatRoom() != null) {
            ChatRoom chatRoom = violation.getChatRoom();
            chatRoom.updateViolatedStatus(status);
            chatRoom.updateIsDeleted(false);
            chatRoomRepository.save(chatRoom);
            log.info("채팅방 위반 상태 수정 - chatRoomId: {}, violatedStatus: {}",
                    chatRoom.getId(), status);
        }
    }

    private ViolationDetailResponse buildViolationDetailResponse(Violation violation) {
        MemberInfo reporterInfo = buildMemberInfo(violation.getReporter());
        MemberInfo reportedMemberInfo = buildMemberInfo(violation.getReportedMember());

        BoardInfo boardInfo = null;
        ChatRoomInfo chatRoomInfo = null;

        if (violation.getBoard() != null) {
            Board board = violation.getBoard();
            boardInfo = new BoardInfo(
                    board.getId(),
                    board.getTitle(),
                    board.getContent(),
                    board.getCreatedAt()
            );
        } else if (violation.getChatRoom() != null) {
            ChatRoom chatRoom = violation.getChatRoom();
            chatRoomInfo = new ChatRoomInfo(
                    chatRoom.getId(),
                    chatRoom.getCreatedAt()
            );
        }

        return new ViolationDetailResponse(
                violation.getId(),
                violation.getViolatedStatus(),
                violation.getReason(),
                violation.getDetailReason(),
                reporterInfo,
                reportedMemberInfo,
                boardInfo,
                chatRoomInfo,
                violation.getCreatedAt()
        );
    }

    private MemberInfo buildMemberInfo(Member member) {
        return new MemberInfo(
                member.getId(),
                member.getNickName(),
                member.getLoginId(),
                member.getViolatedCount(),
                member.getIsBanned()
        );
    }
}