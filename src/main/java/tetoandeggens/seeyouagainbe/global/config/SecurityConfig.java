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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import tetoandeggens.seeyouagainbe.auth.filter.CustomLogoutFilter;
import tetoandeggens.seeyouagainbe.auth.filter.CustomUserLoginFilter;
import tetoandeggens.seeyouagainbe.auth.filter.JwtAuthenticationFilter;
import tetoandeggens.seeyouagainbe.auth.handler.CustomAccessDeniedHandler;
import tetoandeggens.seeyouagainbe.auth.handler.CustomAuthenticationEntryPoint;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.domain.member.entity.Role;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private static final String[] WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/reissue"
    };

    private static final String[] BLACKLIST = {
            "/api/auth/logout",
            "/api/auth/withdraw"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN.getRole())
                        .requestMatchers(WHITELIST).permitAll()
                        .requestMatchers(BLACKLIST).authenticated()
                        .anyRequest().authenticated())
                .addFilterAt(
                        new CustomUserLoginFilter(authenticationManager, tokenProvider, objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        new JwtAuthenticationFilter(tokenProvider, WHITELIST, BLACKLIST, objectMapper),
                        CustomUserLoginFilter.class)
                .addFilterBefore(
                        new CustomLogoutFilter(tokenProvider, objectMapper),
                        LogoutFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }
}
