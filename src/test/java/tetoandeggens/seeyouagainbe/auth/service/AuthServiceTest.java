package tetoandeggens.seeyouagainbe.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDateTime;

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
import tetoandeggens.seeyouagainbe.auth.dto.request.RegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@DisplayName("AuthService 단위 테스트")
class AuthServiceTest extends ServiceTest {

    private static final String VERIFIED = "verified";
    private static final String PREFIX_VERIFICATION_CODE = "phone:code:";
    private static final String PREFIX_VERIFICATION_TIME = "phone:time:";
    private static final String TEST_LOGIN_ID = "testuser123";
    private static final String TEST_PHONE = "01012345678";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_NICKNAME = "테스트";

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
    private HttpServletRequest request;

    @MockitoBean
    private HttpServletResponse response;

    @BeforeEach
    void setUp() { lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);}

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
            willDoNothing().given(valueOperations).set(anyString(), anyString(), any(Duration.class));

            PhoneVerificationResultResponse response = authService.sendPhoneVerificationCode(TEST_PHONE);

            assertThat(response).isNotNull();
            assertThat(response.code()).hasSize(6);
            assertThat(response.code()).matches("\\d{6}");
            assertThat(response.emailAddress()).isEqualTo(emailAddress);

            verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("이미 등록된 번호로 인증 시도 - 예외 발생")
        void sendPhoneVerificationCode_DuplicatedPhone_ThrowsException() {
            given(memberRepository.existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(true);

            assertThatThrownBy(() -> authService.sendPhoneVerificationCode(TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PHONE_NUMBER_DUPLICATED);

            verify(emailService, never()).getServerEmail();
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("휴대폰 인증 코드 검증 테스트")
    class PhoneVerificationCodeVerifyTests {

        @Test
        @DisplayName("인증 코드 검증 - 성공")
        void verifyPhoneCode_Success() {
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            given(valueOperations.get(PREFIX_VERIFICATION_CODE + TEST_PHONE)).willReturn(code);
            given(valueOperations.get(PREFIX_VERIFICATION_TIME + TEST_PHONE)).willReturn(now.toString());
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(true);
            willDoNothing().given(valueOperations).set(eq(TEST_PHONE), eq(VERIFIED), any(Duration.class));
            given(redisTemplate.delete(PREFIX_VERIFICATION_CODE + TEST_PHONE)).willReturn(true);
            given(redisTemplate.delete(PREFIX_VERIFICATION_TIME + TEST_PHONE)).willReturn(true);

            assertThatCode(() -> authService.verifyPhoneCode(TEST_PHONE))
                    .doesNotThrowAnyException();

            verify(emailService).extractCodeByPhoneNumber(code, TEST_PHONE, now);
            verify(valueOperations).set(eq(TEST_PHONE), eq(VERIFIED), any(Duration.class));
            verify(redisTemplate).delete(PREFIX_VERIFICATION_CODE + TEST_PHONE);
            verify(redisTemplate).delete(PREFIX_VERIFICATION_TIME + TEST_PHONE);
        }

        @Test
        @DisplayName("인증 코드가 없으면 - INVALID_VERIFICATION_CODE 예외 발생")
        void verifyPhoneCode_NoCode_ThrowsException() {
            given(valueOperations.get(PREFIX_VERIFICATION_CODE + TEST_PHONE)).willReturn(null);

            assertThatThrownBy(() -> authService.verifyPhoneCode(TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_VERIFICATION_CODE);

            verify(emailService, never()).extractCodeByPhoneNumber(anyString(), anyString(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("인증 시간이 없으면 - 예외 발생")
        void verifyPhoneCode_NoTime_ThrowsException() {
            String code = "123456";
            given(valueOperations.get(PREFIX_VERIFICATION_CODE + TEST_PHONE)).willReturn(code);
            given(valueOperations.get(PREFIX_VERIFICATION_TIME + TEST_PHONE)).willReturn(null);

            assertThatThrownBy(() -> authService.verifyPhoneCode(TEST_PHONE))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("인증 코드가 일치하지 않으면 - 예외 발생")
        void verifyPhoneCode_InvalidCode_ThrowsException() {
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            given(valueOperations.get(PREFIX_VERIFICATION_CODE + TEST_PHONE)).willReturn(code);
            given(valueOperations.get(PREFIX_VERIFICATION_TIME + TEST_PHONE)).willReturn(now.toString());
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(false);

            assertThatThrownBy(() -> authService.verifyPhoneCode(TEST_PHONE))
                    .isInstanceOf(CustomException.class);

            verify(valueOperations, never()).set(eq(TEST_PHONE), eq(VERIFIED), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class RegisterTests {

        @Test
        @DisplayName("회원가입 - 성공")
        void register_Success() {
            RegisterRequest registerRequest = new RegisterRequest(
                    TEST_LOGIN_ID,
                    TEST_PASSWORD,
                    TEST_NICKNAME,
                    TEST_PHONE
            );

            given(valueOperations.get(registerRequest.phoneNumber())).willReturn(VERIFIED);
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(registerRequest.loginId())).willReturn(false);
            given(passwordEncoder.encode(registerRequest.password())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class))).willReturn(null);
            given(redisTemplate.delete(registerRequest.phoneNumber())).willReturn(true);

            assertThatCode(() -> authService.register(registerRequest))
                    .doesNotThrowAnyException();

            verify(memberRepository).save(any(Member.class));
            verify(redisTemplate).delete(registerRequest.phoneNumber());
        }

        @Test
        @DisplayName("휴대폰 인증이 되지 않은 상태로 회원가입 - PHONE_NOT_VERIFIED 예외 발생")
        void register_PhoneNotVerified_ThrowsException() {
            RegisterRequest registerRequest = new RegisterRequest(
                    TEST_LOGIN_ID,
                    TEST_PASSWORD,
                    TEST_NICKNAME,
                    TEST_PHONE
            );

            given(valueOperations.get(registerRequest.phoneNumber())).willReturn(null);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PHONE_NOT_VERIFIED);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("중복된 loginId로 회원가입 시도 - DUPLICATED_LOGIN_ID 예외 발생")
        void register_DuplicatedLoginId_ThrowsException() {
            RegisterRequest registerRequest = new RegisterRequest(
                    TEST_LOGIN_ID,
                    TEST_PASSWORD,
                    TEST_NICKNAME,
                    TEST_PHONE
            );

            given(valueOperations.get(registerRequest.phoneNumber())).willReturn(VERIFIED);
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(registerRequest.loginId())).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.DUPLICATED_LOGIN_ID);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("회원가입 후 Redis 인증 정보 삭제 확인")
        void register_ClearsRedisVerificationData() {
            RegisterRequest registerRequest = new RegisterRequest(
                    TEST_LOGIN_ID,
                    TEST_PASSWORD,
                    TEST_NICKNAME,
                    TEST_PHONE
            );

            given(valueOperations.get(registerRequest.phoneNumber())).willReturn(VERIFIED);
            given(memberRepository.existsByLoginIdAndIsDeletedFalse(registerRequest.loginId())).willReturn(false);
            given(passwordEncoder.encode(registerRequest.password())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class))).willReturn(null);
            given(redisTemplate.delete(registerRequest.phoneNumber())).willReturn(true);

            authService.register(registerRequest);

            verify(redisTemplate).delete(registerRequest.phoneNumber());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class TokenReissueTests {

        @Test
        @DisplayName("토큰 재발급 - 성공")
        void reissueToken_Success() {
            String refreshToken = "valid_refresh_token";
            String newAccessToken = "new_access_token";

            given(tokenProvider.resolveRefreshToken(request)).willReturn(refreshToken);
            given(tokenProvider.reissueAccessToken(refreshToken)).willReturn(newAccessToken);

            ReissueTokenResponse result = authService.reissueToken(request, response);

            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo(newAccessToken);

            verify(tokenProvider).resolveRefreshToken(request);
            verify(tokenProvider).reissueAccessToken(refreshToken);
        }

        @Test
        @DisplayName("Refresh Token이 없으면 - REFRESH_TOKEN_NOT_FOUND 예외 발생")
        void reissueToken_NoRefreshToken_ThrowsException() {
            given(tokenProvider.resolveRefreshToken(request)).willReturn(null);

            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);

            verify(tokenProvider, never()).reissueAccessToken(anyString());
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token - 예외 발생")
        void reissueToken_InvalidRefreshToken_ThrowsException() {
            String invalidRefreshToken = "invalid_refresh_token";
            given(tokenProvider.resolveRefreshToken(request)).willReturn(invalidRefreshToken);
            given(tokenProvider.reissueAccessToken(invalidRefreshToken))
                    .willThrow(new CustomException(AuthErrorCode.INVALID_TOKEN));

            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_TOKEN);
        }
    }
}