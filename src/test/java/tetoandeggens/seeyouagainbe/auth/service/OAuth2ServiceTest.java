package tetoandeggens.seeyouagainbe.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static tetoandeggens.seeyouagainbe.global.constants.EmailVerificationConstant.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.http.HttpServletResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialLoginResultResponse;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@DisplayName("OAuth2Service 단위 테스트")
class OAuth2ServiceTest extends ServiceTest {
    private static final String TEST_PHONE = "01012345678";
    private static final String TEST_PROVIDER = "kakao";
    private static final String TEST_SOCIAL_ID = "123456789";
    private static final String TEST_PROFILE_URL = "https://example.com/profile.jpg";

    @Autowired
    private OAuth2Service oAuth2Service;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("소셜 휴대폰 인증 코드 전송 테스트")
    class SendSocialPhoneVerificationCodeTests {

        @Test
        @DisplayName("소셜 인증 코드 전송 - 성공")
        void sendSocialPhoneVerificationCode_Success() {
            String emailAddress = "test@seeyouagain.com";
            given(memberRepository.existsByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(false);
            given(emailService.getServerEmail()).willReturn(emailAddress);
            willDoNothing().given(valueOperations).set(anyString(), anyString(), any(Duration.class));

            PhoneVerificationResultResponse result = oAuth2Service.sendSocialPhoneVerificationCode(
                    TEST_PHONE, TEST_PROVIDER, TEST_SOCIAL_ID, TEST_PROFILE_URL);

            assertThat(result).isNotNull();
            assertThat(result.code()).hasSize(6);
            assertThat(result.emailAddress()).isEqualTo(emailAddress);

            verify(valueOperations).set(eq(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE), anyString(), any(Duration.class));
            verify(valueOperations).set(eq(PREFIX_SOCIAL_VERIFICATION_TIME + TEST_PHONE), anyString(), any(Duration.class));
            verify(valueOperations).set(eq(PREFIX_SOCIAL_PROVIDER + TEST_PHONE), eq(TEST_PROVIDER), any(Duration.class));
            verify(valueOperations).set(eq(PREFIX_SOCIAL_ID + TEST_PHONE), eq(TEST_SOCIAL_ID), any(Duration.class));
            verify(valueOperations).set(eq(PREFIX_SOCIAL_PROFILE + TEST_PHONE), eq(TEST_PROFILE_URL), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("소셜 휴대폰 인증 코드 검증 테스트")
    class VerifySocialPhoneCodeTests {

        @Test
        @DisplayName("신규 회원 - SIGNUP 상태 반환")
        void verifySocialPhoneCode_NewMember_ReturnsSignupStatus() {
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            given(valueOperations.get(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE)).willReturn(code);
            given(valueOperations.get(PREFIX_SOCIAL_VERIFICATION_TIME + TEST_PHONE)).willReturn(now.toString());
            given(valueOperations.get(PREFIX_SOCIAL_PROVIDER + TEST_PHONE)).willReturn(TEST_PROVIDER);
            given(valueOperations.get(PREFIX_SOCIAL_ID + TEST_PHONE)).willReturn(TEST_SOCIAL_ID);
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(true);
            willDoNothing().given(valueOperations).set(anyString(), anyString(), any(Duration.class));
            given(redisTemplate.delete(anyString())).willReturn(true);
            given(memberRepository.findByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(Optional.empty());

            SocialLoginResultResponse result = oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response);

            assertThat(result.status()).isEqualTo("SIGNUP");
            assertThat(result.message()).contains("회원가입이 필요합니다");
            assertThat(result.loginResponse()).isNull();
        }

        @Test
        @DisplayName("기존 회원 + 이미 연동됨 - LOGIN 상태 반환")
        void verifySocialPhoneCode_ExistingMemberLinked_ReturnsLoginStatus() {
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            Member existingMember = Member.builder()
                    .loginId("testuser")
                    .password("encoded")
                    .nickName("테스트")
                    .phoneNumber(TEST_PHONE)
                    .socialIdKakao(TEST_SOCIAL_ID)
                    .build();

            given(valueOperations.get(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE)).willReturn(code);
            given(valueOperations.get(PREFIX_SOCIAL_VERIFICATION_TIME + TEST_PHONE)).willReturn(now.toString());
            given(valueOperations.get(PREFIX_SOCIAL_PROVIDER + TEST_PHONE)).willReturn(TEST_PROVIDER);
            given(valueOperations.get(PREFIX_SOCIAL_ID + TEST_PHONE)).willReturn(TEST_SOCIAL_ID);
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(true);
            willDoNothing().given(valueOperations).set(anyString(), anyString(), any(Duration.class));
            given(redisTemplate.delete(anyString())).willReturn(true);
            given(memberRepository.findByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(Optional.of(existingMember));

            SocialLoginResultResponse result = oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response);

            assertThat(result.status()).isEqualTo("LOGIN");
            assertThat(result.loginResponse()).isNotNull();
        }

        @Test
        @DisplayName("기존 회원 + 연동 안됨 - LINK 상태 반환")
        void verifySocialPhoneCode_ExistingMemberNotLinked_ReturnsLinkStatus() {
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            Member existingMember = Member.builder()
                    .loginId("testuser")
                    .password("encoded")
                    .nickName("테스트")
                    .phoneNumber(TEST_PHONE)
                    .socialIdKakao(null)  // 연동 안됨
                    .build();

            given(valueOperations.get(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE)).willReturn(code);
            given(valueOperations.get(PREFIX_SOCIAL_VERIFICATION_TIME + TEST_PHONE)).willReturn(now.toString());
            given(valueOperations.get(PREFIX_SOCIAL_PROVIDER + TEST_PHONE)).willReturn(TEST_PROVIDER);
            given(valueOperations.get(PREFIX_SOCIAL_ID + TEST_PHONE)).willReturn(TEST_SOCIAL_ID);
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(true);
            willDoNothing().given(valueOperations).set(anyString(), anyString(), any(Duration.class));
            given(redisTemplate.delete(anyString())).willReturn(true);
            given(memberRepository.findByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(Optional.of(existingMember));

            SocialLoginResultResponse result = oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response);

            assertThat(result.status()).isEqualTo("LINK");
            assertThat(result.message()).contains("연동하시겠습니까");
            assertThat(result.loginResponse()).isNull();
        }

        @Test
        @DisplayName("인증 코드 없음 - 예외 발생")
        void verifySocialPhoneCode_NoCode_ThrowsException() {
            given(valueOperations.get(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE)).willReturn(null);

            assertThatThrownBy(() -> oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_VERIFICATION_CODE);
        }
    }
}