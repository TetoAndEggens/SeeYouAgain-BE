package tetoandeggens.seeyouagainbe.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialLoginResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialTempInfoResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.entity.Role;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@DisplayName("OAuth2Service 단위 테스트")
class OAuth2ServiceTest extends ServiceTest {

    private static final String TEST_PHONE = "01012345678";
    private static final String TEST_PROVIDER = "kakao";
    private static final String TEST_SOCIAL_ID = "123456789";
    private static final String TEST_TEMP_UUID = "temp-uuid-123";
    private static final String TEST_PROFILE_URL = "https://example.com/profile.jpg";

    @Autowired
    private OAuth2Service oAuth2Service;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private RedisAuthService redisAuthService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private SocialAccountLinkStrategy socialAccountLinkStrategy;

    @MockitoBean
    private HttpServletRequest request;

    @MockitoBean
    private HttpServletResponse response;

    @Nested
    @DisplayName("소셜 휴대폰 인증 코드 전송 테스트")
    class SendSocialPhoneVerificationCodeTests {

        @Test
        @DisplayName("소셜 인증 코드 전송 - 성공")
        void sendSocialPhoneVerificationCode_Success() {
            // given
            String emailAddress = "test@seeyouagain.com";

            given(redisAuthService.getTempSocialProvider(TEST_TEMP_UUID)).willReturn(Optional.of(TEST_PROVIDER));
            given(redisAuthService.getTempSocialId(TEST_TEMP_UUID)).willReturn(Optional.of(TEST_SOCIAL_ID));
            given(emailService.getServerEmail()).willReturn(emailAddress);
            doNothing().when(redisAuthService).saveSocialVerificationCode(anyString(), anyString());
            doNothing().when(redisAuthService).saveSocialVerificationTime(anyString(), anyString());
            doNothing().when(redisAuthService).saveSocialProvider(anyString(), anyString());
            doNothing().when(redisAuthService).saveSocialId(anyString(), anyString());
            doNothing().when(redisAuthService).saveSocialTempUuid(anyString(), anyString());

            // when
            PhoneVerificationResultResponse result = oAuth2Service.sendSocialPhoneVerificationCode(
                    TEST_PHONE, TEST_TEMP_UUID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.code()).hasSize(6);
            assertThat(result.emailAddress()).isEqualTo(emailAddress);

            verify(redisAuthService).saveSocialVerificationCode(eq(TEST_PHONE), anyString());
            verify(redisAuthService).saveSocialVerificationTime(eq(TEST_PHONE), anyString());
            verify(redisAuthService).saveSocialProvider(TEST_PHONE, TEST_PROVIDER);
            verify(redisAuthService).saveSocialId(TEST_PHONE, TEST_SOCIAL_ID);
            verify(redisAuthService).saveSocialTempUuid(TEST_PHONE, TEST_TEMP_UUID);
        }

        @Test
        @DisplayName("tempUuid로 provider 조회 실패 - 예외 발생")
        void sendSocialPhoneVerificationCode_ThrowsException_WhenProviderNotFound() {
            // given
            given(redisAuthService.getTempSocialProvider(TEST_TEMP_UUID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> oAuth2Service.sendSocialPhoneVerificationCode(TEST_PHONE, TEST_TEMP_UUID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.REAUTH_TOKEN_NOT_FOUND);

            verify(emailService, never()).getServerEmail();
        }

        @Test
        @DisplayName("tempUuid로 socialId 조회 실패 - 예외 발생")
        void sendSocialPhoneVerificationCode_ThrowsException_WhenSocialIdNotFound() {
            // given
            given(redisAuthService.getTempSocialProvider(TEST_TEMP_UUID)).willReturn(Optional.of(TEST_PROVIDER));
            given(redisAuthService.getTempSocialId(TEST_TEMP_UUID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> oAuth2Service.sendSocialPhoneVerificationCode(TEST_PHONE, TEST_TEMP_UUID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.REAUTH_TOKEN_NOT_FOUND);

            verify(emailService, never()).getServerEmail();
        }
    }

    @Nested
    @DisplayName("소셜 휴대폰 인증 코드 검증 테스트")
    class VerifySocialPhoneCodeTests {

        @Test
        @DisplayName("신규 회원 - SIGNUP 상태 반환")
        void verifySocialPhoneCode_NewMember_ReturnsSignupStatus() {
            // given
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            given(redisAuthService.getSocialVerificationCode(TEST_PHONE)).willReturn(Optional.of(code));
            given(redisAuthService.getSocialVerificationTime(TEST_PHONE)).willReturn(Optional.of(now.toString()));
            given(redisAuthService.getSocialProvider(TEST_PHONE)).willReturn(Optional.of(TEST_PROVIDER));
            given(redisAuthService.getSocialId(TEST_PHONE)).willReturn(Optional.of(TEST_SOCIAL_ID));
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(true);
            doNothing().when(redisAuthService).markSocialPhoneAsVerified(TEST_PHONE);
            doNothing().when(redisAuthService).deleteSocialVerificationData(TEST_PHONE);
            given(memberRepository.findByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(Optional.empty());

            // when
            SocialLoginResultResponse result = oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response);

            // then
            assertThat(result.status()).isEqualTo("SIGNUP");
            assertThat(result.message()).contains("회원가입이 필요합니다");
            assertThat(result.loginResponse()).isNull();
        }

        @Test
        @DisplayName("기존 회원 + 연동 안됨 - LINK 상태 반환")
        void verifySocialPhoneCode_ExistingMemberNotLinked_ReturnsLinkStatus() {
            // given
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            Member existingMember = Member.builder()
                    .loginId("testuser")
                    .password("encoded")
                    .nickName("테스트")
                    .phoneNumber(TEST_PHONE)
                    .build();

            given(redisAuthService.getSocialVerificationCode(TEST_PHONE)).willReturn(Optional.of(code));
            given(redisAuthService.getSocialVerificationTime(TEST_PHONE)).willReturn(Optional.of(now.toString()));
            given(redisAuthService.getSocialProvider(TEST_PHONE)).willReturn(Optional.of(TEST_PROVIDER));
            given(redisAuthService.getSocialId(TEST_PHONE)).willReturn(Optional.of(TEST_SOCIAL_ID));
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(true);
            doNothing().when(redisAuthService).markSocialPhoneAsVerified(TEST_PHONE);
            doNothing().when(redisAuthService).deleteSocialVerificationData(TEST_PHONE);
            given(memberRepository.findByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(Optional.of(existingMember));

            // when
            SocialLoginResultResponse result = oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response);

            // then
            assertThat(result.status()).isEqualTo("LINK");
            assertThat(result.message()).contains("연동하시겠습니까");
            assertThat(result.loginResponse()).isNull();
        }

        @Test
        @DisplayName("인증 코드 없음 - 예외 발생")
        void verifySocialPhoneCode_NoCode_ThrowsException() {
            // given
            given(redisAuthService.getSocialVerificationCode(TEST_PHONE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        @Test
        @DisplayName("인증 코드가 일치하지 않음 - 예외 발생")
        void verifySocialPhoneCode_InvalidCode_ThrowsException() {
            // given
            String code = "123456";
            LocalDateTime now = LocalDateTime.now();

            given(redisAuthService.getSocialVerificationCode(TEST_PHONE)).willReturn(Optional.of(code));
            given(redisAuthService.getSocialVerificationTime(TEST_PHONE)).willReturn(Optional.of(now.toString()));
            given(redisAuthService.getSocialProvider(TEST_PHONE)).willReturn(Optional.of(TEST_PROVIDER));
            given(redisAuthService.getSocialId(TEST_PHONE)).willReturn(Optional.of(TEST_SOCIAL_ID));
            given(emailService.extractCodeByPhoneNumber(code, TEST_PHONE, now)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> oAuth2Service.verifySocialPhoneCode(TEST_PHONE, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_VERIFICATION_CODE);
        }
    }

    @Nested
    @DisplayName("소셜 계정 연동 테스트")
    class LinkSocialAccountTests {
        @Test
        @DisplayName("소셜 계정 연동 - 성공")
        void linkSocialAccount_Success() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encoded")
                    .nickName("테스트")
                    .phoneNumber(TEST_PHONE)
                    .build();

            // Member의 id 필드 설정 (추가)
            ReflectionTestUtils.setField(member, "id", 1L);

            given(redisAuthService.isSocialPhoneVerified(TEST_PHONE)).willReturn(true);
            given(redisAuthService.getSocialId(TEST_PHONE)).willReturn(Optional.of(TEST_SOCIAL_ID));
            given(redisAuthService.getSocialProvider(TEST_PHONE)).willReturn(Optional.of(TEST_PROVIDER));
            given(memberRepository.findByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(Optional.of(member));
            given(redisAuthService.getSocialTempUuid(TEST_PHONE)).willReturn(Optional.of(TEST_TEMP_UUID));
            given(redisAuthService.getTempSocialRefreshToken(TEST_TEMP_UUID)).willReturn(Optional.of("refresh-token"));

            doNothing().when(socialAccountLinkStrategy).linkSocialId(any(), anyString(), anyString(), anyString());
            given(memberRepository.save(member)).willReturn(member);
            doNothing().when(redisAuthService).clearSocialPhoneData(TEST_PHONE);
            doNothing().when(redisAuthService).deleteTempSocialInfo(TEST_TEMP_UUID);

            UserTokenResponse tokenResponse = new UserTokenResponse("access-token", "refresh-token");
            given(tokenProvider.createLoginToken(anyString(), any(Role.class))).willReturn(tokenResponse);
            given(tokenProvider.getAccessTokenExpirationSec()).willReturn(3600L);
            given(tokenProvider.getRefreshTokenExpirationSec()).willReturn(86400L);
            given(tokenProvider.getRefreshTokenExpirationMs()).willReturn(86400000L);

            doNothing().when(cookieService).setAccessTokenCookie(any(), anyString(), anyLong());
            doNothing().when(cookieService).setRefreshTokenCookie(any(), anyString(), anyLong());
            doNothing().when(redisAuthService).saveRefreshToken(anyString(), anyString(), anyLong());
            doNothing().when(redisAuthService).saveMemberId(anyString(), anyLong(), anyLong());

            // when
            SocialLoginResultResponse result = oAuth2Service.linkSocialAccount(TEST_PHONE, response);

            // then
            assertThat(result.status()).isEqualTo("LOGIN");
            assertThat(result.loginResponse()).isNotNull();

            verify(socialAccountLinkStrategy).linkSocialId(member, TEST_PROVIDER, TEST_SOCIAL_ID, "refresh-token");
            verify(memberRepository).save(member);
            verify(redisAuthService).clearSocialPhoneData(TEST_PHONE);
            verify(redisAuthService).deleteTempSocialInfo(TEST_TEMP_UUID);
            verify(redisAuthService).saveRefreshToken(anyString(), anyString(), anyLong());
            verify(redisAuthService).saveMemberId(anyString(), eq(1L), anyLong());  // eq(1L)로 변경
        }

        @Test
        @DisplayName("휴대폰 인증이 안된 상태 - 예외 발생")
        void linkSocialAccount_ThrowsException_WhenPhoneNotVerified() {
            // given
            given(redisAuthService.isSocialPhoneVerified(TEST_PHONE)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> oAuth2Service.linkSocialAccount(TEST_PHONE, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PHONE_NOT_VERIFIED);

            verify(memberRepository, never()).findByPhoneNumberAndIsDeletedFalse(anyString());
            verify(redisAuthService, never()).saveRefreshToken(anyString(), anyString(), anyLong());
            verify(redisAuthService, never()).saveMemberId(anyString(), anyLong(), anyLong());  // 추가
        }

        @Test
        @DisplayName("회원을 찾을 수 없음 - 예외 발생")
        void linkSocialAccount_ThrowsException_WhenMemberNotFound() {
            // given
            given(redisAuthService.isSocialPhoneVerified(TEST_PHONE)).willReturn(true);
            given(redisAuthService.getSocialId(TEST_PHONE)).willReturn(Optional.of(TEST_SOCIAL_ID));
            given(redisAuthService.getSocialProvider(TEST_PHONE)).willReturn(Optional.of(TEST_PROVIDER));
            given(memberRepository.findByPhoneNumberAndIsDeletedFalse(TEST_PHONE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> oAuth2Service.linkSocialAccount(TEST_PHONE, response))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);

            verify(redisAuthService, never()).saveRefreshToken(anyString(), anyString(), anyLong());
            verify(redisAuthService, never()).saveMemberId(anyString(), anyLong(), anyLong());  // 추가
        }
    }

    @Nested
    @DisplayName("소셜 임시 정보 조회 테스트")
    class GetSocialTempInfoTests {

        @Test
        @DisplayName("소셜 임시 정보 조회 - 성공")
        void getSocialTempInfo_Success() {
            // given
            String token = "social-temp-token";

            given(cookieService.resolveSocialTempToken(request)).willReturn(token);

            Claims claims = mock(Claims.class);
            given(tokenProvider.parseClaims(token)).willReturn(claims);
            given(claims.get("profileImageUrl", String.class)).willReturn(TEST_PROFILE_URL);
            given(claims.get("tempUuid", String.class)).willReturn(TEST_TEMP_UUID);

            doNothing().when(redisAuthService).extendTempSocialInfoTTL(TEST_TEMP_UUID);

            // when
            SocialTempInfoResponse result = oAuth2Service.getSocialTempInfo(request);

            // then
            assertThat(result.profileImageUrl()).isEqualTo(TEST_PROFILE_URL);
            assertThat(result.tempUuid()).isEqualTo(TEST_TEMP_UUID);

            verify(redisAuthService).extendTempSocialInfoTTL(TEST_TEMP_UUID);
        }

        @Test
        @DisplayName("소셜 임시 토큰이 없음 - 예외 발생")
        void getSocialTempInfo_ThrowsException_WhenTokenNotFound() {
            // given
            given(cookieService.resolveSocialTempToken(request)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> oAuth2Service.getSocialTempInfo(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_TOKEN);

            verify(tokenProvider, never()).parseClaims(anyString());
        }

        @Test
        @DisplayName("tempUuid가 없음 - 예외 발생")
        void getSocialTempInfo_ThrowsException_WhenTempUuidNotFound() {
            // given
            String token = "social-temp-token";

            given(cookieService.resolveSocialTempToken(request)).willReturn(token);

            Claims claims = mock(Claims.class);
            given(tokenProvider.parseClaims(token)).willReturn(claims);
            given(claims.get("profileImageUrl", String.class)).willReturn(TEST_PROFILE_URL);
            given(claims.get("tempUuid", String.class)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> oAuth2Service.getSocialTempInfo(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_TOKEN);

            verify(redisAuthService, never()).extendTempSocialInfoTTL(anyString());
        }
    }
}