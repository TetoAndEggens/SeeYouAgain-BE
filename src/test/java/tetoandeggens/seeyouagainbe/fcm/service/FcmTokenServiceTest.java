package tetoandeggens.seeyouagainbe.fcm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.fcm.dto.request.FcmTokenRequest;
import tetoandeggens.seeyouagainbe.fcm.dto.response.FcmTokenResponse;
import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;
import tetoandeggens.seeyouagainbe.fcm.repository.FcmTokenRepository;
import tetoandeggens.seeyouagainbe.fcm.util.DeviceTypeValidator;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.FcmErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("FcmTokenService 단위 테스트")
class FcmTokenServiceTest extends ServiceTest {

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_TOKEN = "test-fcm-token-12345";
    private static final String TEST_DEVICE_ID = "test-device-id-12345";
    private static final String TEST_USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; Android 10)";
    private static final String TEST_USER_AGENT_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0)";
    private static final String TEST_USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0)";

    @Autowired
    private FcmTokenService fcmTokenService;

    @MockitoBean
    private FcmTokenRepository fcmTokenRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private DeviceTypeValidator deviceTypeValidator;

    @MockitoBean
    private FirebaseMessagingService firebaseMessagingService;

    @Nested
    @DisplayName("FCM 토큰 등록/업데이트 테스트")
    class SaveOrUpdateTokenTests {

        @Test
        @DisplayName("새로운 FCM 토큰 등록 - 성공 (Android)")
        void saveToken_Success_Android() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            FcmTokenRequest request = new FcmTokenRequest(TEST_TOKEN, TEST_DEVICE_ID);

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(firebaseMessagingService.isValidToken(TEST_TOKEN)).willReturn(true);
            given(deviceTypeValidator.validateAndExtractDeviceType(TEST_USER_AGENT_ANDROID))
                    .willReturn(DeviceType.ANDROID);
            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.empty());
            given(fcmTokenRepository.save(any(FcmToken.class))).willAnswer(invocation ->
                    invocation.getArgument(0));

            // when
            FcmTokenResponse response = fcmTokenService.saveOrUpdateToken(
                    TEST_MEMBER_ID, request, TEST_USER_AGENT_ANDROID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.deviceId()).isEqualTo(TEST_DEVICE_ID);
            assertThat(response.deviceType()).isEqualTo(DeviceType.ANDROID);

            verify(fcmTokenRepository).save(any(FcmToken.class));
        }

        @Test
        @DisplayName("새로운 FCM 토큰 등록 - 성공 (iOS)")
        void saveToken_Success_iOS() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            FcmTokenRequest request = new FcmTokenRequest(TEST_TOKEN, TEST_DEVICE_ID);

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(firebaseMessagingService.isValidToken(TEST_TOKEN)).willReturn(true);
            given(deviceTypeValidator.validateAndExtractDeviceType(TEST_USER_AGENT_IOS))
                    .willReturn(DeviceType.IOS);
            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.empty());
            given(fcmTokenRepository.save(any(FcmToken.class))).willAnswer(invocation ->
                    invocation.getArgument(0));

            // when
            FcmTokenResponse response = fcmTokenService.saveOrUpdateToken(
                    TEST_MEMBER_ID, request, TEST_USER_AGENT_IOS);

            // then
            assertThat(response.deviceType()).isEqualTo(DeviceType.IOS);
            verify(fcmTokenRepository).save(any(FcmToken.class));
        }

        @Test
        @DisplayName("새로운 FCM 토큰 등록 - 성공 (WEB)")
        void saveToken_Success_Web() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            FcmTokenRequest request = new FcmTokenRequest(TEST_TOKEN, TEST_DEVICE_ID);

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(firebaseMessagingService.isValidToken(TEST_TOKEN)).willReturn(true);
            given(deviceTypeValidator.validateAndExtractDeviceType(TEST_USER_AGENT_WEB))
                    .willReturn(DeviceType.WEB);
            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.empty());
            given(fcmTokenRepository.save(any(FcmToken.class))).willAnswer(invocation ->
                    invocation.getArgument(0));

            // when
            FcmTokenResponse response = fcmTokenService.saveOrUpdateToken(
                    TEST_MEMBER_ID, request, TEST_USER_AGENT_WEB);

            // then
            assertThat(response.deviceType()).isEqualTo(DeviceType.WEB);
            verify(fcmTokenRepository).save(any(FcmToken.class));
        }

        @Test
        @DisplayName("기존 FCM 토큰 업데이트 - 성공 (Dirty Checking)")
        void updateToken_Success() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            FcmToken existingToken = FcmToken.builder()
                    .member(member)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
            FcmTokenRequest request = new FcmTokenRequest("new-token-12345", TEST_DEVICE_ID);

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(firebaseMessagingService.isValidToken("new-token-12345")).willReturn(true);
            given(deviceTypeValidator.validateAndExtractDeviceType(TEST_USER_AGENT_ANDROID))
                    .willReturn(DeviceType.ANDROID);
            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.of(existingToken));

            // when
            FcmTokenResponse response = fcmTokenService.saveOrUpdateToken(
                    TEST_MEMBER_ID, request, TEST_USER_AGENT_ANDROID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.deviceId()).isEqualTo(TEST_DEVICE_ID);

            // ✅ 수정: 기존 토큰 업데이트는 Dirty Checking으로 처리되므로 save() 호출 안 함
            verify(fcmTokenRepository, never()).save(any(FcmToken.class));

            // updateToken() 메서드가 호출되었는지 확인 (엔티티 상태 변경)
            assertThat(existingToken.getToken()).isEqualTo("new-token-12345");
        }
    }

    @Nested
    @DisplayName("FCM 토큰 갱신 테스트")
    class RefreshTokenTests {

        @Test
        @DisplayName("FCM 토큰 갱신 - 30일 이상 지났으면 갱신 성공 (Dirty Checking)")
        void refreshToken_Success_WhenExpired() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            FcmToken token = FcmToken.builder()
                    .member(member)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .lastUsedAt(LocalDateTime.now().minusDays(31))
                    .build();

            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.of(token));

            // when
            assertThatCode(() -> fcmTokenService.refreshTokenIfNeeded(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .doesNotThrowAnyException();

            // then
            // ✅ 수정: Dirty Checking으로 처리되므로 save() 호출 안 함
            verify(fcmTokenRepository, never()).save(any());

            // needsRefresh()가 true이므로 updateLastUsedAt() 호출됨 (엔티티 상태 변경)
            // 실제로 lastUsedAt이 갱신되었는지 확인
            assertThat(token.getLastUsedAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        }

        @Test
        @DisplayName("FCM 토큰 갱신 - 30일 이내면 갱신하지 않음")
        void refreshToken_DoesNotRefresh_WhenNotExpired() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            LocalDateTime recentTime = LocalDateTime.now().minusDays(15);
            FcmToken token = FcmToken.builder()
                    .member(member)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .lastUsedAt(recentTime)
                    .build();

            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.of(token));

            // when
            assertThatCode(() -> fcmTokenService.refreshTokenIfNeeded(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .doesNotThrowAnyException();

            // then
            verify(fcmTokenRepository, never()).save(any(FcmToken.class));

            // ✅ 수정: needsRefresh()가 false이므로 lastUsedAt 변경 안 됨
            assertThat(token.getLastUsedAt()).isEqualTo(recentTime);
        }
    }

    @Nested
    @DisplayName("FCM 토큰 삭제 테스트")
    class DeleteTokenTests {

        @Test
        @DisplayName("FCM 토큰 삭제 - 성공")
        void deleteToken_Success() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            FcmToken token = FcmToken.builder()
                    .member(member)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .lastUsedAt(LocalDateTime.now())
                    .build();

            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.of(token));
            willDoNothing().given(fcmTokenRepository).delete(token);

            // when
            assertThatCode(() -> fcmTokenService.deleteToken(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .doesNotThrowAnyException();

            // then
            verify(fcmTokenRepository).delete(token);
        }

        @Test
        @DisplayName("FCM 토큰 삭제 - 토큰이 존재하지 않으면 예외 발생")
        void deleteToken_ThrowsException_WhenTokenNotFound() {
            // given
            given(fcmTokenRepository.findByMemberIdAndDeviceId(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> fcmTokenService.deleteToken(TEST_MEMBER_ID, TEST_DEVICE_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FcmErrorCode.TOKEN_NOT_FOUND);

            verify(fcmTokenRepository, never()).delete(any(FcmToken.class));
        }
    }

    @Nested
    @DisplayName("FCM 토큰 조회 테스트")
    class GetTokensTests {

        @Test
        @DisplayName("FCM 토큰 목록 조회 - 성공")
        void getTokens_Success() {
            // given
            Member member = new Member(TEST_MEMBER_ID);
            List<FcmToken> tokens = List.of(
                    FcmToken.builder()
                            .member(member)
                            .token(TEST_TOKEN)
                            .deviceId(TEST_DEVICE_ID)
                            .deviceType(DeviceType.ANDROID)
                            .lastUsedAt(LocalDateTime.now())
                            .build(),
                    FcmToken.builder()
                            .member(member)
                            .token("another-token-12345")
                            .deviceId("another-device-id")
                            .deviceType(DeviceType.IOS)
                            .lastUsedAt(LocalDateTime.now())
                            .build()
            );

            given(fcmTokenRepository.findAllByMemberId(TEST_MEMBER_ID)).willReturn(tokens);

            // when
            List<FcmTokenResponse> responses = fcmTokenService.getTokensByMemberId(TEST_MEMBER_ID);

            // then
            assertThat(responses).hasSize(2);
            verify(fcmTokenRepository).findAllByMemberId(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("FCM 토큰 목록 조회 - 빈 리스트 반환")
        void getTokens_ReturnsEmptyList_WhenNoTokens() {
            // given
            given(fcmTokenRepository.findAllByMemberId(TEST_MEMBER_ID)).willReturn(List.of());

            // when
            List<FcmTokenResponse> responses = fcmTokenService.getTokensByMemberId(TEST_MEMBER_ID);

            // then
            assertThat(responses).isEmpty();
            verify(fcmTokenRepository).findAllByMemberId(TEST_MEMBER_ID);
        }
    }
}