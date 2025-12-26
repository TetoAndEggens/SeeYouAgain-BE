package tetoandeggens.seeyouagainbe.chat.handler;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.chat.dto.WebSocketPrincipal;

@Slf4j
@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

	@Override
	protected Principal determineUser(
		ServerHttpRequest request,
		WebSocketHandler wsHandler,
		Map<String, Object> attributes
	) {
		Long memberId = (Long)attributes.get("memberId");
		String uuid = (String)attributes.get("uuid");

		if (memberId == null || uuid == null) {
			log.warn("memberId 또는 uuid가 없습니다. WebSocket 연결이 거부됩니다.");
			return null;
		}

		return new WebSocketPrincipal(memberId, uuid);
	}
}
