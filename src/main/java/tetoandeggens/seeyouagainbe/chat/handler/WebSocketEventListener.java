package tetoandeggens.seeyouagainbe.chat.handler;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketEventListener {

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectedEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = headerAccessor.getSessionId();
		Long memberId = (Long)headerAccessor.getSessionAttributes().get("memberId");

		log.info("WebSocket 연결: sessionId={}, memberId={}", sessionId, memberId);
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = headerAccessor.getSessionId();
		Long memberId = (Long)headerAccessor.getSessionAttributes().get("memberId");

		log.info("WebSocket 연결 해제: sessionId={}, memberId={}", sessionId, memberId);
	}
}
