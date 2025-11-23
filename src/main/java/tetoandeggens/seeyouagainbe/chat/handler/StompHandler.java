package tetoandeggens.seeyouagainbe.chat.handler;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final TokenProvider tokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String token = extractToken(accessor);

			if (token == null) {
				throw new CustomException(AuthErrorCode.INVALID_TOKEN);
			}

			tokenProvider.validateToken(token);
			Authentication authentication = tokenProvider.getAuthenticationByAccessToken(token);
			accessor.setUser(authentication);
		}

		return message;
	}

	private String extractToken(StompHeaderAccessor accessor) {
		String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

		if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
			return authHeader.substring(BEARER_PREFIX.length());
		}

		return null;
	}
}