package tetoandeggens.seeyouagainbe.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.custom.ChatRoomRepositoryCustom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

	@Modifying
	@Query("update ChatRoom c set c.isDeleted = true where c.board.id = :boardId")
	void softDeleteByBoardId(Long boardId);
}
