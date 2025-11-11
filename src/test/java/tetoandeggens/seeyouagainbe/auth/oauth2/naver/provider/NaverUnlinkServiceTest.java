package tetoandeggens.seeyouagainbe.auth.oauth2.naver.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tetoandeggens.seeyouagainbe.auth.oauth2.naver.client.NaverApiClient;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NaverUnlinkService 단위 테스트")
class NaverUnlinkServiceTest {

    @Mock
    private NaverApiClient naverApiClient;

    @InjectMocks
    private NaverUnlinkService naverUnlinkService;

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";
    private static final String TEST_ACCESS_TOKEN = "test-access-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(naverUnlinkService, "naverClientId", TEST_CLIENT_ID);
        ReflectionTestUtils.setField(naverUnlinkService, "naverClientSecret", TEST_CLIENT_SECRET);
    }

    @Nested
    @DisplayName("네이버 연동 해제 테스트")
    class UnlinkTests {

        @Test
        @DisplayName("네이버 연동 해제 - 성공")
        void unlink_Success() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "naverRefreshToken", TEST_REFRESH_TOKEN);

            Map<String, Object> tokenResponse = new HashMap<>();
            tokenResponse.put("access_token", TEST_ACCESS_TOKEN);

            when(naverApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenReturn(tokenResponse);

            Map<String, Object> revokeResponse = new HashMap<>();
            revokeResponse.put("result", "success");

            when(naverApiClient.revokeToken(
                    anyString(), anyString(), anyString(), anyString(), anyString()
            )).thenReturn(revokeResponse);

            // when
            boolean result = naverUnlinkService.unlink(member);

            // then
            assertThat(result).isTrue();

            verify(naverApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );

            verify(naverApiClient).revokeToken(
                    eq("delete"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_ACCESS_TOKEN),
                    eq("NAVER")
            );
        }

        @Test
        @DisplayName("네이버 연동 해제 - RefreshToken이 없으면 true 반환")
        void unlink_ReturnsTrue_WhenNoRefreshToken() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            // when
            boolean result = naverUnlinkService.unlink(member);

            // then
            assertThat(result).isTrue();
            verify(naverApiClient, never()).refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            );
            verify(naverApiClient, never()).revokeToken(
                    anyString(), anyString(), anyString(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("네이버 연동 해제 - AccessToken 재발급 실패 시 false 반환")
        void unlink_ReturnsFalse_OnTokenRefreshFailure() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "naverRefreshToken", TEST_REFRESH_TOKEN);

            when(naverApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenThrow(new RuntimeException("토큰 재발급 실패"));

            // when
            boolean result = naverUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(naverApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );
            verify(naverApiClient, never()).revokeToken(
                    anyString(), anyString(), anyString(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("네이버 연동 해제 - 연동 해제 API 호출 실패 시 false 반환")
        void unlink_ReturnsFalse_OnRevokeFailure() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "naverRefreshToken", TEST_REFRESH_TOKEN);

            Map<String, Object> tokenResponse = new HashMap<>();
            tokenResponse.put("access_token", TEST_ACCESS_TOKEN);

            when(naverApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenReturn(tokenResponse);

            when(naverApiClient.revokeToken(
                    anyString(), anyString(), anyString(), anyString(), anyString()
            )).thenThrow(new RuntimeException("연동 해제 실패"));

            // when
            boolean result = naverUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();

            verify(naverApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );

            verify(naverApiClient).revokeToken(
                    eq("delete"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_ACCESS_TOKEN),
                    eq("NAVER")
            );
        }

        @Test
        @DisplayName("네이버 연동 해제 - 네트워크 예외 발생 시 false 반환")
        void unlink_ReturnsFalse_OnNetworkException() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "naverRefreshToken", TEST_REFRESH_TOKEN);

            when(naverApiClient.refreshAccessToken(
                    anyString(), anyString(), anyString(), anyString()
            )).thenThrow(new feign.RetryableException(
                    500,
                    "Network Error",
                    feign.Request.HttpMethod.POST,
                    (Long) null,
                    mock(feign.Request.class)
            ));

            // when
            boolean result = naverUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(naverApiClient).refreshAccessToken(
                    eq("refresh_token"),
                    eq(TEST_CLIENT_ID),
                    eq(TEST_CLIENT_SECRET),
                    eq(TEST_REFRESH_TOKEN)
            );
        }
    }
}