package tetoandeggens.seeyouagainbe.auth.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.auth.service.RedisAuthService;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.entity.Role;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("TokenProvider 단위 테스트")
@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    private static final String SECRET_KEY = Base64.getEncoder().encodeToString(
            "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest".getBytes(StandardCharsets.UTF_8)
    );
    private static final String TEST_UUID = "test-uuid-1234";
    private static final Long TEST_MEMBER_ID = 1L;
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1209600000L; // 14일

    @Mock
    private RedisAuthService redisAuthService;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private TokenProvider tokenProvider;

    private Member createTestMember(Role testRole) {
        Member member = Member.builder()
                .loginId("testuser")
                .password("encodedPassword")
                .nickName("테스트유저")
                .phoneNumber("01012345678")
                .build();

        ReflectionTestUtils.setField(member, "role", testRole);
        ReflectionTestUtils.setField(member, "uuid", TEST_UUID);
        ReflectionTestUtils.setField(member, "id", TEST_MEMBER_ID);
        ReflectionTestUtils.setField(member, "isBanned", false);

        return member;
    }


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenProvider, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION_MS);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION_MS);
        tokenProvider.init();
    }

    @Nested
    @DisplayName("토큰 생성 테스트")
    class TokenCreationTests {

        @Test
        @DisplayName("로그인 토큰 쌍 생성 - 성공")
        void createLoginToken_Success() {
            // when
            UserTokenResponse tokenResponse = tokenProvider.createLoginToken(TEST_UUID, Role.USER);

            // then
            assertThat(tokenResponse).isNotNull();
            assertThat(tokenResponse.accessToken()).isNotBlank();
            assertThat(tokenResponse.refreshToken()).isNotBlank();

            Claims accessClaims = tokenProvider.parseClaims(tokenResponse.accessToken());
            assertThat(accessClaims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(accessClaims.get("role", String.class)).isEqualTo(Role.USER.getRole());

            Claims refreshClaims = tokenProvider.parseClaims(tokenResponse.refreshToken());
            assertThat(refreshClaims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(refreshClaims.get("role", String.class)).isEqualTo(Role.USER.getRole());
        }

        @Test
        @DisplayName("Access Token 생성 - 성공")
        void createAccessToken_Success() {
            // when
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());

            // then
            assertThat(accessToken).isNotBlank();

            Claims claims = tokenProvider.parseClaims(accessToken);
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("Refresh Token 생성 - 성공")
        void createRefreshToken_Success() {
            // when
            String refreshToken = tokenProvider.createRefreshToken(TEST_UUID, Role.USER.getRole());

            // then
            assertThat(refreshToken).isNotBlank();

            Claims claims = tokenProvider.parseClaims(refreshToken);
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
        }

        @Test
        @DisplayName("ADMIN 권한으로 토큰 생성")
        void createToken_WithAdminRole() {
            // when
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, Role.ADMIN.getRole());

            // then
            Claims claims = tokenProvider.parseClaims(accessToken);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.ADMIN.getRole());
        }

        @Test
        @DisplayName("소셜 임시 토큰 생성 - 성공")
        void createSocialTempToken_Success() {
            // given
            String provider = "kakao";
            String profileImageUrl = "https://example.com/profile.jpg";
            String tempUuid = "temp-uuid-123";

            // when
            String socialTempToken = tokenProvider.createSocialTempToken(provider, profileImageUrl, tempUuid);

            // then
            assertThat(socialTempToken).isNotBlank();

            Claims claims = tokenProvider.parseClaims(socialTempToken);
            assertThat(claims.get("provider", String.class)).isEqualTo(provider);
            assertThat(claims.get("profileImageUrl", String.class)).isEqualTo(profileImageUrl);
            assertThat(claims.get("tempUuid", String.class)).isEqualTo(tempUuid);
            assertThat(claims.get("type", String.class)).isEqualTo("social_temp");
        }

        @Test
        @DisplayName("소셜 임시 토큰 생성 - profileImageUrl이 null인 경우")
        void createSocialTempToken_WithNullProfileImage() {
            // given
            String provider = "naver";
            String tempUuid = "temp-uuid-456";

            // when
            String socialTempToken = tokenProvider.createSocialTempToken(provider, null, tempUuid);

            // then
            assertThat(socialTempToken).isNotBlank();

            Claims claims = tokenProvider.parseClaims(socialTempToken);
            assertThat(claims.get("profileImageUrl", String.class)).isEmpty();
        }
    }

    @Nested
    @DisplayName("토큰 검증 테스트")
    class TokenValidationTests {

        @Test
        @DisplayName("유효한 토큰 검증 - 성공")
        void validateToken_ValidToken_Success() {
            // given
            String token = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());

            // when & then
            assertThatCode(() -> tokenProvider.validateToken(token))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("만료된 토큰 검증 - CustomException 발생")
        void validateToken_ExpiredToken_ThrowsException() {
            // given
            byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
            SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

            String expiredToken = Jwts.builder()
                    .subject(TEST_UUID)
                    .claim("role", Role.USER.getRole())
                    .issuedAt(new Date(System.currentTimeMillis() - 10000))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(secretKey)
                    .compact();

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(expiredToken))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("잘못된 서명의 토큰 검증 - CustomException 발생")
        void validateToken_InvalidSignature_ThrowsException() {
            // given
            String invalidToken = "eyJhbGciOiJIUzUxMiJ9.invalid.signature";

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(invalidToken))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("null 토큰 검증 - CustomException 발생")
        void validateToken_NullToken_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(null))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("빈 문자열 토큰 검증 - CustomException 발생")
        void validateToken_EmptyToken_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> tokenProvider.validateToken(""))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("Claims 추출 테스트")
    class ClaimsExtractionTests {

        @Test
        @DisplayName("유효한 토큰에서 Claims 추출 - 성공")
        void parseClaims_ValidToken_ReturnsClaims() {
            // given
            String token = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());

            // when
            Claims claims = tokenProvider.parseClaims(token);

            // then
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.getRole());
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("만료된 토큰에서도 Claims 추출 가능")
        void parseClaims_ExpiredToken_ReturnsClaims() {
            // given
            byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
            SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

            String expiredToken = Jwts.builder()
                    .subject(TEST_UUID)
                    .claim("role", Role.USER.getRole())
                    .issuedAt(new Date(System.currentTimeMillis() - 10000))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(secretKey)
                    .compact();

            // when
            Claims claims = tokenProvider.parseClaims(expiredToken);

            // then
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(TEST_UUID);
        }

        @Test
        @DisplayName("잘못된 토큰에서 Claims 추출 - CustomException 발생")
        void parseClaims_InvalidToken_ThrowsException() {
            // given
            String invalidToken = "invalid.token.here";

            // when & then
            assertThatThrownBy(() -> tokenProvider.parseClaims(invalidToken))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("Authentication 생성 테스트")
    class AuthenticationCreationTests {

        @Test
        @DisplayName("Access Token으로 Authentication 객체 생성 - USER 권한")
        void getAuthenticationByAccessToken_UserRole_ReturnsAuthentication() {
            // given
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());
            Member testMember = createTestMember(Role.USER);

            given(redisAuthService.getMemberId(TEST_UUID)).willReturn(Optional.of(TEST_MEMBER_ID));
            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.of(testMember));

            // when
            Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

            // then
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();

            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            assertThat(principal.getUuid()).isEqualTo(TEST_UUID);
            assertThat(principal.getMemberId()).isEqualTo(TEST_MEMBER_ID);
            assertThat(principal.getRole()).isEqualTo(Role.USER);
            assertThat(authentication.getAuthorities()).isNotEmpty();
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly(Role.USER.getRole());

            verify(redisAuthService).getMemberId(TEST_UUID);
            verify(memberRepository).findByIdAndIsDeletedFalse(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("Access Token으로 Authentication 객체 생성 - ADMIN 권한")
        void getAuthenticationByAccessToken_AdminRole_ReturnsAuthentication() {
            // given
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, Role.ADMIN.getRole());
            Member testMember = createTestMember(Role.ADMIN);

            given(redisAuthService.getMemberId(TEST_UUID)).willReturn(Optional.of(TEST_MEMBER_ID));
            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.of(testMember));

            // when
            Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

            // then
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            assertThat(principal.getMemberId()).isEqualTo(TEST_MEMBER_ID);
            assertThat(principal.getRole()).isEqualTo(Role.ADMIN);
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly(Role.ADMIN.getRole());

            verify(redisAuthService).getMemberId(TEST_UUID);
            verify(memberRepository).findByIdAndIsDeletedFalse(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("Redis에 memberId가 없으면 MEMBER_NOT_FOUND 예외 발생")
        void getAuthenticationByAccessToken_ThrowsException_WhenMemberIdNotFoundInRedis() {
            // given
            String accessToken = tokenProvider.createAccessToken(TEST_UUID, Role.USER.getRole());
            given(redisAuthService.getMemberId(TEST_UUID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenProvider.getAuthenticationByAccessToken(accessToken))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode.MEMBER_NOT_FOUND);

            verify(redisAuthService).getMemberId(TEST_UUID);
        }
    }

    @Nested
    @DisplayName("토큰 만료 시간 조회 테스트")
    class TokenExpirationTests {

        @Test
        @DisplayName("Access Token 만료 시간(초) 조회")
        void getAccessTokenExpirationSec_ReturnsCorrectValue() {
            // when
            long expirationSec = tokenProvider.getAccessTokenExpirationSec();

            // then
            assertThat(expirationSec).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS / 1000);
        }

        @Test
        @DisplayName("Refresh Token 만료 시간(초) 조회")
        void getRefreshTokenExpirationSec_ReturnsCorrectValue() {
            // when
            long expirationSec = tokenProvider.getRefreshTokenExpirationSec();

            // then
            assertThat(expirationSec).isEqualTo(REFRESH_TOKEN_EXPIRATION_MS / 1000);
        }

        @Test
        @DisplayName("Refresh Token 만료 시간(밀리초) 조회")
        void getRefreshTokenExpirationMs_ReturnsCorrectValue() {
            // when
            long expirationMs = tokenProvider.getRefreshTokenExpirationMs();

            // then
            assertThat(expirationMs).isEqualTo(REFRESH_TOKEN_EXPIRATION_MS);
        }
    }
}