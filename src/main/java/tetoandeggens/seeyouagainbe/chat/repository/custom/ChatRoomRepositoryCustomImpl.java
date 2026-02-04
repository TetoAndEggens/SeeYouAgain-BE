package tetoandeggens.seeyouagainbe.chat.repository.custom;

import static tetoandeggens.seeyouagainbe.board.entity.QBoard.*;
import static tetoandeggens.seeyouagainbe.chat.entity.QChatRoom.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

		// 1. 채팅방 기본 정보 조회 (lastMessage 제외)
		List<ChatRoomResponse> chatRooms = queryFactory
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
						Expressions.nullExpression(String.class),  // lastMessage는 나중에 설정
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

		// 2. 각 채팅방의 최신 메시지 조회
		if (!chatRooms.isEmpty()) {
			List<Long> chatRoomIds = chatRooms.stream()
					.map(ChatRoomResponse::chatRoomId)
					.collect(Collectors.toList());

			QChatMessage cm = QChatMessage.chatMessage;

			// 각 채팅방의 최신 메시지 ID 조회
			List<Long> latestMessageIds = queryFactory
					.select(cm.id.max())
					.from(cm)
					.where(cm.chatRoom.id.in(chatRoomIds))
					.groupBy(cm.chatRoom.id)
					.fetch();

			// 최신 메시지들 조회
			if (!latestMessageIds.isEmpty()) {
				Map<Long, String> lastMessages = queryFactory
						.select(cm.chatRoom.id, cm.content)
						.from(cm)
						.where(cm.id.in(latestMessageIds))
						.fetch()
						.stream()
						.collect(Collectors.toMap(
								tuple -> tuple.get(cm.chatRoom.id),
								tuple -> tuple.get(cm.content)
						));

				// 3. ChatRoomResponse에 lastMessage 설정
				return chatRooms.stream()
						.map(room -> new ChatRoomResponse(
								room.chatRoomId(),
								room.boardId(),
								room.boardTitle(),
								room.contentType(),
								room.senderId(),
								room.receiverId(),
								room.otherMemberNickname(),
								lastMessages.get(room.chatRoomId()),  // lastMessage 설정
								room.lastMessageTime(),
								room.unreadCount()
						))
						.collect(Collectors.toList());
			}
		}

		return chatRooms;
	}

	@Override
	public List<ChatRoomResponse> findUnreadChatRoomsWithDetails(Long memberId, Long cursorId, int size,
																 SortDirection sortDirection) {
		BooleanExpression cursorCondition = createCursorCondition(cursorId, sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QChatMessage subMessage = new QChatMessage("subMessage");
		QMember sender = new QMember("sender");
		QMember receiver = new QMember("receiver");

		// 1. 채팅방 기본 정보 조회 (lastMessage 제외)
		List<ChatRoomResponse> chatRooms = queryFactory
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
						Expressions.nullExpression(String.class),  // lastMessage는 나중에 설정
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

		// 2. 각 채팅방의 최신 메시지 조회
		if (!chatRooms.isEmpty()) {
			List<Long> chatRoomIds = chatRooms.stream()
					.map(ChatRoomResponse::chatRoomId)
					.collect(Collectors.toList());

			QChatMessage cm = QChatMessage.chatMessage;

			// 각 채팅방의 최신 메시지 ID 조회
			List<Long> latestMessageIds = queryFactory
					.select(cm.id.max())
					.from(cm)
					.where(cm.chatRoom.id.in(chatRoomIds))
					.groupBy(cm.chatRoom.id)
					.fetch();

			// 최신 메시지들 조회
			if (!latestMessageIds.isEmpty()) {
				Map<Long, String> lastMessages = queryFactory
						.select(cm.chatRoom.id, cm.content)
						.from(cm)
						.where(cm.id.in(latestMessageIds))
						.fetch()
						.stream()
						.collect(Collectors.toMap(
								tuple -> tuple.get(cm.chatRoom.id),
								tuple -> tuple.get(cm.content)
						));

				// 3. ChatRoomResponse에 lastMessage 설정
				return chatRooms.stream()
						.map(room -> new ChatRoomResponse(
								room.chatRoomId(),
								room.boardId(),
								room.boardTitle(),
								room.contentType(),
								room.senderId(),
								room.receiverId(),
								room.otherMemberNickname(),
								lastMessages.get(room.chatRoomId()),  // lastMessage 설정
								room.lastMessageTime(),
								room.unreadCount()
						))
						.collect(Collectors.toList());
			}
		}

		return chatRooms;
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