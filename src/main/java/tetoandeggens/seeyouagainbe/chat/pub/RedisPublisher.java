package tetoandeggens.seeyouagainbe.chat.pub;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

	private final ChannelTopic channelTopic;
	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	public void publish(ChatMessageDto message) {
		try {
			String jsonMessage = objectMapper.writeValueAsString(message);
			redisTemplate.convertAndSend(channelTopic.getTopic(), jsonMessage);
		} catch (JsonProcessingException e) {
			log.error("메시지 발행 실패 (DB에는 저장됨): chatRoomId={}, messageType={}",
				message.chatRoomId(), message.messageType(), e);
		}
	}
}
