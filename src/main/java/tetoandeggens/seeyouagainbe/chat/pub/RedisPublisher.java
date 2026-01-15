package tetoandeggens.seeyouagainbe.chat.pub;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;
import tetoandeggens.seeyouagainbe.chat.dto.ChatReadNotificationDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

	private final ChannelTopic channelTopic;
	private final ChannelTopic readChannelTopic;
	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	public void publish(ChatMessageDto message) {
		try {
			String jsonMessage = objectMapper.writeValueAsString(message);
			redisTemplate.convertAndSend(channelTopic.getTopic(), jsonMessage);
		} catch (JsonProcessingException e) {
			log.error("메시지 발행 실패 (DB에는 저장됨): chatRoomId={}",
				message.chatRoomId(), e);
		}
	}

	public void publishReadNotification(ChatReadNotificationDto notification) {
		try {
			String jsonMessage = objectMapper.writeValueAsString(notification);
			redisTemplate.convertAndSend(readChannelTopic.getTopic(), jsonMessage);
		} catch (JsonProcessingException e) {
			log.error("읽음 알림 발행 실패: messageId={}",
				notification.messageId(), e);
		}
	}
}
