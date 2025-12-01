package tetoandeggens.seeyouagainbe.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.service-account.path}")
    private Resource serviceAccountResource;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountResource.getInputStream()))
                        .setProjectId(projectId)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase 초기화 완료 - Project ID: {}", projectId);
            } else {
                log.info("Firebase가 이미 초기화되어 있습니다.");
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화에 실패했습니다.", e);
        }
    }
}