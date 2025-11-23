package tetoandeggens.seeyouagainbe.chat.handler;

import static tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants.*;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

	private final TokenProvider tokenProvider;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
		WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest servletRequest = (ServletServerHttpRequest)request;
			HttpServletRequest httpRequest = servletRequest.getServletRequest();

			String accessToken = extractTokenFromCookies(httpRequest.getCookies());

			if (accessToken != null) {
				try {
					tokenProvider.validateToken(accessToken);
					Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

					CustomUserDetails userDetails = (CustomUserDetails)authentication.getPrincipal();
					Long memberId = userDetails.getMemberId();

					attributes.put("memberId", memberId);
					attributes.put(ACCESS_TOKEN_COOKIE_NAME, accessToken);

					return true;
				} catch (Exception e) {
					log.error("WebSocket 핸드셰이크 실패: 토큰 검증 오류", e);
					return false;
				}
			} else {
				log.error("WebSocket 핸드셰이크 실패: 토큰을 찾을 수 없음");
				return false;
			}
		}

		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
		WebSocketHandler wsHandler, Exception exception) {
	}

	private String extractTokenFromCookies(Cookie[] cookies) {
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}
}