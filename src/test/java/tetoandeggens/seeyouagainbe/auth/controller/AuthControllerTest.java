package tetoandeggens.seeyouagainbe.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import tetoandeggens.seeyouagainbe.auth.dto.request.PhoneVerificationRequest;
import tetoandeggens.seeyouagainbe.auth.dto.request.RegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.service.AuthService;
import tetoandeggens.seeyouagainbe.global.config.TestSecurityConfig;

@WebMvcTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Auth 컨트롤러 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("loginId 중복 체크 - 성공")
    void checkLoginId_Success() throws Exception {
        // given
        String loginId = "testuser123";
        doNothing().when(authService).checkLoginIdAvailable(anyString());

        // when & then
        mockMvc.perform(get("/auth/check/loginId")
                        .param("loginId", loginId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));

        verify(authService).checkLoginIdAvailable(loginId);
    }

    @Test
    @DisplayName("loginId 중복 체크 - 파라미터 없으면 실패")
    void checkLoginId_ValidationFail_NoParam() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/check/loginId"))
                .andExpect(status().is5xxServerError());

        verify(authService, never()).checkLoginIdAvailable(anyString());
    }

    @Test
    @DisplayName("loginId 중복 체크 - 빈 문자열이면 실패")
    void checkLoginId_ValidationFail_EmptyString() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/check/loginId")
                        .param("loginId", ""))
                .andExpect(status().is5xxServerError());

        verify(authService, never()).checkLoginIdAvailable(anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 중복 체크 - 성공")
    void checkPhoneNumber_Success() throws Exception {
        // given
        PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");
        doNothing().when(authService).checkPhoneNumberDuplicate(anyString());

        // when & then
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
        // given
        String requestBody = "{\"phone\":null}";

        // when & then
        mockMvc.perform(post("/auth/check/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).checkPhoneNumberDuplicate(anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 중복 체크 - 잘못된 형식이면 실패")
    void checkPhoneNumber_ValidationFail_InvalidFormat() throws Exception {
        // given
        PhoneVerificationRequest request = new PhoneVerificationRequest("123");

        // when & then
        mockMvc.perform(post("/auth/check/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).checkPhoneNumberDuplicate(anyString());
    }

    @Test
    @DisplayName("휴대폰 인증 코드 요청 - 성공")
    void sendPhoneVerificationCode_Success() throws Exception {
        // given
        PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");
        PhoneVerificationResultResponse response = new PhoneVerificationResultResponse(
                "123456",
                "test@seeyouagain.com"
        );

        when(authService.sendPhoneVerificationCode(anyString())).thenReturn(response);

        // when & then
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
        // given
        String requestBody = "{\"phone\":null}";

        // when & then
        mockMvc.perform(post("/auth/phone/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).sendPhoneVerificationCode(anyString());
    }

    @Test
    @DisplayName("휴대폰 인증 코드 요청 - 잘못된 형식이면 실패")
    void sendPhoneVerificationCode_ValidationFail_InvalidFormat() throws Exception {
        // given
        PhoneVerificationRequest request = new PhoneVerificationRequest("invalid-phone");

        // when & then
        mockMvc.perform(post("/auth/phone/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).sendPhoneVerificationCode(anyString());
    }

    @Test
    @DisplayName("휴대폰 인증 코드 검증 - 성공")
    void verifyPhoneCode_Success() throws Exception {
        // given
        PhoneVerificationRequest request = new PhoneVerificationRequest("01012345678");
        doNothing().when(authService).verifyPhoneCode(anyString());

        // when & then
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
        // given
        String requestBody = "{\"phone\":null}";

        // when & then
        mockMvc.perform(post("/auth/phone/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).verifyPhoneCode(anyString());
    }

    @Test
    @DisplayName("휴대폰 인증 코드 검증 - 잘못된 형식이면 실패")
    void verifyPhoneCode_ValidationFail_InvalidFormat() throws Exception {
        // given
        PhoneVerificationRequest request = new PhoneVerificationRequest("123");

        // when & then
        mockMvc.perform(post("/auth/phone/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).verifyPhoneCode(anyString());
    }

    // ============ 회원가입 테스트 ============

    @Test
    @DisplayName("회원가입 - 성공")
    void register_Success() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser123",
                "Password123!",
                "테스트",
                "01012345678"
        );
        doNothing().when(authService).register(any(RegisterRequest.class));

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - loginId가 null이면 실패")
    void register_ValidationFail_LoginIdNull() throws Exception {
        // given
        String requestBody = "{\"loginId\":null,\"password\":\"Password123!\",\"nickName\":\"테스트\",\"phoneNumber\":\"01012345678\"}";

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - loginId가 빈 문자열이면 실패")
    void register_ValidationFail_LoginIdEmpty() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
                "",
                "Password123!",
                "테스트",
                "01012345678"
        );

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - password가 null이면 실패")
    void register_ValidationFail_PasswordNull() throws Exception {
        // given
        String requestBody = "{\"loginId\":\"testuser\",\"password\":null,\"nickName\":\"테스트\",\"phoneNumber\":\"01012345678\"}";

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - password가 빈 문자열이면 실패")
    void register_ValidationFail_PasswordEmpty() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser",
                "",
                "테스트",
                "01012345678"
        );

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - nickName이 null이면 실패")
    void register_ValidationFail_NickNameNull() throws Exception {
        // given
        String requestBody = "{\"loginId\":\"testuser\",\"password\":\"Password123!\",\"nickName\":null,\"phoneNumber\":\"01012345678\"}";

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - nickName이 빈 문자열이면 실패")
    void register_ValidationFail_NickNameEmpty() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser",
                "Password123!",
                "",
                "01012345678"
        );

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - phoneNumber가 null이면 실패")
    void register_ValidationFail_PhoneNumberNull() throws Exception {
        // given
        String requestBody = "{\"loginId\":\"testuser\",\"password\":\"Password123!\",\"nickName\":\"테스트\",\"phoneNumber\":null}";

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - phoneNumber가 빈 문자열이면 실패")
    void register_ValidationFail_PhoneNumberEmpty() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser",
                "Password123!",
                "테스트",
                ""
        );

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 - phoneNumber가 잘못된 형식이면 실패")
    void register_ValidationFail_PhoneNumberInvalidFormat() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
                "testuser",
                "Password123!",
                "테스트",
                "123456"
        );

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("토큰 재발급 - 성공")
    void reissueToken_Success() throws Exception {
        // given
        ReissueTokenResponse response = ReissueTokenResponse.builder()
                .accessToken("new_access_token")
                .build();

        when(authService.reissueToken(any(), any())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/auth/reissue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));

        verify(authService).reissueToken(any(), any());
    }

    @Test
    @DisplayName("토큰 재발급 - RefreshToken이 없으면 실패 (서비스에서 예외 발생)")
    void reissueToken_Fail_NoRefreshToken() throws Exception {
        // given
        when(authService.reissueToken(any(), any()))
                .thenThrow(new IllegalArgumentException("Refresh Token not found"));

        // when & then
        mockMvc.perform(post("/auth/reissue"))
                .andExpect(status().is5xxServerError());

        verify(authService).reissueToken(any(), any());
    }
}