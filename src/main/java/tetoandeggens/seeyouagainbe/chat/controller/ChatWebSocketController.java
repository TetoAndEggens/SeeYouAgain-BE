package tetoandeggens.seeyouagainbe.chat.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;
import tetoandeggens.seeyouagainbe.chat.pub.RedisPublisher;
import tetoandeggens.seeyouagainbe.chat.service.ChatService;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

	private final ChatService chatService;
	private final RedisPublisher redisPublisher;

	@MessageMapping("/chat/send")
	public void sendMessage(ChatMessageDto message) {
		ChatMessageDto savedMessage = chatService.saveMessage(message);

		redisPublisher.publish(savedMessage);
	}
}