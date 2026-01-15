package tetoandeggens.seeyouagainbe.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile({"local", "dev", "prod"})
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("현재 서버")
                ))
                .info(new Info()
                        .title("SeeYouAgain API Document")
                        .description("SeeYouAgain 서버 API 명세서입니다.")
                        .version("v1.0.0")
                ).components(authComponent());
    }

    private Components authComponent() {
        return new Components().addSecuritySchemes("jwt",
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .scheme("Bearer")
                        .bearerFormat("JWT"));
    }
}
