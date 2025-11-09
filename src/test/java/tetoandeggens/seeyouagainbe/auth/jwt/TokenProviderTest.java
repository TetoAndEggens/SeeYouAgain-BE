package tetoandeggens.seeyouagainbe.auth.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import tetoandeggens.seeyouagainbe.member.entity.Role;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

@DisplayName("TokenProvider 단위 테스트")
@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    private static final String SECRET_KEY = Base64.getEncoder().encodeToString(
            "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest".getBytes(StandardCharsets.UTF_8)
    );
    private static final String TEST_UUID = "test-uuid-1234";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1209600000L; // 14일

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest httpServletRequest;

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider(redisTemplate);
        ReflectionTestUtils.setField(tokenProvider, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION_MS);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION_MS);
        tokenProvider.init();

        // lenient()를 사용하여 모든 테스트에서 공통으로 사용되는 stub 설정
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("토큰 생성 테스트")
    class TokenCreationTests {
        @Test
        @DisplayName("Access Token 생성 - 성공")
        void createAccessToken_Success() {
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());

            assertThat(accessToken).isNotBlank();

            Claims claims = tokenProvider.getClaimsFromToken(accessToken);
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("Refresh Token 생성 - 성공")
        void createRefreshToken_Success() {
            String refreshToken = tokenProvider.createRefreshToken(TEST_UUID, Role.USER.getRole());

            assertThat(refreshToken).isNotBlank();

            Claims claims = tokenProvider.getClaimsFromToken(refreshToken);
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
        }

        @Test
        @DisplayName("ADMIN 권한으로 토큰 생성")
        void createToken_WithAdminRole() {
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, Role.ADMIN.getRole());

            Claims claims = tokenProvider.getClaimsFromToken(accessToken);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.ADMIN.getRole());
        }
    }

    @Nested
    @DisplayName("토큰 검증 테스트")
    class TokenValidationTests {
        @Test
        @DisplayName("유효한 토큰 검증 - 성공")
        void validateToken_ValidToken_ReturnsTrue() {
            String token = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());

            boolean isValid = tokenProvider.validateToken(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰 검증 - ExpiredJwtException 발생")
        void validateToken_ExpiredToken_ThrowsException() {
            byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
            SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

            String expiredToken = Jwts.builder()
                    .subject(TEST_UUID)
                    .claim("role", Role.USER.getRole())
                    .issuedAt(new Date(System.currentTimeMillis() - 10000))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(secretKey)
                    .compact();

            assertThatThrownBy(() -> tokenProvider.validateToken(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("잘못된 서명의 토큰 검증 - SignatureException 발생")
        void validateToken_InvalidSignature_ThrowsException() {
            String invalidToken = "eyJhbGciOiJIUzUxMiJ9.invalid.signature";

            assertThatThrownBy(() -> tokenProvider.validateToken(invalidToken))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("null 토큰 검증 - Exception 발생")
        void validateToken_NullToken_ThrowsException() {
            assertThatThrownBy(() -> tokenProvider.validateToken(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("빈 문자열 토큰 검증 - Exception 발생")
        void validateToken_EmptyToken_ThrowsException() {
            assertThatThrownBy(() -> tokenProvider.validateToken(""))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Claims 추출 테스트")
    class ClaimsExtractionTests {
        @Test
        @DisplayName("유효한 토큰에서 Claims 추출 - 성공")
        void getClaimsFromToken_ValidToken_ReturnsClaims() {
            String token = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());

            Claims claims = tokenProvider.getClaimsFromToken(token);

            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("만료된 토큰에서도 Claims 추출 가능")
        void getClaimsFromToken_ExpiredToken_ReturnsClaims() {
            byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
            SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

            String expiredToken = Jwts.builder()
                    .subject(TEST_UUID)
                    .claim("role", Role.USER.getRole())
                    .issuedAt(new Date(System.currentTimeMillis() - 10000))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(secretKey)
                    .compact();

            Claims claims = tokenProvider.getClaimsFromToken(expiredToken);

            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
        }
    }

    @Nested
    @DisplayName("Authentication 생성 테스트")
    class AuthenticationCreationTests {
        @Test
        @DisplayName("Access Token으로 Authentication 객체 생성 - USER 권한")
        void getAuthenticationByAccessToken_UserRole_ReturnsAuthentication() {
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, "USER");

            Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();

            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            assertThat(principal.getUuid()).isEqualTo(TEST_UUID);
            assertThat(principal.getRole()).isEqualTo(Role.USER);
            assertThat(authentication.getAuthorities()).isNotEmpty();
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly(Role.USER.getRole());
        }

        @Test
        @DisplayName("Access Token으로 Authentication 객체 생성 - ADMIN 권한")
        void getAuthenticationByAccessToken_AdminRole_ReturnsAuthentication() {
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, "ADMIN");

            Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            assertThat(principal.getRole()).isEqualTo(Role.ADMIN);
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly(Role.ADMIN.getRole()); // "ROLE_ADMIN"
        }
    }

    @Nested
    @DisplayName("토큰 추출 테스트")
    class TokenResolutionTests {
        @Test
        @DisplayName("Authorization 헤더에서 Access Token 추출 - 성공")
        void resolveAccessToken_ValidHeader_ReturnsToken() {
            String token = "test_access_token";
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

            String resolvedToken = tokenProvider.resolveAccessToken(httpServletRequest);

            assertThat(resolvedToken).isEqualTo(token);
        }

        @Test
        @DisplayName("Authorization 헤더가 없으면 null 반환")
        void resolveAccessToken_NoHeader_ReturnsNull() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

            String resolvedToken = tokenProvider.resolveAccessToken(httpServletRequest);

            assertThat(resolvedToken).isNull();
        }

        @Test
        @DisplayName("Bearer 접두사가 없으면 null 반환")
        void resolveAccessToken_NoBearerPrefix_ReturnsNull() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn("InvalidToken");

            String resolvedToken = tokenProvider.resolveAccessToken(httpServletRequest);

            assertThat(resolvedToken).isNull();
        }

        @Test
        @DisplayName("Bearer 접두사는 있지만 토큰이 없으면 빈 문자열 반환")
        void resolveAccessToken_BearerWithoutToken_ReturnsEmptyString() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer ");

            String resolvedToken = tokenProvider.resolveAccessToken(httpServletRequest);

            assertThat(resolvedToken).isEmpty();
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class TokenReissueTests {
        @Test
        @DisplayName("Refresh Token으로 Access Token 재발급 - 성공")
        void reissueAccessToken_ValidRefreshToken_ReturnsNewAccessToken() {
            String refreshToken = tokenProvider.createRefreshToken(TEST_UUID, Role.USER.getRole());
            when(valueOperations.get(TEST_UUID)).thenReturn(refreshToken);

            String newAccessToken = tokenProvider.reissueAccessToken(refreshToken);

            assertThat(newAccessToken).isNotBlank();

            Claims claims = tokenProvider.getClaimsFromToken(newAccessToken);
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
        }

        @Test
        @DisplayName("저장된 Refresh Token이 없으면 예외 발생")
        void reissueAccessToken_NoStoredToken_ThrowsException() {
            String refreshToken = tokenProvider.createRefreshToken(TEST_UUID, Role.USER.getRole());
            when(valueOperations.get(TEST_UUID)).thenReturn(null);

            assertThatThrownBy(() -> tokenProvider.reissueAccessToken(refreshToken))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("Refresh Token이 일치하지 않으면 예외 발생")
        void reissueAccessToken_TokenMismatch_ThrowsException() {
            String refreshToken = tokenProvider.createRefreshToken(TEST_UUID, Role.USER.getRole());
            String differentToken = tokenProvider.createRefreshToken("different-uuid", Role.USER.getRole());
            when(valueOperations.get(TEST_UUID)).thenReturn(differentToken);

            assertThatThrownBy(() -> tokenProvider.reissueAccessToken(refreshToken))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("만료된 Refresh Token으로 재발급 시도 - 예외 발생")
        void reissueAccessToken_ExpiredRefreshToken_ThrowsException() {
            byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
            SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

            String expiredRefreshToken = Jwts.builder()
                    .subject(TEST_UUID)
                    .claim("role", Role.USER.getRole())
                    .issuedAt(new Date(System.currentTimeMillis() - 20000))
                    .expiration(new Date(System.currentTimeMillis() - 10000))
                    .signWith(secretKey)
                    .compact();

            assertThatThrownBy(() -> tokenProvider.reissueAccessToken(expiredRefreshToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("Refresh Token 삭제 테스트")
    class RefreshTokenDeletionTests {
        @Test
        @DisplayName("Refresh Token 삭제 - 성공")
        void deleteRefreshToken_Success() {
            when(redisTemplate.delete(TEST_UUID)).thenReturn(true);

            assertThatCode(() -> tokenProvider.deleteUuid(TEST_UUID))
                    .doesNotThrowAnyException();

            verify(redisTemplate).delete(TEST_UUID);
        }

        @Test
        @DisplayName("존재하지 않는 Refresh Token 삭제 시도")
        void deleteRefreshToken_NonExistentToken() {
            when(redisTemplate.delete(TEST_UUID)).thenReturn(false);

            assertThatCode(() -> tokenProvider.deleteUuid(TEST_UUID))
                    .doesNotThrowAnyException();

            verify(redisTemplate).delete(TEST_UUID);
        }
    }
}