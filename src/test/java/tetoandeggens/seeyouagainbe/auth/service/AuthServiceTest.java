package tetoandeggens.seeyouagainbe.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tetoandeggens.seeyouagainbe.auth.dto.request.UnifiedRegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.request.WithdrawalRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2UnlinkServiceProvider;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@DisplayName("AuthService 단위 테스트")
class AuthServiceTest extends ServiceTest {

    private static final String TEST_LOGIN_ID = "testuser123";
    private static final String TEST_PHONE = "01012345678";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_NICKNAME = "테스트";
    private static final String TEST_UUID = "test-uuid-123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";

    @Autowired
    private AuthService authService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private RedisAuthService redisAuthService;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private SocialAccountLinkStrategy socialAccountLinkStrategy;

    @MockitoBean
    private HttpServletRequest request;

    @MockitoBean
    private HttpServletResponse response;

    private Map<String, OAuth2UnlinkServiceProvider> unlinkServices;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        OAuth2UnlinkServiceProvider kakaoUnlinkService = mock(OAuth2UnlinkServiceProvider.class);
        OAuth2UnlinkServiceProvider naverUnlinkService = mock(OAuth2UnlinkServiceProvider.class);
        OAuth2UnlinkServiceProvider googleUnlinkService = mock(OAuth2UnlinkServiceProvider.class);

        unlinkServices = new HashMap<>();
        unlinkServices.put("kakaoUnlinkService", kakaoUnlinkService);
        unlinkServices.put("naverUnlinkService", naverUnlinkService);
        unlinkServices.put("googleUnlinkService", googleUnlinkService);
    }

    @Nested
    @DisplayName("loginId 중복 체크 테스트")
    class LoginIdDuplicationCheckTests {

        @Test
        @DisplayName("사용 가능한 loginId - 성공")
        void checkLoginIdAvailable_Success() {
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(TEST_LOGIN_ID)).willReturn(false);

            assertThatCode(() -> authService.checkLoginIdAvailable(TEST_LOGIN_ID))
                    .doesNotThrowAnyException();

            verify(memberRepository).existsByLoginIdAndIsDeletedFalse(TEST_LOGIN_ID);
        }

        @Test
        @DisplayName("이미 존재하는 loginId - DUPLICATED_LOGIN_ID 예외 발생")
        void checkLoginIdAvailable_ThrowsException_WhenDuplicated() {
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(TEST_LOGIN_ID)).willReturn(true);

            assertThatThrownBy(() -> authService.checkLoginIdAvailable(TEST_LOGIN_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.DUPLICATED_LOGIN_ID);

            verify(memberRepository).existsByLoginIdAndIsDeletedFalse(TEST_LOGIN_ID);
        }
    }

    @Nested
    @DisplayName("휴대폰 번호 중복 체크 테스트")
    class PhoneNumberDuplicationCheckTests {

        @Test
        @DisplayName("사용 가능한 휴대폰 번호 - 성공")
        void checkPhoneNumberDuplicate_Success() {
            given(memberRepository.existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(false);

            assertThatCode(() -> authService.checkPhoneNumberDuplicate(TEST_PHONE))
                    .doesNotThrowAnyException();

            verify(memberRepository).existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE);
        }

        @Test
        @DisplayName("이미 존재하는 휴대폰 번호 - PHONE_NUMBER_DUPLICATED 예외 발생")
        void checkPhoneNumberDuplicate_ThrowsException_WhenDuplicated() {
            given(memberRepository.existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(true);

            assertThatThrownBy(() -> authService.checkPhoneNumberDuplicate(TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PHONE_NUMBER_DUPLICATED);

            verify(memberRepository).existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE);
        }
    }

    @Nested
    @DisplayName("휴대폰 인증 코드 전송 테스트")
    class PhoneVerificationCodeSendTests {

        @Test
        @DisplayName("인증 코드 전송 - 성공")
        void sendPhoneVerificationCode_Success() {
            String emailAddress = "test@seeyouagain.com";
            given(memberRepository.existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(false);
            given(emailService.getServerEmail()).willReturn(emailAddress);

            willDoNothing().given(redisAuthService).saveVerificationCode(eq(TEST_PHONE), anyString());
            willDoNothing().given(redisAuthService).saveVerificationTime(eq(TEST_PHONE), anyString());

            PhoneVerificationResultResponse result = authService.sendPhoneVerificationCode(TEST_PHONE);

            assertThat(result).isNotNull();
            assertThat(result.code()).matches("\\d{6}");
            assertThat(result.emailAddress()).isEqualTo(emailAddress);

            verify(redisAuthService).saveVerificationCode(eq(TEST_PHONE), anyString());
            verify(redisAuthService).saveVerificationTime(eq(TEST_PHONE), anyString());
        }

        @Test
        @DisplayName("이미 등록된 번호로 인증 시도 - 예외 발생")
        void sendPhoneVerificationCode_DuplicatedPhone_ThrowsException() {
            given(memberRepository.existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(true);

            assertThatThrownBy(() -> authService.sendPhoneVerificationCode(TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PHONE_NUMBER_DUPLICATED);

            verify(emailService, never()).getServerEmail();
            verify(redisAuthService, never()).saveVerificationCode(anyString(), anyString());
            verify(redisAuthService, never()).saveVerificationTime(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("휴대폰 인증 코드 검증 테스트")
    class PhoneVerificationCodeVerifyTests {

        @Test
        @DisplayName("인증 코드 검증 - 성공")
        void verifyPhoneCode_Success() {
            String code = "123456";
            String nowIso = LocalDateTime.now().toString();

            given(redisAuthService.getVerificationCode(TEST_PHONE)).willReturn(Optional.of(code));
            given(redisAuthService.getVerificationTime(TEST_PHONE)).willReturn(Optional.of(nowIso));
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, LocalDateTime.parse(nowIso))).willReturn(true);

            willDoNothing().given(redisAuthService).markPhoneAsVerified(TEST_PHONE);
            willDoNothing().given(redisAuthService).deleteVerificationData(TEST_PHONE);

            assertThatCode(() -> authService.verifyPhoneCode(TEST_PHONE)).doesNotThrowAnyException();

            verify(redisAuthService).markPhoneAsVerified(TEST_PHONE);
            verify(redisAuthService).deleteVerificationData(TEST_PHONE);
        }

        @Test
        @DisplayName("인증 코드가 없으면 - INVALID_VERIFICATION_CODE 예외 발생")
        void verifyPhoneCode_NoCode_ThrowsException() {
            given(redisAuthService.getVerificationCode(TEST_PHONE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyPhoneCode(TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_VERIFICATION_CODE);

            verify(emailService, never()).extractCodeByPhoneNumber(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("인증 시간이 없으면 - 예외 발생")
        void verifyPhoneCode_NoTime_ThrowsException() {
            given(redisAuthService.getVerificationCode(TEST_PHONE)).willReturn(Optional.of("123456"));
            given(redisAuthService.getVerificationTime(TEST_PHONE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyPhoneCode(TEST_PHONE))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("인증 코드가 일치하지 않으면 - 예외 발생")
        void verifyPhoneCode_InvalidCode_ThrowsException() {
            String code = "123456";
            String nowIso = LocalDateTime.now().toString();

            given(redisAuthService.getVerificationCode(TEST_PHONE)).willReturn(Optional.of(code));
            given(redisAuthService.getVerificationTime(TEST_PHONE)).willReturn(Optional.of(nowIso));
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, LocalDateTime.parse(nowIso))).willReturn(false);

            assertThatThrownBy(() -> authService.verifyPhoneCode(TEST_PHONE))
                    .isInstanceOf(CustomException.class);

            verify(redisAuthService, never()).markPhoneAsVerified(anyString());
        }
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class RegisterTests {

        @Test
        @DisplayName("회원가입 - 성공")
        void register_Success() {
            UnifiedRegisterRequest req = new UnifiedRegisterRequest(
                    TEST_LOGIN_ID, TEST_PASSWORD, TEST_NICKNAME, TEST_PHONE, null, null
            );

            given(redisAuthService.isPhoneVerified(req.phoneNumber())).willReturn(true);
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(req.loginId())).willReturn(false);
            given(passwordEncoder.encode(req.password())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class))).willReturn(null);

            willDoNothing().given(redisAuthService).deletePhoneVerification(req.phoneNumber());

            assertThatCode(() -> authService.unifiedRegister(req)).doesNotThrowAnyException();

            verify(memberRepository).save(any(Member.class));
            verify(redisAuthService).deletePhoneVerification(req.phoneNumber());
        }

        @Test
        @DisplayName("휴대폰 인증이 되지 않은 상태로 회원가입 - PHONE_NOT_VERIFIED 예외 발생")
        void register_PhoneNotVerified_ThrowsException() {
            UnifiedRegisterRequest req = new UnifiedRegisterRequest(
                    TEST_LOGIN_ID, TEST_PASSWORD, TEST_NICKNAME, TEST_PHONE, null, null
            );

            given(redisAuthService.isPhoneVerified(req.phoneNumber())).willReturn(false);

            assertThatThrownBy(() -> authService.unifiedRegister(req))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PHONE_NOT_VERIFIED);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("중복된 loginId로 회원가입 시도 - DUPLICATED_LOGIN_ID 예외 발생")
        void register_DuplicatedLoginId_ThrowsException() {
            UnifiedRegisterRequest req = new UnifiedRegisterRequest(
                    TEST_LOGIN_ID, TEST_PASSWORD, TEST_NICKNAME, TEST_PHONE, null, null
            );

            given(redisAuthService.isPhoneVerified(req.phoneNumber())).willReturn(true); // 중요
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(req.loginId())).willReturn(true);

            assertThatThrownBy(() -> authService.unifiedRegister(req))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.DUPLICATED_LOGIN_ID);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("회원가입 후 Redis 인증 정보 삭제 확인")
        void register_ClearsRedisVerificationData() {
            UnifiedRegisterRequest req = new UnifiedRegisterRequest(
                    TEST_LOGIN_ID, TEST_PASSWORD, TEST_NICKNAME, TEST_PHONE, null, null
            );

            given(redisAuthService.isPhoneVerified(req.phoneNumber())).willReturn(true);
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(req.loginId())).willReturn(false);
            given(passwordEncoder.encode(req.password())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class))).willReturn(null);

            willDoNothing().given(redisAuthService).deletePhoneVerification(req.phoneNumber());

            authService.unifiedRegister(req);

            verify(redisAuthService).deletePhoneVerification(req.phoneNumber());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class ReissueTokenTests {

        @Test
        @DisplayName("토큰 재발급 - 성공")
        void reissueToken_Success() {
            // given
            String refreshToken = "valid-refresh-token";
            String uuid = "test-uuid";
            String role = "ROLE_USER";
            String storedRefreshToken = "valid-refresh-token";
            String newAccessToken = "new-access-token";
            long accessTokenExpSec = 3600L;

            given(cookieService.resolveRefreshToken(request)).willReturn(refreshToken);

            io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
            given(tokenProvider.parseClaims(refreshToken)).willReturn(claims);
            given(claims.getSubject()).willReturn(uuid);
            given(claims.get("role", String.class)).willReturn(role);

            given(redisAuthService.getRefreshToken(uuid)).willReturn(Optional.of(storedRefreshToken));

            doNothing().when(tokenProvider).validateToken(refreshToken);
            given(tokenProvider.createAccessToken(uuid, role)).willReturn(newAccessToken);
            given(tokenProvider.getAccessTokenExpirationSec()).willReturn(accessTokenExpSec);
            doNothing().when(cookieService).setAccessTokenCookie(response, newAccessToken, accessTokenExpSec);

            // when
            authService.reissueToken(request, response);

            // then
            verify(cookieService).resolveRefreshToken(request);
            verify(tokenProvider, atLeast(2)).parseClaims(refreshToken); // 두 번 호출됨
            verify(redisAuthService).getRefreshToken(uuid);
            verify(tokenProvider).createAccessToken(uuid, role);
            verify(cookieService).setAccessTokenCookie(response, newAccessToken, accessTokenExpSec);

            verify(tokenProvider, never()).createLoginToken(anyString(), any());
            verify(cookieService, never()).setRefreshTokenCookie(any(), any(), anyLong());
            verify(redisAuthService, never()).deleteRefreshToken(anyString());
            verify(redisAuthService, never()).saveRefreshToken(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("토큰 재발급 - RefreshToken이 쿠키에 없으면 예외 발생")
        void reissueToken_ThrowsException_WhenNoRefreshToken() {
            // given
            given(cookieService.resolveRefreshToken(request)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);

            verify(cookieService).resolveRefreshToken(request);
            verify(tokenProvider, never()).parseClaims(anyString());
        }

        @Test
        @DisplayName("토큰 재발급 - Redis의 RefreshToken과 일치하지 않으면 예외 발생")
        void reissueToken_ThrowsException_WhenTokenMismatch() {
            // given
            String refreshToken = "valid-refresh-token";
            String uuid = "test-uuid";
            String differentToken = "different-token";

            given(cookieService.resolveRefreshToken(request)).willReturn(refreshToken);

            io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
            given(tokenProvider.parseClaims(refreshToken)).willReturn(claims);
            given(claims.getSubject()).willReturn(uuid);

            given(redisAuthService.getRefreshToken(uuid)).willReturn(Optional.of(differentToken));

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.REFRESH_TOKEN_MISMATCH);

            verify(cookieService).resolveRefreshToken(request);
            verify(tokenProvider, atLeast(2)).parseClaims(refreshToken); // parseClaims 두 번 호출됨
            verify(redisAuthService).getRefreshToken(uuid);
            verify(tokenProvider, never()).createLoginToken(anyString(), any());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class WithdrawalTests {

        @Test
        @DisplayName("회원 탈퇴 - 소셜 연동 없는 회원 탈퇴 성공")
        void withdrawMember_Success_WithoutSocialAccounts() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password(ENCODED_PASSWORD)
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            WithdrawalRequest withdrawalRequest = new WithdrawalRequest(TEST_PASSWORD, "서비스 이용 불편");

            given(memberRepository.findByUuidAndIsDeletedFalse(TEST_UUID))
                    .willReturn(Optional.of(member));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                    .willReturn(true);
            given(memberRepository.save(any(Member.class))).willReturn(member);
            doNothing().when(redisAuthService).deleteRefreshToken(TEST_UUID);

            // when
            authService.withdrawMember(TEST_UUID, withdrawalRequest);

            // then
            verify(memberRepository).findByUuidAndIsDeletedFalse(TEST_UUID);
            verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
            verify(memberRepository).save(member);
            verify(redisAuthService).deleteRefreshToken(TEST_UUID);
        }

        @Test
        @DisplayName("회원 탈퇴 - 존재하지 않는 회원이면 예외 발생")
        void withdrawMember_ThrowsException_WhenMemberNotFound() {
            // given
            WithdrawalRequest withdrawalRequest = new WithdrawalRequest(TEST_PASSWORD, null);

            given(memberRepository.findByUuidAndIsDeletedFalse(TEST_UUID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.withdrawMember(TEST_UUID, withdrawalRequest))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);

            verify(memberRepository).findByUuidAndIsDeletedFalse(TEST_UUID);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("회원 탈퇴 - 비밀번호가 일치하지 않으면 예외 발생")
        void withdrawMember_ThrowsException_WhenPasswordMismatch() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password(ENCODED_PASSWORD)
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            WithdrawalRequest withdrawalRequest = new WithdrawalRequest("WrongPassword!", "탈퇴 사유");

            given(memberRepository.findByUuidAndIsDeletedFalse(TEST_UUID))
                    .willReturn(Optional.of(member));
            given(passwordEncoder.matches("WrongPassword!", ENCODED_PASSWORD))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.withdrawMember(TEST_UUID, withdrawalRequest))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.WRONG_ID_PW);

            verify(memberRepository).findByUuidAndIsDeletedFalse(TEST_UUID);
            verify(passwordEncoder).matches("WrongPassword!", ENCODED_PASSWORD);
            verify(memberRepository, never()).save(any(Member.class));
        }
    }
}