package tetoandeggens.seeyouagainbe.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.auth.dto.request.*;
import tetoandeggens.seeyouagainbe.auth.dto.response.LoginResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialLoginResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialTempInfoResponse;
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
        @DisplayName("소셜 임시 정보 조회 API 테스트")
        class GetSocialTempInfoTests {

            @Test
            @DisplayName("소셜 임시 정보 조회 - 성공")
            void getSocialTempInfo_Success() throws Exception {
                // given
                SocialTempInfoResponse response = new SocialTempInfoResponse(
                        "https://example.com/profile.jpg",
                        "temp-uuid-123"
                );

                given(oAuth2Service.getSocialTempInfo(any(HttpServletRequest.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(get("/auth/social/temp-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.jpg"))
                        .andExpect(jsonPath("$.data.tempUuid").value("temp-uuid-123"));

                verify(oAuth2Service).getSocialTempInfo(any(HttpServletRequest.class));
            }
        }

        @Nested
        @DisplayName("소셜 휴대폰 인증 코드 전송 API 테스트")
        class SendSocialPhoneVerificationCodeTests {

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 전송 - 성공")
            void sendSocialPhoneVerificationCode_Success() throws Exception {
                // given
                SocialPhoneVerificationRequest request = new SocialPhoneVerificationRequest(
                        "01012345678",
                        "temp-uuid-123"
                );

                PhoneVerificationResultResponse response = new PhoneVerificationResultResponse(
                        "123456",
                        "test@seeyouagain.com"
                );

                given(oAuth2Service.sendSocialPhoneVerificationCode(anyString(), anyString()))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/auth/social/phone/send-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.data.code").value("123456"))
                        .andExpect(jsonPath("$.data.emailAddress").value("test@seeyouagain.com"));

                verify(oAuth2Service).sendSocialPhoneVerificationCode("01012345678", "temp-uuid-123");
            }

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 전송 - 전화번호 null이면 실패")
            void sendSocialPhoneVerificationCode_ValidationFail_NullPhone() throws Exception {
                // given
                String requestBody = "{\"phone\":null,\"tempUuid\":\"temp-uuid-123\"}";

                // when & then
                mockMvc.perform(post("/auth/social/phone/send-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).sendSocialPhoneVerificationCode(anyString(), anyString());
            }

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 전송 - tempUuid null이면 실패")
            void sendSocialPhoneVerificationCode_ValidationFail_NullTempUuid() throws Exception {
                // given
                String requestBody = "{\"phone\":\"01012345678\",\"tempUuid\":null}";

                // when & then
                mockMvc.perform(post("/auth/social/phone/send-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).sendSocialPhoneVerificationCode(anyString(), anyString());
            }

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 전송 - 잘못된 전화번호 형식이면 실패")
            void sendSocialPhoneVerificationCode_ValidationFail_InvalidPhoneFormat() throws Exception {
                // given
                SocialPhoneVerificationRequest request = new SocialPhoneVerificationRequest(
                        "123",
                        "temp-uuid-123"
                );

                // when & then
                mockMvc.perform(post("/auth/social/phone/send-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).sendSocialPhoneVerificationCode(anyString(), anyString());
            }
        }

        @Nested
        @DisplayName("소셜 휴대폰 인증 코드 검증 API 테스트")
        class VerifySocialPhoneCodeTests {

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 검증 - 로그인 상태 반환 성공")
            void verifySocialPhoneCode_Success_Login() throws Exception {
                // given
                PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");

                SocialLoginResultResponse response = SocialLoginResultResponse.builder()
                        .status("LOGIN")
                        .message("로그인 성공")
                        .loginResponse(LoginResponse.builder()
                                .uuid("test-uuid-123")
                                .role("ROLE_USER")
                                .build())
                        .build();

                given(oAuth2Service.verifySocialPhoneCode(anyString(), any(HttpServletResponse.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/auth/social/phone/verify-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.data.status").value("LOGIN"));

                verify(oAuth2Service).verifySocialPhoneCode(eq("01012345678"), any(HttpServletResponse.class));
            }

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 검증 - 연동 상태 반환 성공")
            void verifySocialPhoneCode_Success_Link() throws Exception {
                // given
                PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");

                SocialLoginResultResponse response = SocialLoginResultResponse.builder()
                        .status("LINK")
                        .message("이미 가입된 계정입니다. 소셜 계정을 연동하시겠습니까?")
                        .loginResponse(null)
                        .build();

                given(oAuth2Service.verifySocialPhoneCode(anyString(), any(HttpServletResponse.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/auth/social/phone/verify-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.data.status").value("LINK"))
                        .andExpect(jsonPath("$.data.message").value("이미 가입된 계정입니다. 소셜 계정을 연동하시겠습니까?"));

                verify(oAuth2Service).verifySocialPhoneCode(eq("01012345678"), any(HttpServletResponse.class));
            }

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 검증 - 회원가입 상태 반환 성공")
            void verifySocialPhoneCode_Success_Signup() throws Exception {
                // given
                PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");

                SocialLoginResultResponse response = SocialLoginResultResponse.builder()
                        .status("SIGNUP")
                        .message("신규 회원가입이 필요합니다. /auth/signup으로 요청하세요.")
                        .loginResponse(null)
                        .build();

                given(oAuth2Service.verifySocialPhoneCode(anyString(), any(HttpServletResponse.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/auth/social/phone/verify-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.data.status").value("SIGNUP"));

                verify(oAuth2Service).verifySocialPhoneCode(eq("01012345678"), any(HttpServletResponse.class));
            }

            @Test
            @DisplayName("소셜 휴대폰 인증 코드 검증 - 전화번호 null이면 실패")
            void verifySocialPhoneCode_ValidationFail_NullPhone() throws Exception {
                // given
                String requestBody = "{\"phone\":null}";

                // when & then
                mockMvc.perform(post("/auth/social/phone/verify-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).verifySocialPhoneCode(anyString(), any(HttpServletResponse.class));
            }
        }

        @Nested
        @DisplayName("소셜 계정 연동 API 테스트")
        class LinkSocialAccountTests {

            @Test
            @DisplayName("소셜 계정 연동 - 성공")
            void linkSocialAccount_Success() throws Exception {
                // given
                SocialLinkRequest request = new SocialLinkRequest("01012345678");

                SocialLoginResultResponse response = SocialLoginResultResponse.builder()
                        .status("LOGIN")
                        .message("로그인 성공")
                        .loginResponse(LoginResponse.builder()
                                .uuid("test-uuid-123")
                                .role("ROLE_USER")
                                .build())
                        .build();

                given(oAuth2Service.linkSocialAccount(anyString(), any(HttpServletResponse.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/auth/social/link")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.data.status").value("LOGIN"));

                verify(oAuth2Service).linkSocialAccount(eq("01012345678"), any(HttpServletResponse.class));
            }

            @Test
            @DisplayName("소셜 계정 연동 - 전화번호 null이면 실패")
            void linkSocialAccount_ValidationFail_NullPhone() throws Exception {
                // given
                String requestBody = "{\"phoneNumber\":null}";

                // when & then
                mockMvc.perform(post("/auth/social/link")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).linkSocialAccount(anyString(), any(HttpServletResponse.class));
            }

            @Test
            @DisplayName("소셜 계정 연동 - 잘못된 전화번호 형식이면 실패")
            void linkSocialAccount_ValidationFail_InvalidPhoneFormat() throws Exception {
                // given
                SocialLinkRequest request = new SocialLinkRequest("123");

                // when & then
                mockMvc.perform(post("/auth/social/link")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isBadRequest());

                verify(oAuth2Service, never()).linkSocialAccount(anyString(), any(HttpServletResponse.class));
            }
        }
    }


    @Nested
    @DisplayName("토큰 재발급 API 테스트")
    class ReissueTokenTests {

        @Test
        @DisplayName("토큰 재발급 - 성공")
        void reissueToken_Success() throws Exception {
            // given
            doNothing().when(authService).reissueToken(
                    any(HttpServletRequest.class),
                    any(HttpServletResponse.class)
            );

            // when & then
            mockMvc.perform(post("/auth/reissue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(authService).reissueToken(
                    any(HttpServletRequest.class),
                    any(HttpServletResponse.class)
            );
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 API 테스트")
    class WithdrawalTests {

        @Test
        @DisplayName("회원 탈퇴 - 성공")
        void withdrawMember_Success() throws Exception {
            // given
            WithdrawalRequest request = new WithdrawalRequest("Password123!", "탈퇴 사유");

            doNothing().when(authService).withdrawMember(anyString(), any(WithdrawalRequest.class));

            // when & then
            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request))
                            .with(mockUser(1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(authService).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }

        @Test
        @DisplayName("회원 탈퇴 - 비밀번호 null이면 실패")
        void withdrawMember_ValidationFail_NullPassword() throws Exception {
            // given
            String requestBody = "{\"password\":null}";

            // when & then
            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(mockUser(1L)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }

        @Test
        @DisplayName("회원 탈퇴 - 비밀번호가 빈 문자열이면 실패")
        void withdrawMember_ValidationFail_BlankPassword() throws Exception {
            // given
            WithdrawalRequest request = new WithdrawalRequest("   ", "탈퇴 사유");

            // when & then
            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request))
                            .with(mockUser(1L)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }

        @Test
        @DisplayName("회원 탈퇴 - 인증 없이 요청하면 실패")
        void withdrawMember_Fail_WithoutAuthentication() throws Exception {
            // given
            WithdrawalRequest request = new WithdrawalRequest("Password123!", "탈퇴 사유");

            // when & then
            mockMvc.perform(delete("/auth/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().is5xxServerError());

            verify(authService, never()).withdrawMember(anyString(), any(WithdrawalRequest.class));
        }
    }
}