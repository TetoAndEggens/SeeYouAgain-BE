package tetoandeggens.seeyouagainbe.chat.handler;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import lombok.extern.slf4j.Slf4j;

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

		if (memberId == null) {
			log.warn("memberId가 없습니다. WebSocket 연결이 거부됩니다.");
			return null;
		}

		return new UsernamePasswordAuthenticationToken(
			memberId.toString(),
			null,
			null
		);
	}
}
