package tetoandeggens.seeyouagainbe.auth.oauth2.google.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleAttributeExtractor 단위 테스트")
class GoogleAttributeExtractorTest {

    @InjectMocks
    private GoogleAttributeExtractor googleAttributeExtractor;

    @Nested
    @DisplayName("소셜 ID 추출 테스트")
    class ExtractSocialIdTests {

        @Test
        @DisplayName("구글 소셜 ID 추출 - OidcUser에서 성공")
        void extractSocialId_Success_FromOidcUser() {
            // given
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "google123456789");

            OidcIdToken idToken = new OidcIdToken(
                    "token-value",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    claims
            );

            OidcUser oidcUser = new DefaultOidcUser(null, idToken);

            // when
            String socialId = googleAttributeExtractor.extractSocialId(oidcUser);

            // then
            assertThat(socialId).isEqualTo("google123456789");
        }

        @Test
        @DisplayName("구글 소셜 ID 추출 - 일반 OAuth2User에서 성공")
        void extractSocialId_Success_FromOAuth2User() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", "google987654321");
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = googleAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isEqualTo("google987654321");
        }

        @Test
        @DisplayName("구글 소셜 ID 추출 - sub가 null이면 null 반환")
        void extractSocialId_ReturnsNull_WhenSubIsNull() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", null);
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = googleAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isNull();
        }

        @Test
        @DisplayName("구글 소셜 ID 추출 - sub가 없으면 null 반환")
        void extractSocialId_ReturnsNull_WhenSubNotExists() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String socialId = googleAttributeExtractor.extractSocialId(oAuth2User);

            // then
            assertThat(socialId).isNull();
        }
    }

    @Nested
    @DisplayName("프로필 이미지 URL 추출 테스트")
    class ExtractProfileImageUrlTests {

        @Test
        @DisplayName("프로필 이미지 URL 추출 - OidcUser에서 성공")
        void extractProfileImageUrl_Success_FromOidcUser() {
            // given
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "google123");
            claims.put("picture", "https://google.com/profile.jpg");

            OidcIdToken idToken = new OidcIdToken(
                    "token-value",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    claims
            );

            OidcUser oidcUser = new DefaultOidcUser(null, idToken);

            // when
            String profileImageUrl = googleAttributeExtractor.extractProfileImageUrl(oidcUser);

            // then
            assertThat(profileImageUrl).isEqualTo("https://google.com/profile.jpg");
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - 일반 OAuth2User에서 성공")
        void extractProfileImageUrl_Success_FromOAuth2User() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("picture", "https://google.com/profile.jpg");
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = googleAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isEqualTo("https://google.com/profile.jpg");
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - OidcUser에서 picture가 null이면 null 반환")
        void extractProfileImageUrl_ReturnsNull_FromOidcUser_WhenPictureIsNull() {
            // given
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "google123");
            claims.put("picture", null);

            OidcIdToken idToken = new OidcIdToken(
                    "token-value",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    claims
            );

            OidcUser oidcUser = new DefaultOidcUser(null, idToken);

            // when
            String profileImageUrl = googleAttributeExtractor.extractProfileImageUrl(oidcUser);

            // then
            assertThat(profileImageUrl).isNull();
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - picture가 없으면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenPictureNotExists() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = googleAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }

        @Test
        @DisplayName("프로필 이미지 URL 추출 - picture가 null이면 null 반환")
        void extractProfileImageUrl_ReturnsNull_WhenPictureIsNull() {
            // given
            OAuth2User oAuth2User = mock(OAuth2User.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("picture", null);
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            // when
            String profileImageUrl = googleAttributeExtractor.extractProfileImageUrl(oAuth2User);

            // then
            assertThat(profileImageUrl).isNull();
        }
    }

    @Nested
    @DisplayName("OIDC 통합 테스트")
    class OidcIntegrationTests {

        @Test
        @DisplayName("OIDC 사용자 정보 추출 - 모든 정보 포함 성공")
        void extractOidcUserInfo_Success_WithAllData() {
            // given
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "google123456789");
            claims.put("picture", "https://google.com/profile.jpg");
            claims.put("email", "test@gmail.com");
            claims.put("name", "Test User");

            OidcIdToken idToken = new OidcIdToken(
                    "token-value",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    claims
            );

            OidcUser oidcUser = new DefaultOidcUser(null, idToken);

            // when
            String socialId = googleAttributeExtractor.extractSocialId(oidcUser);
            String profileImageUrl = googleAttributeExtractor.extractProfileImageUrl(oidcUser);

            // then
            assertThat(socialId).isEqualTo("google123456789");
            assertThat(profileImageUrl).isEqualTo("https://google.com/profile.jpg");
        }

        @Test
        @DisplayName("OIDC 사용자 정보 추출 - 프로필 이미지 없이 성공")
        void extractOidcUserInfo_Success_WithoutProfileImage() {
            // given
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "google123456789");
            claims.put("email", "test@gmail.com");

            OidcIdToken idToken = new OidcIdToken(
                    "token-value",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    claims
            );

            OidcUser oidcUser = new DefaultOidcUser(null, idToken);

            // when
            String socialId = googleAttributeExtractor.extractSocialId(oidcUser);
            String profileImageUrl = googleAttributeExtractor.extractProfileImageUrl(oidcUser);

            // then
            assertThat(socialId).isEqualTo("google123456789");
            assertThat(profileImageUrl).isNull();
        }
    }
}