package tetoandeggens.seeyouagainbe.chat.repository.custom;

import java.util.Optional;

import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;

public interface ChatRoomRepositoryCustom {

	Optional<ChatRoom> findByBoardAndMembers(Long boardId, Long senderId, Long receiverId);
}