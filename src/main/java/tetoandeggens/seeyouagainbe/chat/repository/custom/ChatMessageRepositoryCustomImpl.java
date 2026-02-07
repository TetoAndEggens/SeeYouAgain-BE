package tetoandeggens.seeyouagainbe.chat.repository.custom;

import java.util.List;
import java.util.Optional;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.QChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.QChatRoom;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.member.entity.QMember;

@RequiredArgsConstructor
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<ChatMessage> findMessagesByChatRoom(Long chatRoomId, Long cursorId, int size,
		SortDirection sortDirection) {
		QChatMessage chatMessage = QChatMessage.chatMessage;
		BooleanExpression cursorCondition = createCursorCondition(cursorId, sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QMember sender = new QMember("sender");
		QMember receiver = new QMember("receiver");
		QMember chatRoomSender = new QMember("chatRoomSender");
		QMember chatRoomReceiver = new QMember("chatRoomReceiver");

		return queryFactory
			.selectFrom(chatMessage)
			.join(chatMessage.chatRoom, QChatRoom.chatRoom).fetchJoin()
			.join(QChatRoom.chatRoom.sender, chatRoomSender).fetchJoin()
			.join(QChatRoom.chatRoom.receiver, chatRoomReceiver).fetchJoin()
			.join(chatMessage.sender, sender).fetchJoin()
			.join(chatMessage.receiver, receiver).fetchJoin()
			.where(
				chatMessage.chatRoom.id.eq(chatRoomId),
				cursorCondition
			)
			.orderBy(orderSpecifier)
			.limit(size + 1)
			.fetch();
	}

	@Override
	public void markAsReadByChatRoomAndReceiver(Long chatRoomId, Long memberId) {
		QChatMessage chatMessage = QChatMessage.chatMessage;

		queryFactory
			.update(chatMessage)
			.set(chatMessage.isRead, true)
			.where(
				chatMessage.chatRoom.id.eq(chatRoomId),
				chatMessage.receiver.id.eq(memberId),
				chatMessage.isRead.eq(false)
			)
			.execute();
	}

	@Override
	public Optional<ChatMessage> findByIdWithChatRoomAndMembersAndValidateAccess(Long messageId, Long memberId) {
		QChatMessage chatMessage = QChatMessage.chatMessage;
		QMember chatRoomSender = new QMember("chatRoomSender");
		QMember chatRoomReceiver = new QMember("chatRoomReceiver");

		ChatMessage result = queryFactory
			.selectFrom(chatMessage)
			.join(chatMessage.chatRoom, QChatRoom.chatRoom).fetchJoin()
			.join(QChatRoom.chatRoom.sender, chatRoomSender).fetchJoin()
			.join(QChatRoom.chatRoom.receiver, chatRoomReceiver).fetchJoin()
			.where(
				chatMessage.id.eq(messageId),
				QChatRoom.chatRoom.sender.id.eq(memberId)
					.or(QChatRoom.chatRoom.receiver.id.eq(memberId))
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public Optional<ChatMessage> findByIdWithMembers(Long messageId) {
		QChatMessage chatMessage = QChatMessage.chatMessage;
		QMember sender = new QMember("sender");
		QMember receiver = new QMember("receiver");

		ChatMessage result = queryFactory
			.selectFrom(chatMessage)
			.join(chatMessage.sender, sender).fetchJoin()
			.join(chatMessage.receiver, receiver).fetchJoin()
			.where(chatMessage.id.eq(messageId))
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public Long countUnreadMessagesByChatRoomAndReceiver(Long chatRoomId, Long memberId) {
		QChatMessage chatMessage = QChatMessage.chatMessage;

		return queryFactory
			.select(chatMessage.count())
			.from(chatMessage)
			.where(
				chatMessage.chatRoom.id.eq(chatRoomId),
				chatMessage.receiver.id.eq(memberId),
				chatMessage.isRead.eq(false)
			)
			.fetchOne();
	}

	private BooleanExpression createCursorCondition(Long cursorId, SortDirection sortDirection) {
		QChatMessage chatMessage = QChatMessage.chatMessage;

		if (cursorId == null) {
			return null;
		}
		return sortDirection == SortDirection.LATEST
			? chatMessage.id.loe(cursorId)
			: chatMessage.id.goe(cursorId);
	}

	private OrderSpecifier<Long> createOrderSpecifier(SortDirection sortDirection) {
		QChatMessage chatMessage = QChatMessage.chatMessage;

		return sortDirection == SortDirection.LATEST
			? chatMessage.id.desc()
			: chatMessage.id.asc();
	}

}