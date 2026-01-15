package tetoandeggens.seeyouagainbe.auth.oauth2.kakao.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tetoandeggens.seeyouagainbe.auth.oauth2.kakao.client.KakaoApiClient;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoUnlinkService 단위 테스트")
class KakaoUnlinkServiceTest {

    @Mock
    private KakaoApiClient kakaoApiClient;

    @InjectMocks
    private KakaoUnlinkService kakaoUnlinkService;

    private static final String TEST_ADMIN_KEY = "test-admin-key";
    private static final String TEST_SOCIAL_ID = "kakao123456789";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kakaoUnlinkService, "kakaoAdminKey", TEST_ADMIN_KEY);
    }

    @Nested
    @DisplayName("카카오 연동 해제 테스트")
    class UnlinkTests {

        @Test
        @DisplayName("카카오 연동 해제 - 성공")
        void unlink_Success() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            // Reflection으로 private 필드 설정
            ReflectionTestUtils.setField(member, "socialIdKakao", TEST_SOCIAL_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("id", TEST_SOCIAL_ID);

            when(kakaoApiClient.unlinkUser(anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when
            boolean result = kakaoUnlinkService.unlink(member);

            // then
            assertThat(result).isTrue();
            verify(kakaoApiClient).unlinkUser(
                    eq("KakaoAK " + TEST_ADMIN_KEY),
                    eq("user_id"),
                    eq(TEST_SOCIAL_ID)
            );
        }

        @Test
        @DisplayName("카카오 연동 해제 - 소셜 ID가 없으면 true 반환")
        void unlink_ReturnsTrue_WhenNoSocialId() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            // when
            boolean result = kakaoUnlinkService.unlink(member);

            // then
            assertThat(result).isTrue();
            verify(kakaoApiClient, never()).unlinkUser(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("카카오 연동 해제 - API 호출 실패 시 false 반환")
        void unlink_ReturnsFalse_OnApiFailure() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "socialIdKakao", TEST_SOCIAL_ID);

            when(kakaoApiClient.unlinkUser(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("API 호출 실패"));

            // when
            boolean result = kakaoUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(kakaoApiClient).unlinkUser(
                    eq("KakaoAK " + TEST_ADMIN_KEY),
                    eq("user_id"),
                    eq(TEST_SOCIAL_ID)
            );
        }

        @Test
        @DisplayName("카카오 연동 해제 - 네트워크 예외 발생 시 false 반환")
        void unlink_ReturnsFalse_OnNetworkException() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "socialIdKakao", TEST_SOCIAL_ID);

            when(kakaoApiClient.unlinkUser(anyString(), anyString(), anyString()))
                    .thenThrow(new feign.RetryableException(
                            500,
                            "Network Error",
                            feign.Request.HttpMethod.POST,
                            (Long) null,
                            mock(feign.Request.class)
                    ));

            // when
            boolean result = kakaoUnlinkService.unlink(member);

            // then
            assertThat(result).isFalse();
            verify(kakaoApiClient).unlinkUser(
                    eq("KakaoAK " + TEST_ADMIN_KEY),
                    eq("user_id"),
                    eq(TEST_SOCIAL_ID)
            );
        }

        @Test
        @DisplayName("카카오 연동 해제 - 응답이 null이어도 예외 없이 true 반환")
        void unlink_ReturnsTrue_WhenResponseIsNull() {
            // given
            Member member = Member.builder()
                    .loginId("testuser")
                    .password("encodedPassword")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();

            ReflectionTestUtils.setField(member, "socialIdKakao", TEST_SOCIAL_ID);

            when(kakaoApiClient.unlinkUser(anyString(), anyString(), anyString()))
                    .thenReturn(null);

            // when
            boolean result = kakaoUnlinkService.unlink(member);

            // then
            assertThat(result).isTrue();
            verify(kakaoApiClient).unlinkUser(
                    eq("KakaoAK " + TEST_ADMIN_KEY),
                    eq("user_id"),
                    eq(TEST_SOCIAL_ID)
            );
        }
    }
}