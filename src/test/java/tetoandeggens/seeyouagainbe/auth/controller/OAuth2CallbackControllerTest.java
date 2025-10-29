package tetoandeggens.seeyouagainbe.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tetoandeggens.seeyouagainbe.auth.service.OAuth2Service;
import tetoandeggens.seeyouagainbe.global.ControllerTest;

@WebMvcTest(controllers = OAuth2CallbackController.class)
@DisplayName("OAuth2CallbackController 테스트")
class OAuth2CallbackControllerTest extends ControllerTest {
    @MockitoBean
    private OAuth2Service oAuth2Service;

    @Nested
    @DisplayName("카카오 OAuth2 콜백")
    class KakaoCallbackTests {
        @Test
        @DisplayName("성공 호출")
        void kakaoCallback_Success() throws Exception {
            doNothing().when(oAuth2Service).socialLogin(eq("kakao"), eq("authCode123"), any(HttpServletResponse.class));

            mockMvc.perform(get("/login/oauth2/code/kakao")
                            .param("code", "authCode123"))
                    .andExpect(status().isOk());

            verify(oAuth2Service).socialLogin(eq("kakao"), eq("authCode123"), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("OAuth2Service에서 예외 발생")
        void kakaoCallback_ServiceThrows_5xx() throws Exception {
            doThrow(new RuntimeException("카카오 API 오류"))
                    .when(oAuth2Service).socialLogin(eq("kakao"), anyString(), any());

            mockMvc.perform(get("/login/oauth2/code/kakao")
                            .param("code", "authCode999"))
                    .andExpect(status().is5xxServerError());

            verify(oAuth2Service).socialLogin(eq("kakao"), eq("authCode999"), any());
        }
    }

    @Nested
    @DisplayName("네이버 OAuth2 콜백")
    class NaverCallbackTests {
        @Test
        @DisplayName("성공 호출")
        void naverCallback_Success() throws Exception {
            doNothing().when(oAuth2Service).socialLogin(eq("naver"), eq("authCode456"), any(HttpServletResponse.class));

            mockMvc.perform(get("/login/oauth2/code/naver")
                            .param("code", "authCode456")
                            .param("state", "xyz"))
                    .andExpect(status().isOk());

            verify(oAuth2Service).socialLogin(eq("naver"), eq("authCode456"), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("OAuth2Service 내부 예외")
        void naverCallback_ServiceError() throws Exception {
            doThrow(new IllegalStateException("Naver Token Exchange 실패"))
                    .when(oAuth2Service).socialLogin(eq("naver"), anyString(), any());

            mockMvc.perform(get("/login/oauth2/code/naver")
                            .param("code", "errorCode")
                            .param("state", "xyz"))
                    .andExpect(status().is5xxServerError());

            verify(oAuth2Service).socialLogin(eq("naver"), eq("errorCode"), any());
        }
    }

    @Nested
    @DisplayName("구글 OAuth2 콜백")
    class GoogleCallbackTests {
        @Test
        @DisplayName("성공 호출")
        void googleCallback_Success() throws Exception {
            doNothing().when(oAuth2Service).socialLogin(eq("google"), eq("authCode789"), any(HttpServletResponse.class));

            mockMvc.perform(get("/login/oauth2/code/google")
                            .param("code", "authCode789"))
                    .andExpect(status().isOk());

            verify(oAuth2Service).socialLogin(eq("google"), eq("authCode789"), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("OAuth2Service 예외")
        void googleCallback_ServiceThrows_5xx() throws Exception {
            doThrow(new RuntimeException("Google Token Validation 실패"))
                    .when(oAuth2Service).socialLogin(eq("google"), anyString(), any());

            mockMvc.perform(get("/login/oauth2/code/google")
                            .param("code", "brokenCode"))
                    .andExpect(status().is5xxServerError());

            verify(oAuth2Service).socialLogin(eq("google"), eq("brokenCode"), any());
        }
    }
}