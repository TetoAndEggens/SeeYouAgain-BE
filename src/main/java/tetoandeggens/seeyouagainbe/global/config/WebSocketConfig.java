package tetoandeggens.seeyouagainbe.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.handler.CustomHandshakeHandler;
import tetoandeggens.seeyouagainbe.chat.handler.StompHandler;
import tetoandeggens.seeyouagainbe.chat.handler.WebSocketHandshakeInterceptor;

@Profile("!test")
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompHandler stompHandler;
	private final WebSocketHandshakeInterceptor handshakeInterceptor;
	private final CustomHandshakeHandler customHandshakeHandler;

	@Bean
	public TaskScheduler messageBrokerTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("ws-heartbeat-");
		scheduler.initialize();
		return scheduler;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/queue")
			.setHeartbeatValue(new long[] {30000, 30000})
			.setTaskScheduler(messageBrokerTaskScheduler());
		config.setApplicationDestinationPrefixes("/pub");
		config.setUserDestinationPrefix("/member");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws-stomp")
			.setAllowedOriginPatterns("*")
			.setHandshakeHandler(customHandshakeHandler)
			.addInterceptors(handshakeInterceptor)
			.withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompHandler);
	}
}