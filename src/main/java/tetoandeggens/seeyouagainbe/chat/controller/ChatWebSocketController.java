package tetoandeggens.seeyouagainbe.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.dto.request.ChatMessageSendRequest;
import tetoandeggens.seeyouagainbe.chat.dto.request.ChatReadRequest;
import tetoandeggens.seeyouagainbe.chat.service.ChatService;

@Controller
@MessageMapping("/chat")
@RequiredArgsConstructor
public class ChatWebSocketController {

	private final ChatService chatService;

	@MessageMapping("/send")
	public void sendMessage(ChatMessageSendRequest request, Principal principal) {
		chatService.sendMessage(request, principal);
	}

	@MessageMapping("/read")
	public void markAsRead(ChatReadRequest readDto, Principal principal) {
		chatService.markAsRead(readDto.messageId(), principal);
	}
}