package tetoandeggens.seeyouagainbe.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}