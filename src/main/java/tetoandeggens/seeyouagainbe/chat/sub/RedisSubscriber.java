package tetoandeggens.seeyouagainbe.chat.sub;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriber {

	private final ObjectMapper objectMapper;
	private final SimpMessageSendingOperations messagingTemplate;

	public void sendMessage(String publishMessage) {
		try {
			ChatMessageDto chatMessage = objectMapper.readValue(publishMessage, ChatMessageDto.class);

			messagingTemplate.convertAndSend(
				"/queue/chat-" + chatMessage.senderId(),
				chatMessage
			);

			messagingTemplate.convertAndSend(
				"/queue/chat-" + chatMessage.receiverId(),
				chatMessage
			);
		} catch (Exception e) {
			log.error("메시지 처리 실패: message={}", publishMessage, e);
		}
	}
}