package tetoandeggens.seeyouagainbe.global;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import tetoandeggens.seeyouagainbe.chat.sub.RedisSubscriber;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@Testcontainers
@Import(RedisTestContainer.TestRedisConfig.class)
public abstract class RedisTestContainer {

	@Container
	private static final GenericContainer<?> REDIS_CONTAINER =
		new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379)
			.withReuse(true);

	@DynamicPropertySource
	static void registerRedisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
	}

	@TestConfiguration
	public static class TestRedisConfig {

		@Bean
		public RedisConnectionFactory redisConnectionFactory() {
			String host = REDIS_CONTAINER.getHost();
			Integer port = REDIS_CONTAINER.getMappedPort(6379);

			RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
			config.setHostName(host);
			config.setPort(port);
			return new LettuceConnectionFactory(config);
		}

		@Bean
		public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
			RedisTemplate<String, String> template = new RedisTemplate<>();
			template.setConnectionFactory(redisConnectionFactory);
			template.setKeySerializer(new StringRedisSerializer());
			template.setValueSerializer(new StringRedisSerializer());
			return template;
		}

		@Bean
		public ChannelTopic channelTopic() {
			return new ChannelTopic("chatroom");
		}

		@Bean
		public ChannelTopic readChannelTopic() {
			return new ChannelTopic("chatread");
		}
	}
}