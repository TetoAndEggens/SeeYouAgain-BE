package tetoandeggens.seeyouagainbe.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import tetoandeggens.seeyouagainbe.auth.dto.request.PhoneVerificationRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("인증/인가 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 전체 플로우 테스트")
    void registerFlow_Success() throws Exception {
        String loginId = "integrationtest";
        String phone = "01099998888";

        // 1. loginId 중복 체크
        mockMvc.perform(get("/auth/check/loginId")
                        .param("loginId", loginId))
                .andExpect(status().isOk());

        // 2. 휴대폰 번호 중복 체크
        PhoneVerificationRequest phoneRequest = new PhoneVerificationRequest(phone);
        mockMvc.perform(post("/auth/check/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phoneRequest)))
                .andExpect(status().isOk());

        // 3. 휴대폰 인증 코드 요청
        mockMvc.perform(post("/auth/phone/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phoneRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").exists())
                .andExpect(jsonPath("$.data.emailAddress").exists());
    }

    @Test
    @DisplayName("토큰 재발급 플로우 테스트")
    void reissueTokenFlow_Test() throws Exception {
        // 토큰 재발급 요청
        mockMvc.perform(post("/auth/reissue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }
}
