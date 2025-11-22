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

    /**
     * 신고 목록 조회 (페이징)
     */
    public Page<ViolationListResponse> getViolationList(
            ViolatedStatus status,
            Pageable pageable
    ) {
        return violationRepository.findViolationList(status, pageable);
    }

    /**
     * 신고 상세 조회
     */
    public ViolationDetailResponse getViolationDetail(Long violationId) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new CustomException(AdminErrorCode.VIOLATION_NOT_FOUND));

        return buildViolationDetailResponse(violation);
    }

    /**
     * 신고 처리 (위반/위반 아님 결정)
     */
    @Transactional
    public void processViolation(Long violationId, ViolationProcessRequest request) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new CustomException(AdminErrorCode.VIOLATION_NOT_FOUND));

        // 유효성 검증
        validateViolationProcessRequest(violation, request);

        // deleteContent 기본값 설정
        boolean shouldDeleteContent = determineDeleteContent(request);

        // 신고 상태 업데이트
        violation.updateViolatedStatus(request.violatedStatus());

        // 위반으로 판단된 경우
        if (request.violatedStatus() == ViolatedStatus.VIOLATED) {
            handleViolatedCase(violation, shouldDeleteContent);
        } else {
            // 위반 아님으로 처리된 경우
            handleNormalCase(violation);
        }

        log.info("신고 처리 완료 - violationId: {}, status: {}, deleteContent: {}",
                violationId, request.violatedStatus(), shouldDeleteContent);
    }

    /**
     * 신고 처리 요청 검증
     */
    private void validateViolationProcessRequest(Violation violation, ViolationProcessRequest request) {
        // 이미 처리된 신고인지 확인
//        if (violation.getViolatedStatus() != ViolatedStatus.WAITING) {
//            throw new CustomException(AdminErrorCode.ALREADY_PROCESSED_VIOLATION);
//        }

        // WAITING 상태로는 처리할 수 없음
        if (request.violatedStatus() == ViolatedStatus.WAITING) {
            throw new IllegalArgumentException("처리 상태는 VIOLATED 또는 NORMAL만 가능합니다.");
        }
    }

    /**
     * 콘텐츠 삭제 여부 결정
     */
    private boolean determineDeleteContent(ViolationProcessRequest request) {
        if (request.deleteContent() != null) {
            return request.deleteContent();
        }

        // deleteContent가 null인 경우 기본값 설정
        return request.violatedStatus() == ViolatedStatus.VIOLATED;
    }

    /**
     * 위반 처리 로직
     */
    private void handleViolatedCase(Violation violation, boolean deleteContent) {
        // 피신고자 위반 횟수 증가 및 자동 정지 처리
        Member reportedMember = violation.getReportedMember();
        reportedMember.increaseViolatedCount();
        memberRepository.save(reportedMember);

        log.info("위반 횟수 증가 - memberId: {}, violatedCount: {}, isBanned: {}",
                reportedMember.getId(),
                reportedMember.getViolatedCount(),
                reportedMember.getIsBanned());

        // 콘텐츠 삭제 및 위반 상태 업데이트
        if (deleteContent) {
            deleteViolatedContent(violation);
        } else {
            // 삭제하지 않더라도 위반 상태는 업데이트
            updateContentViolatedStatus(violation, ViolatedStatus.VIOLATED);
        }
    }

    /**
     * 위반 아님 처리 로직
     */
    private void handleNormalCase(Violation violation) {
        // 콘텐츠의 위반 상태를 NORMAL로 업데이트
        updateContentViolatedStatus(violation, ViolatedStatus.NORMAL);

        log.info("위반 아님 처리 완료 - violationId: {}", violation.getId());
    }

    /**
     * 위반 콘텐츠 삭제 (isDeleted = true, violatedStatus = VIOLATED)
     */
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

    /**
     * 콘텐츠 위반 상태만 업데이트 (isDeleted는 변경하지 않음)
     */
    private void updateContentViolatedStatus(Violation violation, ViolatedStatus status) {
        if (violation.getBoard() != null) {
            Board board = violation.getBoard();
            board.updateViolatedStatus(status);
            board.updateIsDeleted(false);
            boardRepository.save(board);
            log.info("게시물 위반 상태 업데이트 - boardId: {}, violatedStatus: {}",
                    board.getId(), status);
        } else if (violation.getChatRoom() != null) {
            ChatRoom chatRoom = violation.getChatRoom();
            chatRoom.updateViolatedStatus(status);
            chatRoom.updateIsDeleted(false);
            chatRoomRepository.save(chatRoom);
            log.info("채팅방 위반 상태 업데이트 - chatRoomId: {}, violatedStatus: {}",
                    chatRoom.getId(), status);
        }
    }

    /**
     * ViolationDetailResponse 빌드
     */
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

    /**
     * MemberInfo 빌드
     */
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