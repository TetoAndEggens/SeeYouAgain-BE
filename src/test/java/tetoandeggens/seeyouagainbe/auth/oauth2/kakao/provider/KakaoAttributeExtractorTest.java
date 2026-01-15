package tetoandeggens.seeyouagainbe.auth.oauth2.kakao.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoAttributeExtractor 단위 테스트")
class KakaoAttributeExtractorTest {
    @InjectMocks
    private KakaoAttributeExtractor kakaoAttributeExtractor;

    @Nested
    @DisplayName("소셜 ID 추출 테스트")
    class ExtractSocialIdTests {

        @Test
        @DisplayName("카카오 소셜 ID 추출 - 성공")
        void extractSocialId_Success() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", 123456789L);
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = kakaoAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isEqualTo("123456789");
        }

        @Test
        @DisplayName("카카오 소셜 ID 추출 - ID가 String 타입일 때 성공")
        void extractSocialId_Success_WhenIdIsString() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", "123456789");
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = kakaoAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isEqualTo("123456789");
        }

        @Test
        @DisplayName("카카오 소셜 ID 추출 - ID가 null이면 null 반환")
        void extractSocialId_ReturnsNull_WhenIdIsNull() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", null);
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = kakaoAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("프로필 이미지 URL 추출 테스트")
    class ExtractProfileImageUrlTests {

        @Test
        @DisplayName("프로필 이미지 URL 추출 - kakao_account.profile에서 성공")
        void extractProfileImageUrl_Success_FromKakaoAccountProfile() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> profile = new HashMap<>();
            profile.put("profile_image_url", "https://kakao.com/profile.jpg");

            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("profile", profile);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("kakao_account", kakaoAccount);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = kakaoAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isEqualTo("https://kakao.com/profile.jpg");
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - properties에서 성공")
        void extractProfileImageUrl_Success_FromProperties() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> properties = new HashMap<>();
            properties.put("profile_image", "https://kakao.com/profile.jpg");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("properties", properties);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = kakaoAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isEqualTo("https://kakao.com/profile.jpg");
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - kakao_account.profile 우선순위 테스트")
        void extractProfileImageUrl_PrioritizesKakaoAccountProfile() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> profile = new HashMap<>();
            profile.put("profile_image_url", "https://kakao.com/account-profile.jpg");

            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("profile", profile);

            Map<String, Object> properties = new HashMap<>();
            properties.put("profile_image", "https://kakao.com/properties-profile.jpg");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("kakao_account", kakaoAccount);
            attributes.put("properties", properties);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = kakaoAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isEqualTo("https://kakao.com/account-profile.jpg");
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - 빈 문자열이면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenBlankString() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> profile = new HashMap<>();
            profile.put("profile_image_url", "   ");

            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("profile", profile);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("kakao_account", kakaoAccount);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = kakaoAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - 데이터가 없으면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenNoData() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = kakaoAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - 예외 발생 시 null 반환")
        void extractProfileImageUrl_ReturnsNull_OnException() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            when(oAuth2User.getAttributes()).thenThrow(new RuntimeException("Test exception"));

            // when
            String profileImageUrl = kakaoAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }
    }
}