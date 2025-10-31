package tetoandeggens.seeyouagainbe.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.auth.dto.request.*;
import tetoandeggens.seeyouagainbe.auth.dto.response.LoginResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialLoginResultResponse;
import tetoandeggens.seeyouagainbe.auth.service.AuthService;
import tetoandeggens.seeyouagainbe.auth.service.OAuth2Service;
import tetoandeggens.seeyouagainbe.global.ControllerTest;

@WebMvcTest(controllers = AuthController.class)
@DisplayName("Auth 컨트롤러 테스트")
class AuthControllerTest extends ControllerTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OAuth2Service oAuth2Service;

    @Nested
    @DisplayName("loginId 중복 체크 API 테스트")
    class CheckLoginIdTests {
        @Test
        @DisplayName("loginId 중복 체크 - 성공")
        void checkLoginId_Success() throws Exception {
            String loginId = "testuser123";
            doNothing().when(authService).checkLoginIdAvailable(anyString());

            mockMvc.perform(get("/auth/check/loginId")
                            .param("loginId", loginId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(authService).checkLoginIdAvailable(loginId);
        }

        @Test
        @DisplayName("loginId 중복 체크 - 파라미터 없으면 실패")
        void checkLoginId_ValidationFail_NoParam() throws Exception {
            mockMvc.perform(get("/auth/check/loginId"))
                    .andExpect(status().is5xxServerError());

            verify(authService, never()).checkLoginIdAvailable(anyString());
        }

        @Test
        @DisplayName("loginId 중복 체크 - 빈 문자열이면 실패")
        void checkLoginId_ValidationFail_EmptyString() throws Exception {
            mockMvc.perform(get("/auth/check/loginId")
                            .param("loginId", ""))
                    .andExpect(status().is5xxServerError());

            verify(authService, never()).checkLoginIdAvailable(anyString());
        }
    }

    @Nested
    @DisplayName("휴대폰 번호 중복 체크 API 테스트")
    class CheckPhoneNumberTests {
        @Test
        @DisplayName("휴대폰 번호 중복 체크 - 성공")
        void checkPhoneNumber_Success() throws Exception {
            PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");
            doNothing().when(authService).checkPhoneNumberDuplicate(anyString());

            mockMvc.perform(post("/auth/check/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(authService).checkPhoneNumberDuplicate("01012345678");
        }

        @Test
        @DisplayName("휴대폰 번호 중복 체크 - 전화번호가 null이면 실패")
        void checkPhoneNumber_ValidationFail_NullPhone() throws Exception {
            String requestBody = "{\"phone\":null}";

            mockMvc.perform(post("/auth/check/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).checkPhoneNumberDuplicate(anyString());
        }

        @Test
        @DisplayName("휴대폰 번호 중복 체크 - 잘못된 형식이면 실패")
        void checkPhoneNumber_ValidationFail_InvalidFormat() throws Exception {
            PhoneVerificationRequest request = new PhoneVerificationRequest("123");

            mockMvc.perform(post("/auth/check/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).checkPhoneNumberDuplicate(anyString());
        }
    }

    @Nested
    @DisplayName("휴대폰 인증 코드 전송 API 테스트")
    class SendPhoneVerificationCodeTests {
        @Test
        @DisplayName("휴대폰 인증 코드 요청 - 성공")
        void sendPhoneVerificationCode_Success() throws Exception {
            PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");
            PhoneVerificationResultResponse response = new PhoneVerificationResultResponse(
                    "123456",
                    "test@seeyouagain.com"
            );

            when(authService.sendPhoneVerificationCode(anyString())).thenReturn(response);

            mockMvc.perform(post("/auth/phone/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.code").value("123456"))
                    .andExpect(jsonPath("$.data.emailAddress").value("test@seeyouagain.com"));

            verify(authService).sendPhoneVerificationCode("01012345678");
        }

        @Test
        @DisplayName("휴대폰 인증 코드 요청 - 전화번호가 null이면 실패")
        void sendPhoneVerificationCode_ValidationFail_NullPhone() throws Exception {
            String requestBody = "{\"phone\":null}";

            mockMvc.perform(post("/auth/phone/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).sendPhoneVerificationCode(anyString());
        }

        @Test
        @DisplayName("휴대폰 인증 코드 요청 - 잘못된 형식이면 실패")
        void sendPhoneVerificationCode_ValidationFail_InvalidFormat() throws Exception {
            PhoneVerificationRequest request = new PhoneVerificationRequest("invalid-phone");

            mockMvc.perform(post("/auth/phone/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).sendPhoneVerificationCode(anyString());
        }
    }

    @Nested
    @DisplayName("휴대폰 인증 코드 검증 API 테스트")
    class VerifyPhoneCodeTests {
        @Test
        @DisplayName("휴대폰 인증 코드 검증 - 성공")
        void verifyPhoneCode_Success() throws Exception {
            PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");
            doNothing().when(authService).verifyPhoneCode(anyString());

            mockMvc.perform(post("/auth/phone/verify-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(authService).verifyPhoneCode("01012345678");
        }

        @Test
        @DisplayName("휴대폰 인증 코드 검증 - 전화번호가 null이면 실패")
        void verifyPhoneCode_ValidationFail_NullPhone() throws Exception {
            String requestBody = "{\"phone\":null}";

            mockMvc.perform(post("/auth/phone/verify-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).verifyPhoneCode(anyString());
        }

        @Test
        @DisplayName("휴대폰 인증 코드 검증 - 잘못된 형식이면 실패")
        void verifyPhoneCode_ValidationFail_InvalidFormat() throws Exception {
            PhoneVerificationRequest request = new PhoneVerificationRequest("123");

            mockMvc.perform(post("/auth/phone/verify-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).verifyPhoneCode(anyString());
        }
    }

    @Nested
    @DisplayName("회원가입 API 테스트")
    class RegisterTests {
        @Test
        @DisplayName("회원가입 - 성공")
        void register_Success() throws Exception {
            UnifiedRegisterRequest request = new UnifiedRegisterRequest(
                    "testuser123",
                    "Password123!",
                    "테스트",
                    "01012345678",
                    null,
                    null,
                    null
            );
            doNothing().when(authService).unifiedRegister(any(UnifiedRegisterRequest.class));

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(201));

            verify(authService).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - loginId가 null이면 실패")
        void register_ValidationFail_LoginIdNull() throws Exception {
            String requestBody = "{\"loginId\":null,\"password\":\"Password123!\",\"nickName\":\"테스트\",\"phoneNumber\":\"01012345678\"}";

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - loginId가 빈 문자열이면 실패")
        void register_ValidationFail_LoginIdEmpty() throws Exception {
            UnifiedRegisterRequest request = new UnifiedRegisterRequest(
                    "",
                    "Password123!",
                    "테스트",
                    "01012345678",
                    null,
                    null,
                    null
            );

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - password가 null이면 실패")
        void register_ValidationFail_PasswordNull() throws Exception {
            String requestBody = "{\"loginId\":\"testuser\",\"password\":null,\"nickName\":\"테스트\",\"phoneNumber\":\"01012345678\"}";

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - password가 빈 문자열이면 실패")
        void register_ValidationFail_PasswordEmpty() throws Exception {
            UnifiedRegisterRequest request = new UnifiedRegisterRequest(
                    "testuser",
                    "",
                    "테스트",
                    "01012345678",
                    null,
                    null,
                    null
            );

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - nickName이 null이면 실패")
        void register_ValidationFail_NickNameNull() throws Exception {
            String requestBody = "{\"loginId\":\"testuser\",\"password\":\"Password123!\",\"nickName\":null,\"phoneNumber\":\"01012345678\"}";

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - nickName이 빈 문자열이면 실패")
        void register_ValidationFail_NickNameEmpty() throws Exception {
            UnifiedRegisterRequest request = new UnifiedRegisterRequest(
                    "testuser",
                    "Password123!",
                    "",
                    "01012345678",
                    null,
                    null,
                    null
            );

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - phoneNumber가 null이면 실패")
        void register_ValidationFail_PhoneNumberNull() throws Exception {
            String requestBody = "{\"loginId\":\"testuser\",\"password\":\"Password123!\",\"nickName\":\"테스트\",\"phoneNumber\":null}";

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - phoneNumber가 빈 문자열이면 실패")
        void register_ValidationFail_PhoneNumberEmpty() throws Exception {
            UnifiedRegisterRequest request = new UnifiedRegisterRequest(
                    "testuser",
                    "Password123!",
                    "테스트",
                    "",
                    null,
                    null,
                    null
            );

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }

        @Test
        @DisplayName("회원가입 - phoneNumber가 잘못된 형식이면 실패")
        void register_ValidationFail_PhoneNumberInvalidFormat() throws Exception {
            UnifiedRegisterRequest request = new UnifiedRegisterRequest(
                    "testuser",
                    "Password123!",
                    "테스트",
                    "123456",
                    null,
                    null,
                    null
            );

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).unifiedRegister(any(UnifiedRegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("소셜 인증 관련 테스트")
    class SocialAuthTests {

        @Nested
        @DisplayName("[소셜] 휴대폰 인증 코드 요청")
        class SocialSendCode {

            @Test
            @DisplayName("성공 시 200 OK")
            void sendSocialPhoneVerificationCode_Success() throws Exception {
                SocialPhoneVerificationRequest request = new SocialPhoneVerificationRequest(
                        "01012345678", "kakao", "SOC1234", "https://profile.jpg"
                );
                PhoneVerificationResultResponse mockResponse =
                        new PhoneVerificationResultResponse("123456", "server@seeyouagain.com");

                when(oAuth2Service.sendSocialPhoneVerificationCode(
                        anyString(), anyString(), anyString(), anyString()))
                        .thenReturn(mockResponse);

                mockMvc.perform(post("/auth/social/phone/send-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk());

                verify(oAuth2Service).sendSocialPhoneVerificationCode(
                        eq("01012345678"), eq("kakao"), eq("SOC1234"), eq("https://profile.jpg"));
            }

            @Test
            @DisplayName("잘못된 요청 본문 시 400 반환")
            void sendSocialPhoneVerificationCode_BadRequest() throws Exception {
                String invalidJson = """
                {"provider":"kakao","socialId":"SOC1234","profileImageUrl":"https://p.jpg"}
                """;

                mockMvc.perform(post("/auth/social/phone/send-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).sendSocialPhoneVerificationCode(any(), any(), any(), any());
            }
        }

        @Nested
        @DisplayName("[소셜] 휴대폰 인증 코드 검증")
        class SocialVerifyCode {

            @Test
            @DisplayName("성공 시 200 OK")
            void verifySocialPhoneCode_Success() throws Exception {
                PhoneVerificationRequest request = new PhoneVerificationRequest("01099998888");
                SocialLoginResultResponse mockResult = new SocialLoginResultResponse("LINK", "소셜 계정 연동 필요", new LoginResponse("a", "r", null));

                when(oAuth2Service.verifySocialPhoneCode(eq("01099998888"), any(HttpServletResponse.class)))
                        .thenReturn(mockResult);

                mockMvc.perform(post("/auth/social/phone/verify-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk());

                verify(oAuth2Service).verifySocialPhoneCode(eq("01099998888"), any(HttpServletResponse.class));
            }

            @Test
            @DisplayName("요청 본문 누락 시 400 반환")
            void verifySocialPhoneCode_BadRequest() throws Exception {
                mockMvc.perform(post("/auth/social/phone/verify-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).verifySocialPhoneCode(any(), any());
            }
        }

        @Nested
        @DisplayName("[소셜] 기존 계정 연동")
        class SocialLink {

            @Test
            @DisplayName("성공 시 200 OK")
            void linkSocialAccount_Success() throws Exception {
                SocialLinkRequest request = new SocialLinkRequest("01077776666");
                SocialLoginResultResponse mockResult = new SocialLoginResultResponse("LINK", "소셜 계정 연동 필요", new LoginResponse("a", "r", null));

                when(oAuth2Service.linkSocialAccount(eq("01077776666"), any(HttpServletResponse.class)))
                        .thenReturn(mockResult);

                mockMvc.perform(post("/auth/social/link")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk());

                verify(oAuth2Service).linkSocialAccount(eq("01077776666"), any(HttpServletResponse.class));
            }

            @Test
            @DisplayName("요청 필드 누락 시 400 반환")
            void linkSocialAccount_BadRequest() throws Exception {
                mockMvc.perform(post("/auth/social/link")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).linkSocialAccount(any(), any());
            }
        }
    }


    @Nested
    @DisplayName("토큰 재발급 API 테스트")
    class ReissueTokenTests {
        @Test
        @DisplayName("토큰 재발급 - 성공")
        void reissueToken_Success() throws Exception {
            ReissueTokenResponse response = ReissueTokenResponse.builder()
                    .accessToken("new_access_token")
                    .build();

            when(authService.reissueToken(any(), any())).thenReturn(response);

            mockMvc.perform(post("/auth/reissue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));

            verify(authService).reissueToken(any(), any());
        }

        @Test
        @DisplayName("토큰 재발급 - RefreshToken이 없으면 실패 (서비스에서 예외 발생)")
        void reissueToken_Fail_NoRefreshToken() throws Exception {
            when(authService.reissueToken(any(), any()))
                    .thenThrow(new IllegalArgumentException("Refresh Token not found"));

            mockMvc.perform(post("/auth/reissue"))
                    .andExpect(status().is5xxServerError());

            verify(authService).reissueToken(any(), any());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 API 테스트")
    class WithdrawalTests {

        @Test
        @DisplayName("회원탈퇴 - 성공 (일반 회원)")
        void withdrawMember_Success_NormalMember() throws Exception {
            WithdrawalRequest request = new WithdrawalRequest("Password123!", "서비스 이용 불편");

            CustomUserDetails customUser = CustomUserDetails.fromClaims("test-uuid-123", "ROLE_USER");

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(customUser, null, customUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            doNothing().when(authService).withdrawMember(anyString(), any(WithdrawalRequest.class));

            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(authService).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }

        @Test
        @DisplayName("회원탈퇴 - 비밀번호가 null이면 실패")
        void withdrawMember_ValidationFail_PasswordNull() throws Exception {
            String requestBody = "{\"password\":null,\"reason\":\"test\"}";

            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }

        @Test
        @DisplayName("회원탈퇴 - 비밀번호가 빈 문자열이면 실패")
        void withdrawMember_ValidationFail_PasswordEmpty() throws Exception {
            WithdrawalRequest request = new WithdrawalRequest("", "test");

            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }

        @Test
        @DisplayName("회원탈퇴 - 탈퇴 사유는 선택사항 (null 가능)")
        void withdrawMember_Success_ReasonOptional() throws Exception {
            WithdrawalRequest request = new WithdrawalRequest("Password123!", null);

            CustomUserDetails customUser = CustomUserDetails.fromClaims("test-uuid-123", "ROLE_USER");

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(customUser, null, customUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            doNothing().when(authService).withdrawMember(anyString(), any(WithdrawalRequest.class));

            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(authService).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }
    }
}