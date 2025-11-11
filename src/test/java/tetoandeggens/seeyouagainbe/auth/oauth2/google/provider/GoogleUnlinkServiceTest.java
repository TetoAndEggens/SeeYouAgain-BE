package tetoandeggens.seeyouagainbe.auth.oauth2.google.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tetoandeggens.seeyouagainbe.auth.oauth2.google.client.GoogleApiClient;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleUnlinkService 단위 테스트")
class GoogleUnlinkServiceTest {

    @Mock
    private GoogleApiClient googleApiClient;

    @InjectMocks
    private GoogleUnlinkService googleUnlinkService;

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";
    private static final String TEST_ACCESS_TOKEN = "test-access-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleUnlinkService, "googleClientId", TEST_CLIENT_ID);
        ReflectionTestUtils.setField(googleUnlinkService, "googleClientSecret", TEST_CLIENT_SECRET);
    }

    @Nested
    @DisplayName("구글 연동 해제 테스트")
    class UnlinkTests {

        @Test
        @DisplayName("구글 연동 해제 - 성공")
        void unlink_Success() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "googleRefreshToken", TEST_REFRESH_TOKEN);

            Map<String, Object> tokenResponse = new HashMap<>();
            tokenResponse.put("access_token", TEST_ACCESS_TOKEN);

            when(googleApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenReturn(tokenResponse);

            doNothing().when(googleApiClient).revokeToken(anyString());

            // when
            boolean result = googleUnlinkService.unlink(member);

            // then
            assertThat(result).isTrue();

            verify(googleApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );

            verify(googleApiClient).revokeToken(eq(TEST_ACCESS_TOKEN));
        }

        @Test
        @DisplayName("구글 연동 해제 - RefreshToken이 없으면 true 반환")
        void unlink_ReturnsTrue_WhenNoRefreshToken() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            // when
            boolean result = googleUnlinkService.unlink(member);

            // then
            assertThat(result).isTrue();
            verify(googleApiClient, never()).refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            );
            verify(googleApiClient, never()).revokeToken(anyString());
        }

        @Test
        @DisplayName("구글 연동 해제 - AccessToken 재발급 실패 시 false 반환")
        void unlink_ReturnsFalse_OnTokenRefreshFailure() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "googleRefreshToken", TEST_REFRESH_TOKEN);

            when(googleApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenThrow(new RuntimeException("토큰 재발급 실패"));

            // when
            boolean result = googleUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(googleApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );
            verify(googleApiClient, never()).revokeToken(anyString());
        }

        @Test
        @DisplayName("구글 연동 해제 - AccessToken이 null이면 false 반환")
        void unlink_ReturnsFalse_WhenAccessTokenIsNull() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "googleRefreshToken", TEST_REFRESH_TOKEN);

            Map<String, Object> tokenResponse = new HashMap<>();
            tokenResponse.put("access_token", null);

            when(googleApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenReturn(tokenResponse);

            // when
            boolean result = googleUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(googleApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );
            verify(googleApiClient, never()).revokeToken(anyString());
        }

        @Test
        @DisplayName("구글 연동 해제 - 연동 해제 API 호출 실패 시 false 반환")
        void unlink_ReturnsFalse_OnRevokeFailure() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "googleRefreshToken", TEST_REFRESH_TOKEN);

            Map<String, Object> tokenResponse = new HashMap<>();
            tokenResponse.put("access_token", TEST_ACCESS_TOKEN);

            when(googleApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenReturn(tokenResponse);

            doThrow(new RuntimeException("연동 해제 실패"))
                    .when(googleApiClient)
                    .revokeToken(anyString());

            // when
            boolean result = googleUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();

            verify(googleApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );

            verify(googleApiClient).revokeToken(eq(TEST_ACCESS_TOKEN));
        }

        @Test
        @DisplayName("구글 연동 해제 - 네트워크 예외 발생 시 false 반환")
        void unlink_ReturnsFalse_OnNetworkException() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "googleRefreshToken", TEST_REFRESH_TOKEN);

            when(googleApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenThrow(new feign.RetryableException(
                    500,
                    "Network Error",
                    feign.Request.HttpMethod.POST,
                    (Long) null,
                    mock(feign.Request.class)
            ));

            // when
            boolean result = googleUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(googleApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );
        }

        @Test
        @DisplayName("구글 연동 해제 - AccessToken 재발급 응답이 비어있으면 false 반환")
        void unlink_ReturnsFalse_WhenEmptyTokenResponse() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "googleRefreshToken", TEST_REFRESH_TOKEN);

            Map<String, Object> tokenResponse = new HashMap<>();

            when(googleApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenReturn(tokenResponse);

            // when
            boolean result = googleUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(googleApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );
            verify(googleApiClient, never()).revokeToken(anyString());
        }
    }
}