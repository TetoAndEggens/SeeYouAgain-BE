package tetoandeggens.seeyouagainbe.chat.sub;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;
import tetoandeggens.seeyouagainbe.chat.dto.ChatReadNotificationDto;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageWebSocketResponse;

@Profile("!test")
@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriber {

	private final ObjectMapper objectMapper;
	private final SimpMessageSendingOperations messagingTemplate;

	public void sendMessage(String publishMessage) {
		try {
			ChatMessageDto chatMessage = objectMapper.readValue(publishMessage, ChatMessageDto.class);
			ChatMessageWebSocketResponse response = ChatMessageWebSocketResponse.from(chatMessage);

			messagingTemplate.convertAndSendToUser(
				chatMessage.senderId().toString(),
				"/queue/chat",
				response
			);

			messagingTemplate.convertAndSendToUser(
				chatMessage.receiverId().toString(),
				"/queue/chat",
				response
			);
		} catch (Exception e) {
			log.error("메시지 처리 실패: message={}", publishMessage, e);
		}
	}

	public void sendReadNotification(String publishMessage) {
		try {
			ChatReadNotificationDto notification = objectMapper.readValue(publishMessage,
				ChatReadNotificationDto.class);

			messagingTemplate.convertAndSendToUser(
				notification.senderId().toString(),
				"/queue/chat/read",
				notification
			);
		} catch (Exception e) {
			log.error("읽음 알림 처리 실패: message={}", publishMessage, e);
		}
	}
}