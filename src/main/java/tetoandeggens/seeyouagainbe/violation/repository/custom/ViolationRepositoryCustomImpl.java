package tetoandeggens.seeyouagainbe.violation.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.violation.dto.response.ViolationResponse;

import java.util.List;

import static tetoandeggens.seeyouagainbe.violation.entity.QViolation.violation;

@RequiredArgsConstructor
public class ViolationRepositoryCustomImpl implements ViolationRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Long countByBoard(Long boardId) {
        return queryFactory
                .select(violation.count())
                .from(violation)
                .where(violation.board.id.eq(boardId))
                .fetchOne();
    }

    @Override
    public Long countByChatRoom(Long chatRoomId) {
        return queryFactory
                .select(violation.count())
                .from(violation)
                .where(violation.chatRoom.id.eq(chatRoomId))
                .fetchOne();
    }

    @Override
    public boolean existsByReporterAndBoard(Long reporterId, Long boardId) {
        Integer fetchFirst = queryFactory
                .selectOne()
                .from(violation)
                .where(
                        violation.reporter.id.eq(reporterId),
                        violation.board.id.eq(boardId)
                )
                .fetchFirst();

        return fetchFirst != null;
    }

    @Override
    public boolean existsByReporterAndChatRoom(Long reporterId, Long chatRoomId) {
        Integer fetchFirst = queryFactory
                .selectOne()
                .from(violation)
                .where(
                        violation.reporter.id.eq(reporterId),
                        violation.chatRoom.id.eq(chatRoomId)
                )
                .fetchFirst();

        return fetchFirst != null;
    }

    @Override
    public Long countByReportedMember(Long reportedMemberId) {
        // reportedMember 필드로 직접 카운트
        Long count = queryFactory
                .select(violation.count())
                .from(violation)
                .where(violation.reportedMember.id.eq(reportedMemberId))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<ViolationResponse> findPendingViolations(CursorPageRequest request) {
        // TODO: 관리자용 대기 중인 신고 목록 조회 구현
        return List.of();
    }
}
