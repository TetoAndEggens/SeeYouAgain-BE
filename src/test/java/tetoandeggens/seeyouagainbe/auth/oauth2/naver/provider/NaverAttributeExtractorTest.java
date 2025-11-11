package tetoandeggens.seeyouagainbe.auth.oauth2.naver.provider;

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
@DisplayName("NaverAttributeExtractor 단위 테스트")
class NaverAttributeExtractorTest {

    @InjectMocks
    private NaverAttributeExtractor naverAttributeExtractor;

    @Nested
    @DisplayName("소셜 ID 추출 테스트")
    class ExtractSocialIdTests {

        @Test
        @DisplayName("네이버 소셜 ID 추출 - 성공")
        void extractSocialId_Success() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> response = new HashMap<>();
            response.put("id", "naver123456789");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", response);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = naverAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isEqualTo("naver123456789");
        }

        @Test
        @DisplayName("네이버 소셜 ID 추출 - response가 null이면 NullPointerException 발생")
        void extractSocialId_ThrowsException_WhenResponseIsNull() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    NullPointerException.class,
                    () -> naverAttributeExtractor.extractSocialId(oAuth2User)
            );
        }

        @Test
        @DisplayName("네이버 소셜 ID 추출 - ID가 null이면 null 반환")
        void extractSocialId_ReturnsNull_WhenIdIsNull() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> response = new HashMap<>();
            response.put("id", null);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", response);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = naverAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isNull();
        }
    }

    @Nested
    @DisplayName("프로필 이미지 URL 추출 테스트")
    class ExtractProfileImageUrlTests {

        @Test
        @DisplayName("프로필 이미지 URL 추출 - 성공")
        void extractProfileImageUrl_Success() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> response = new HashMap<>();
            response.put("profile_image", "https://naver.com/profile.jpg");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", response);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = naverAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isEqualTo("https://naver.com/profile.jpg");
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - 빈 문자열이면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenBlankString() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> response = new HashMap<>();
            response.put("profile_image", "   ");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", response);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = naverAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - profile_image가 null이면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenProfileImageIsNull() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> response = new HashMap<>();
            response.put("profile_image", null);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", response);

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = naverAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - response가 없으면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenNoResponse() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = naverAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - response가 Map이 아니면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenResponseIsNotMap() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", "not a map");

            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = naverAttributeExtractor.extractProfileImageUrl(oAuth2User);

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
            String profileImageUrl = naverAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }
    }
}