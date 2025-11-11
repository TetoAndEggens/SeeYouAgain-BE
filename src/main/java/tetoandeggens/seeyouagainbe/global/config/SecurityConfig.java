package tetoandeggens.seeyouagainbe.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tetoandeggens.seeyouagainbe.auth.filter.CustomLogoutFilter;
import tetoandeggens.seeyouagainbe.auth.filter.CustomLoginFilter;
import tetoandeggens.seeyouagainbe.auth.filter.JwtAuthenticationFilter;
import tetoandeggens.seeyouagainbe.auth.handler.CustomAccessDeniedHandler;
import tetoandeggens.seeyouagainbe.auth.handler.CustomAuthenticationEntryPoint;
import tetoandeggens.seeyouagainbe.auth.oauth2.handler.OAuth2AuthenticationFailureHandler;
import tetoandeggens.seeyouagainbe.auth.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.service.CustomOAuth2UserService;
import tetoandeggens.seeyouagainbe.auth.service.CookieService;
import tetoandeggens.seeyouagainbe.auth.service.RedisAuthService;
import tetoandeggens.seeyouagainbe.member.entity.Role;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final CookieService cookieService;
    private final RedisAuthService redisAuthService;
    private final ObjectMapper objectMapper;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    private static final String[] WHITE_LIST = {
            "/auth/**",
            "/login/oauth2/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**",
            "/abandoned-animal/**"
    };

    private static final String[] BLACK_LIST = {
            "/auth/logout",
            "/auth/withdrawal"
    };

    private static final String[] ADMINLIST = {
            "/admin/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "https://dev-api.seeyouagain.store",
                "http://localhost:3000"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(BLACK_LIST).authenticated()
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers(ADMINLIST).hasAuthority(Role.ADMIN.getRole())
                        .anyRequest().authenticated())

                .addFilterAt(
                        new CustomLoginFilter(
                                authenticationManager,
                                tokenProvider,
                                cookieService,
                                redisAuthService,
                                objectMapper
                        ),
                        UsernamePasswordAuthenticationFilter.class)

                .addFilterAfter(
                        new JwtAuthenticationFilter(
                                tokenProvider,
                                cookieService,
                                WHITE_LIST,
                                BLACK_LIST,
                                objectMapper
                        ),
                        CustomLoginFilter.class)

                .addFilterBefore(
                        new CustomLogoutFilter(
                                tokenProvider,
                                cookieService,
                                redisAuthService,
                                objectMapper
                        ),
                        LogoutFilter.class)

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }
}