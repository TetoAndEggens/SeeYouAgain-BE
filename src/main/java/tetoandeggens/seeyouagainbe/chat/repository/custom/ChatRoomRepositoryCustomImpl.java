package tetoandeggens.seeyouagainbe.chat.repository.custom;

import static tetoandeggens.seeyouagainbe.chat.entity.QChatRoom.*;

import java.util.Optional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;

@RequiredArgsConstructor
public class ChatRoomRepositoryCustomImpl implements ChatRoomRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<ChatRoom> findByBoardAndMembers(Long boardId, Long senderId, Long receiverId) {
		ChatRoom result = queryFactory
			.selectFrom(chatRoom)
			.where(
				chatRoom.board.id.eq(boardId),
				chatRoom.sender.id.eq(senderId).and(chatRoom.receiver.id.eq(receiverId))
					.or(chatRoom.sender.id.eq(receiverId).and(chatRoom.receiver.id.eq(senderId)))
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}
}