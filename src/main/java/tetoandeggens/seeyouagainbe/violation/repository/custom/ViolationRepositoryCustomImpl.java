package tetoandeggens.seeyouagainbe.violation.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static tetoandeggens.seeyouagainbe.violation.entity.QViolation.violation;

@RequiredArgsConstructor
public class ViolationRepositoryCustomImpl implements ViolationRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByReporterAndBoard(Long reporterId, Long boardId) {
        return queryFactory
                .selectOne()
                .from(violation)
                .where(
                        violation.reporter.id.eq(reporterId),
                        violation.board.id.eq(boardId)
                )
                .fetchFirst() != null;
    }

    @Override
    public boolean existsByReporterAndChatRoom(Long reporterId, Long chatRoomId) {
        return queryFactory
                .selectOne()
                .from(violation)
                .where(
                        violation.reporter.id.eq(reporterId),
                        violation.chatRoom.id.eq(chatRoomId)
                )
                .fetchFirst() != null;
    }
}
