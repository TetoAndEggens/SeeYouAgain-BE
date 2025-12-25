package tetoandeggens.seeyouagainbe.chat.repository.custom;

import static tetoandeggens.seeyouagainbe.board.entity.QBoard.*;
import static tetoandeggens.seeyouagainbe.chat.entity.QChatRoom.*;

import java.util.List;
import java.util.Optional;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.entity.QChatMessage;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.member.entity.QMember;

@RequiredArgsConstructor
public class ChatRoomRepositoryCustomImpl implements ChatRoomRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<Long> findChatRoomIdByBoardAndMembers(Long boardId, Long senderId, Long receiverId) {
		Long result = queryFactory
			.select(chatRoom.id)
			.from(chatRoom)
			.where(
				chatRoom.board.id.eq(boardId),
				chatRoom.sender.id.eq(senderId),
				chatRoom.receiver.id.eq(receiverId)
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public Optional<ChatRoom> findByIdWithMembers(Long chatRoomId) {
		QMember sender = new QMember("sender");
		QMember receiver = new QMember("receiver");

		ChatRoom result = queryFactory
			.selectFrom(chatRoom)
			.join(chatRoom.sender, sender).fetchJoin()
			.join(chatRoom.receiver, receiver).fetchJoin()
			.where(chatRoom.id.eq(chatRoomId))
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public Optional<ChatRoom> findByIdWithMembersAndValidateAccess(Long chatRoomId, Long memberId) {
		QMember sender = new QMember("sender");
		QMember receiver = new QMember("receiver");

		ChatRoom result = queryFactory
			.selectFrom(chatRoom)
			.join(chatRoom.sender, sender).fetchJoin()
			.join(chatRoom.receiver, receiver).fetchJoin()
			.where(
				chatRoom.id.eq(chatRoomId),
				chatRoom.sender.id.eq(memberId)
					.or(chatRoom.receiver.id.eq(memberId))
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public List<ChatRoomResponse> findChatRoomsWithDetails(Long memberId, Long cursorId, int size,
		SortDirection sortDirection) {
		BooleanExpression cursorCondition = createCursorCondition(cursorId, sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QChatMessage subMessage = new QChatMessage("subMessage");
		QMember sender = new QMember("sender");
		QMember receiver = new QMember("receiver");

		return queryFactory
			.select(Projections.constructor(
				ChatRoomResponse.class,
				chatRoom.id,
				board.id,
				board.title,
				board.contentType,
				chatRoom.sender.id,
				chatRoom.receiver.id,
				Expressions.cases()
					.when(chatRoom.sender.id.eq(memberId))
					.then(receiver.nickName)
					.otherwise(sender.nickName),
				JPAExpressions
					.select(subMessage.content)
					.from(subMessage)
					.where(subMessage.chatRoom.eq(chatRoom))
					.orderBy(subMessage.createdAt.desc())
					.limit(1),
				chatRoom.lastMessageAt,
				JPAExpressions
					.select(subMessage.count())
					.from(subMessage)
					.where(
						subMessage.chatRoom.eq(chatRoom),
						subMessage.sender.id.ne(memberId),
						subMessage.isRead.eq(false)
					)
			))
			.from(chatRoom)
			.join(chatRoom.board, board)
			.join(chatRoom.sender, sender)
			.join(chatRoom.receiver, receiver)
			.where(
				chatRoom.sender.id.eq(memberId)
					.or(chatRoom.receiver.id.eq(memberId)),
				cursorCondition
			)
			.orderBy(orderSpecifier)
			.limit(size + 1)
			.fetch();
	}

	@Override
	public List<ChatRoomResponse> findUnreadChatRoomsWithDetails(Long memberId, Long cursorId, int size,
		SortDirection sortDirection) {
		BooleanExpression cursorCondition = createCursorCondition(cursorId, sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QChatMessage subMessage = new QChatMessage("subMessage");
		QMember sender = new QMember("sender");
		QMember receiver = new QMember("receiver");

		return queryFactory
			.select(Projections.constructor(
				ChatRoomResponse.class,
				chatRoom.id,
				board.id,
				board.title,
				board.contentType,
				chatRoom.sender.id,
				chatRoom.receiver.id,
				Expressions.cases()
					.when(chatRoom.sender.id.eq(memberId))
					.then(receiver.nickName)
					.otherwise(sender.nickName),
				JPAExpressions
					.select(subMessage.content)
					.from(subMessage)
					.where(subMessage.chatRoom.eq(chatRoom))
					.orderBy(subMessage.createdAt.desc())
					.limit(1),
				chatRoom.lastMessageAt,
				JPAExpressions
					.select(subMessage.count())
					.from(subMessage)
					.where(
						subMessage.chatRoom.eq(chatRoom),
						subMessage.sender.id.ne(memberId),
						subMessage.isRead.eq(false)
					)
			))
			.from(chatRoom)
			.join(chatRoom.board, board)
			.join(chatRoom.sender, sender)
			.join(chatRoom.receiver, receiver)
			.where(
				chatRoom.sender.id.eq(memberId)
					.or(chatRoom.receiver.id.eq(memberId)),
				chatRoom.id.in(
					JPAExpressions
						.select(subMessage.chatRoom.id)
						.from(subMessage)
						.where(
							subMessage.sender.id.ne(memberId),
							subMessage.isRead.eq(false)
						)
				),
				cursorCondition
			)
			.orderBy(orderSpecifier)
			.limit(size + 1)
			.fetch();
	}

	private BooleanExpression createCursorCondition(Long cursorId, SortDirection sortDirection) {
		if (cursorId == null) {
			return null;
		}
		return sortDirection == SortDirection.LATEST
			? chatRoom.id.lt(cursorId)
			: chatRoom.id.gt(cursorId);
	}

	private OrderSpecifier<Long> createOrderSpecifier(SortDirection sortDirection) {
		return sortDirection == SortDirection.LATEST
			? chatRoom.id.desc()
			: chatRoom.id.asc();
	}
}