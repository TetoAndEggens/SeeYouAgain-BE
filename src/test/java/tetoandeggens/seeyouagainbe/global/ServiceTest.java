package tetoandeggens.seeyouagainbe.global;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.global.config.FirebaseConfig;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public abstract class ServiceTest {

	@MockitoBean
	protected RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	protected ChannelTopic channelTopic;

    @MockitoBean
    protected FirebaseConfig firebaseConfig;
}