package tetoandeggens.seeyouagainbe.global;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import tetoandeggens.seeyouagainbe.global.config.QueryDslConfig;
import tetoandeggens.seeyouagainbe.global.util.AesEncryptionConverter;
import tetoandeggens.seeyouagainbe.global.util.AesEncryptionUtil;

@EnableJpaAuditing
@DataJpaTest
@ActiveProfiles("test")
@Import({QueryDslConfig.class, AesEncryptionUtil.class, AesEncryptionConverter.class})
public abstract class RepositoryTest {
}