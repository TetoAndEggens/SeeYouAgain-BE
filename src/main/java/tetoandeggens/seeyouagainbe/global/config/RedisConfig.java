package tetoandeggens.seeyouagainbe.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tetoandeggens.seeyouagainbe.chat.sub.RedisSubscriber;

@Profile("!test")
@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private int port;

	@Bean
	public ChannelTopic channelTopic() {
		return new ChannelTopic("chatroom");
	}

	@Bean
	public ChannelTopic readChannelTopic() {
		return new ChannelTopic("chatread");
	}

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(host);
		config.setPort(port);
		return new LettuceConnectionFactory(config);
	}

	@Bean
	public RedisTemplate<String, String> redisTemplate() {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory());
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListener(
		MessageListenerAdapter listenerAdapterChatMessage,
		MessageListenerAdapter listenerAdapterChatReadNotification,
		ChannelTopic channelTopic,
		ChannelTopic readChannelTopic
	) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory());
		container.addMessageListener(listenerAdapterChatMessage, channelTopic);
		container.addMessageListener(listenerAdapterChatReadNotification, readChannelTopic);
		return container;
	}

	@Bean
	public MessageListenerAdapter listenerAdapterChatMessage(RedisSubscriber subscriber) {
		return new MessageListenerAdapter(subscriber, "sendMessage");
	}

	@Bean
	public MessageListenerAdapter listenerAdapterChatReadNotification(RedisSubscriber subscriber) {
		return new MessageListenerAdapter(subscriber, "sendReadNotification");
	}
}