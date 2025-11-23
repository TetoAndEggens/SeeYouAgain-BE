package tetoandeggens.seeyouagainbe.chat.handler;

import static tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants.*;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

	private final TokenProvider tokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		StompCommand command = accessor.getCommand();

		if (StompCommand.CONNECT.equals(command)) {
			Long memberId = (Long)accessor.getSessionAttributes().get("memberId");

			if (memberId == null) {
				throw new CustomException(AuthErrorCode.INVALID_TOKEN);
			}
		} else if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
			String accessToken = (String)accessor.getSessionAttributes().get(ACCESS_TOKEN_COOKIE_NAME);

			if (accessToken == null) {
				throw new CustomException(AuthErrorCode.INVALID_TOKEN);
			}

			try {
				tokenProvider.validateToken(accessToken);
			} catch (Exception e) {
				throw new CustomException(AuthErrorCode.INVALID_TOKEN);
			}
		}

		return message;
	}
}