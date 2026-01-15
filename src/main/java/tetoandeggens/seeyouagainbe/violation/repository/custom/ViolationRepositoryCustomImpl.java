package tetoandeggens.seeyouagainbe.violation.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationListResponse;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;

import java.util.List;
import java.util.Optional;

import static tetoandeggens.seeyouagainbe.violation.entity.QViolation.violation;

@Repository
@RequiredArgsConstructor
public class ViolationRepositoryCustomImpl implements ViolationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByReporterAndBoard(Long reporterId, Long boardId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(violation)
                .where(
                        violation.reporter.id.eq(reporterId),
                        violation.board.id.eq(boardId)
                )
                .fetchFirst();

        return fetchOne != null;
    }

    @Override
    public boolean existsByReporterAndChatRoom(Long reporterId, Long chatRoomId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(violation)
                .where(
                        violation.reporter.id.eq(reporterId),
                        violation.chatRoom.id.eq(chatRoomId)
                )
                .fetchFirst();

        return fetchOne != null;
    }

    @Override
    public Page<ViolationListResponse> findViolationList(
            ViolatedStatus status,
            Pageable pageable
    ) {
        List<ViolationListResponse> content = queryFactory
                .select(Projections.constructor(
                        ViolationListResponse.class,
                        violation.id,
                        violation.violatedStatus,
                        violation.reason,
                        violation.reporter.nickName,
                        violation.reportedMember.nickName,
                        // CaseBuilder를 사용한 조건부 표현식
                        new CaseBuilder()
                                .when(violation.board.id.isNotNull())
                                .then("BOARD")
                                .otherwise("CHATROOM"),
                        // board가 있으면 boardId, 없으면 chatRoomId
                        violation.board.id.coalesce(violation.chatRoom.id),
                        violation.createdAt
                ))
                .from(violation)
                .join(violation.reporter)
                .join(violation.reportedMember)
                .where(statusEq(status))
                .orderBy(violation.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = getTotalCount(status);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<Violation> findByIdWithAllFetch(Long violationId) {
        Violation result = queryFactory
                .selectFrom(violation)
                .distinct()  // 중복 엔티티 제거
                .join(violation.reporter).fetchJoin()
                .join(violation.reportedMember).fetchJoin()
                .leftJoin(violation.board).fetchJoin()
                .leftJoin(violation.chatRoom).fetchJoin()
                .where(violation.id.eq(violationId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private long getTotalCount(ViolatedStatus status) {
        Long count = queryFactory
                .select(violation.count())
                .from(violation)
                .where(statusEq(status))
                .fetchOne();

        if (count == null) {
            return 0L;
        }
        return count;
    }

    private BooleanExpression statusEq(ViolatedStatus status) {
        return status != null ? violation.violatedStatus.eq(status) : null;
    }
}