package tetoandeggens.seeyouagainbe.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tetoandeggens.seeyouagainbe.auth.dto.ImapCredentials;

import java.util.Properties;

@Configuration
public class ImapConfig {
    @Value("${spring.mail.properties.mail.imap.host}")
    private String host;

    @Value("${spring.mail.properties.mail.imap.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.imap.ssl.enable}")
    private boolean sslEnable;

    @Value("${spring.mail.properties.mail.imap.connectiontimeout}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.imap.timeout}")
    private int timeout;

    @Bean
    public Properties imapConnectionProperties() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", String.valueOf(port));
        props.put("mail.imaps.ssl.enable", sslEnable);
        props.put("mail.imaps.connectiontimeout", connectionTimeout);
        props.put("mail.imaps.timeout", timeout);
        return props;
    }

    @Bean
    public ImapCredentials imapCredentials() {
        return new ImapCredentials(username, password);
    }
}
