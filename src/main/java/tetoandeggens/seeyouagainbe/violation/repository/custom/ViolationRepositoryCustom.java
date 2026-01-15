package tetoandeggens.seeyouagainbe.violation.repository.custom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationListResponse;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;

import java.util.Optional;

public interface ViolationRepositoryCustom {

    // 게시물 신고 중복 확인
    boolean existsByReporterAndBoard(Long reporterId, Long boardId);

    // 채팅방 신고 중복 확인
    boolean existsByReporterAndChatRoom(Long reporterId, Long chatRoomId);

    // 신고 목록 조회 (페이징, 필터링)
    Page<ViolationListResponse> findViolationList(
            ViolatedStatus status,
            Pageable pageable
    );

    // 신고 상세 조회 또는 신고 처리 시 사용
    Optional<Violation> findByIdWithAllFetch(Long violationId);
}