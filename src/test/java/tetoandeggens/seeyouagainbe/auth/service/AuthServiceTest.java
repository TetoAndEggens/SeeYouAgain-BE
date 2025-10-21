package tetoandeggens.seeyouagainbe.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tetoandeggens.seeyouagainbe.auth.dto.request.RegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@DisplayName("Auth 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EmailService emailService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private static final String VERIFIED = "VERIFIED";
    private static final String PREFIX_VERIFICATION_CODE = "VC:";
    private static final String PREFIX_VERIFICATION_TIME = "VT:";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ============ loginId 중복 체크 테스트 ============

    @Test
    @DisplayName("loginId 중복 체크 - 사용 가능한 아이디")
    void checkLoginIdAvailable_Success() {
        // given
        String loginId = "testuser123";
        when(memberRepository.existsByLoginIdAndIsDeletedFalse(loginId)).thenReturn(false);

        // when & then
        assertDoesNotThrow(() -> authService.checkLoginIdAvailable(loginId));

        verify(memberRepository).existsByLoginIdAndIsDeletedFalse(loginId);
    }

    @Test
    @DisplayName("loginId 중복 체크 - 이미 존재하는 아이디면 예외 발생")
    void checkLoginIdAvailable_ThrowsException_WhenDuplicated() {
        // given
        String loginId = "testuser123";
        when(memberRepository.existsByLoginIdAndIsDeletedFalse(loginId)).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.checkLoginIdAvailable(loginId));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.DUPLICATED_LOGIN_ID);
        verify(memberRepository).existsByLoginIdAndIsDeletedFalse(loginId);
    }

    // ============ 휴대폰 번호 중복 체크 테스트 ============

    @Test
    @DisplayName("휴대폰 번호 중복 체크 - 사용 가능한 번호")
    void checkPhoneNumberDuplicate_Success() {
        // given
        String phone = "01012345678";
        when(memberRepository.existsByPhoneNumberAndIsDeletedFalse(phone)).thenReturn(false);

        // when & then
        assertDoesNotThrow(() -> authService.checkPhoneNumberDuplicate(phone));

        verify(memberRepository).existsByPhoneNumberAndIsDeletedFalse(phone);
    }

    @Test
    @DisplayName("휴대폰 번호 중복 체크 - 이미 존재하는 번호면 예외 발생")
    void checkPhoneNumberDuplicate_ThrowsException_WhenDuplicated() {
        // given
        String phone = "01012345678";
        when(memberRepository.existsByPhoneNumberAndIsDeletedFalse(phone)).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.checkPhoneNumberDuplicate(phone));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PHONE_NUMBER_DUPLICATED);
        verify(memberRepository).existsByPhoneNumberAndIsDeletedFalse(phone);
    }

    // ============ 휴대폰 인증 코드 전송 테스트 ============

    @Test
    @DisplayName("휴대폰 인증 코드 전송 - 성공")
    void sendPhoneVerificationCode_Success() {
        // given
        String phone = "01012345678";
        String emailAddress = "test@example.com";

        when(memberRepository.existsByPhoneNumberAndIsDeletedFalse(phone)).thenReturn(false);
        when(emailService.getServerEmail()).thenReturn(emailAddress);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // when
        PhoneVerificationResultResponse response = authService.sendPhoneVerificationCode(phone);

        // then
        assertThat(response).isNotNull();
        assertThat(response.code()).hasSize(6);
        assertThat(response.emailAddress()).isEqualTo(emailAddress);
        verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));
        verify(memberRepository).existsByPhoneNumberAndIsDeletedFalse(phone);
    }

    @Test
    @DisplayName("휴대폰 인증 코드 전송 - 이미 존재하는 번호면 예외 발생")
    void sendPhoneVerificationCode_ThrowsException_WhenDuplicated() {
        // given
        String phone = "01012345678";
        when(memberRepository.existsByPhoneNumberAndIsDeletedFalse(phone)).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.sendPhoneVerificationCode(phone));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PHONE_NUMBER_DUPLICATED);
        verify(memberRepository).existsByPhoneNumberAndIsDeletedFalse(phone);
    }

    // ============ 휴대폰 인증 코드 검증 테스트 ============

    @Test
    @DisplayName("휴대폰 인증 코드 검증 - 성공")
    void verifyPhoneCode_Success() {
        // given
        String phone = "01012345678";
        String code = "123456";
        LocalDateTime now = LocalDateTime.now();

        when(valueOperations.get(PREFIX_VERIFICATION_CODE + phone)).thenReturn(code);
        when(valueOperations.get(PREFIX_VERIFICATION_TIME + phone)).thenReturn(now.toString());
        when(emailService.extractCodeByPhoneNumber(code, phone, now)).thenReturn(true);
        doNothing().when(valueOperations).set(eq(phone), eq(VERIFIED), any(Duration.class));
        when(redisTemplate.delete(PREFIX_VERIFICATION_CODE + phone)).thenReturn(true);
        when(redisTemplate.delete(PREFIX_VERIFICATION_TIME + phone)).thenReturn(true);

        // when & then
        assertDoesNotThrow(() -> authService.verifyPhoneCode(phone));

        verify(emailService).extractCodeByPhoneNumber(code, phone, now);
        verify(valueOperations).set(eq(phone), eq(VERIFIED), any(Duration.class));
        verify(redisTemplate).delete(PREFIX_VERIFICATION_CODE + phone);
        verify(redisTemplate).delete(PREFIX_VERIFICATION_TIME + phone);
    }

    @Test
    @DisplayName("휴대폰 인증 코드 검증 - 코드가 없으면 예외 발생")
    void verifyPhoneCode_ThrowsException_WhenCodeNotFound() {
        // given
        String phone = "01012345678";
        when(valueOperations.get(PREFIX_VERIFICATION_CODE + phone)).thenReturn(null);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.verifyPhoneCode(phone));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    @DisplayName("휴대폰 인증 코드 검증 - 이메일 검증 실패 시 예외 발생")
    void verifyPhoneCode_ThrowsException_WhenEmailVerificationFailed() {
        // given
        String phone = "01012345678";
        String code = "123456";
        LocalDateTime now = LocalDateTime.now();

        when(valueOperations.get(PREFIX_VERIFICATION_CODE + phone)).thenReturn(code);
        when(valueOperations.get(PREFIX_VERIFICATION_TIME + phone)).thenReturn(now.toString());
        when(emailService.extractCodeByPhoneNumber(code, phone, now)).thenReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.verifyPhoneCode(phone));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_VERIFICATION_CODE);
    }

    // ============ 회원가입 테스트 ============

    @Test
    @DisplayName("회원가입 - 성공")
    void register_Success() {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser123",
                "Password123!",
                "테스트",
                "01012345678"
        );

        when(valueOperations.get(request.phoneNumber())).thenReturn(VERIFIED);
        when(memberRepository.existsByLoginIdAndIsDeletedFalse(request.loginId())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenReturn(null);
        when(redisTemplate.delete(request.phoneNumber())).thenReturn(true);

        // when & then
        assertDoesNotThrow(() -> authService.register(request));

        verify(memberRepository).save(any(Member.class));
        verify(redisTemplate).delete(request.phoneNumber());
        verify(memberRepository).existsByLoginIdAndIsDeletedFalse(request.loginId());
    }

    @Test
    @DisplayName("회원가입 - 휴대폰 인증이 되지 않은 상태면 예외 발생")
    void register_ThrowsException_WhenPhoneNotVerified() {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser123",
                "Password123!",
                "테스트",
                "01012345678"
        );

        when(valueOperations.get(request.phoneNumber())).thenReturn(null);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.register(request));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PHONE_NOT_VERIFIED);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 - 휴대폰 인증 값이 올바르지 않으면 예외 발생")
    void register_ThrowsException_WhenPhoneVerificationWrong() {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser123",
                "Password123!",
                "테스트",
                "01012345678"
        );

        when(valueOperations.get(request.phoneNumber())).thenReturn("WRONG_VALUE");

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.register(request));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PHONE_NOT_VERIFIED);
        verify(memberRepository, never()).save(any(Member.class));
    }

    // ============ 토큰 재발급 테스트 ============

    @Test
    @DisplayName("토큰 재발급 - 성공")
    void reissueToken_Success() {
        // given
        String refreshToken = "valid_refresh_token";
        String newAccessToken = "new_access_token";

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        when(tokenProvider.resolveRefreshToken(request)).thenReturn(refreshToken);
        when(tokenProvider.reissueAccessToken(refreshToken)).thenReturn(newAccessToken);

        // when
        ReissueTokenResponse result = authService.reissueToken(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        verify(tokenProvider).resolveRefreshToken(request);
        verify(tokenProvider).reissueAccessToken(refreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 - Refresh Token이 없으면 예외 발생")
    void reissueToken_ThrowsException_WhenRefreshTokenNotFound() {
        // given
        when(tokenProvider.resolveRefreshToken(request)).thenReturn(null);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(request, response));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }
}