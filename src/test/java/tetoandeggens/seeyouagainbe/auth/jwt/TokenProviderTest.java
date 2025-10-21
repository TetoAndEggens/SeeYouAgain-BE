package tetoandeggens.seeyouagainbe.auth.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.domain.member.entity.Role;

@DisplayName("JWT 토큰 Provider 테스트")
@ExtendWith(MockitoExtension.class)
class TokenProviderTest {
    // Base64로 인코딩된 256비트(32바이트) 키
    private static final String SECRET_KEY = Base64.getEncoder().encodeToString(
            "testtesttesttesttesttesttesttesttest".getBytes(StandardCharsets.UTF_8)
    );
    private static final String UUID = "test-uuid-1234";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider(redisTemplate);

        // ReflectionTestUtils를 사용하여 private 필드 주입
        ReflectionTestUtils.setField(tokenProvider, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpirationMs", 3600000L); // 1시간
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationMs", 1209600000L); // 14일

        // @PostConstruct 메서드 수동 호출
        tokenProvider.init();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ============ Access Token 생성 테스트 ============

    @Test
    @DisplayName("Access Token을 성공적으로 생성한다")
    void createAccessToken_Success() {
        // when
        String accessToken = tokenProvider.createAccessToken(UUID, Role.USER.getRole());

        // then
        assertThat(accessToken).isNotBlank();

        Claims claims = tokenProvider.getClaimsFromToken(accessToken);
        assertThat(claims.getSubject()).isEqualTo(UUID);
        assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
    }

    // ============ Refresh Token 생성 테스트 ============

    @Test
    @DisplayName("Refresh Token을 성공적으로 생성한다")
    void createRefreshToken_Success() {
        // when
        String refreshToken = tokenProvider.createRefreshToken(UUID, Role.USER.getRole());

        // then
        assertThat(refreshToken).isNotBlank();

        Claims claims = tokenProvider.getClaimsFromToken(refreshToken);
        assertThat(claims.getSubject()).isEqualTo(UUID);
        assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
    }

    // ============ 토큰 검증 테스트 ============

    @Test
    @DisplayName("유효한 토큰을 검증한다")
    void validateToken_ValidToken_ReturnsTrue() {
        // given
        String token = tokenProvider.createAccessToken(UUID, Role.USER.getRole());

        // when
        boolean isValid = tokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 예외를 발생시킨다")
    void validateToken_ExpiredToken_ThrowsException() {
        // given
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

        String expiredToken = Jwts.builder()
                .subject(UUID)
                .claim("role", Role.USER.getRole())
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(secretKey)
                .compact();

        // when & then
        assertThatThrownBy(() -> tokenProvider.validateToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("잘못된 서명의 토큰은 예외를 발생시킨다")
    void validateToken_InvalidSignature_ThrowsException() {
        // given
        String invalidToken = "eyJhbGciOiJIUzUxMiJ9.invalid.signature";

        // when & then
        assertThatThrownBy(() -> tokenProvider.validateToken(invalidToken))
                .isInstanceOf(Exception.class);
    }

    // ============ Authentication 객체 생성 테스트 ============

    @Test
    @DisplayName("Access Token으로 Authentication 객체를 생성한다")
    void getAuthenticationByAccessToken_ValidToken_ReturnsAuthentication() {
        // given
        String accessToken = tokenProvider.createAccessToken(UUID, Role.ADMIN.getRole());

        // when
        Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

        // then
        assertThat(authentication).isNotNull();

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        assertThat(principal.getUuid()).isEqualTo(UUID);
        assertThat(principal.getRole()).isEqualTo(Role.ADMIN);
        assertThat(authentication.getAuthorities()).isNotEmpty();
        assertThat(authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(Role.ADMIN.getRole()))).isTrue();
    }

    // ============ Claims 추출 테스트 ============

    @Test
    @DisplayName("유효한 토큰에서 Claims를 추출한다")
    void getClaimsFromToken_ValidToken_ReturnsClaims() {
        // given
        String token = tokenProvider.createAccessToken(UUID, Role.USER.getRole());

        // when
        Claims claims = tokenProvider.getClaimsFromToken(token);

        // then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(UUID);
        assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("만료된 토큰에서도 Claims를 추출할 수 있다")
    void getClaimsFromToken_ExpiredToken_ReturnsClaims() {
        // given
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

        String expiredToken = Jwts.builder()
                .subject(UUID)
                .claim("role", Role.USER.getRole())
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(secretKey)
                .compact();

        // when
        Claims claims = tokenProvider.getClaimsFromToken(expiredToken);

        // then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(UUID);
    }

    // ============ Access Token 해결 테스트 ============

    @Test
    @DisplayName("Authorization 헤더에서 Access Token을 추출한다")
    void resolveAccessToken_Success() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String token = "test_access_token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // when
        String resolvedToken = tokenProvider.resolveAccessToken(request);

        // then
        assertThat(resolvedToken).isEqualTo(token);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 null 반환")
    void resolveAccessToken_NoHeader_ReturnsNull() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        String resolvedToken = tokenProvider.resolveAccessToken(request);

        // then
        assertThat(resolvedToken).isNull();
    }

    @Test
    @DisplayName("Bearer 접두사가 없으면 null 반환")
    void resolveAccessToken_NoBearerPrefix_ReturnsNull() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("InvalidToken");

        // when
        String resolvedToken = tokenProvider.resolveAccessToken(request);

        // then
        assertThat(resolvedToken).isNull();
    }

    // ============ Refresh Token 재발급 테스트 ============

    @Test
    @DisplayName("Refresh Token으로 Access Token 재발급 성공")
    void reissueAccessToken_Success() {
        // given
        String refreshToken = tokenProvider.createRefreshToken(UUID, Role.USER.getRole());

        when(valueOperations.get(UUID)).thenReturn(refreshToken);

        // when
        String newAccessToken = tokenProvider.reissueAccessToken(refreshToken);

        // then
        assertThat(newAccessToken).isNotBlank();

        Claims claims = tokenProvider.getClaimsFromToken(newAccessToken);
        assertThat(claims.getSubject()).isEqualTo(UUID);
        assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
    }

    @Test
    @DisplayName("저장된 Refresh Token이 없으면 예외 발생")
    void reissueAccessToken_ThrowsException_WhenStoredTokenNotFound() {
        // given
        String refreshToken = tokenProvider.createRefreshToken(UUID, Role.USER.getRole());

        when(valueOperations.get(UUID)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> tokenProvider.reissueAccessToken(refreshToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Refresh Token이 일치하지 않으면 예외 발생")
    void reissueAccessToken_ThrowsException_WhenTokenMismatch() {
        // given
        String refreshToken = tokenProvider.createRefreshToken(UUID, Role.USER.getRole());
        String differentToken = tokenProvider.createRefreshToken("different-uuid", Role.USER.getRole());

        when(valueOperations.get(UUID)).thenReturn(differentToken);

        // when & then
        assertThatThrownBy(() -> tokenProvider.reissueAccessToken(refreshToken))
                .isInstanceOf(Exception.class);
    }

    // ============ Refresh Token 삭제 테스트 ============

    @Test
    @DisplayName("Refresh Token 삭제 성공")
    void deleteRefreshToken_Success() {
        // given
        when(redisTemplate.delete(UUID)).thenReturn(true);

        // when & then
        assertDoesNotThrow(() -> tokenProvider.deleteRefreshToken(UUID));

        verify(redisTemplate).delete(UUID);
    }
}