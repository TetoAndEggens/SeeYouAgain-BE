package tetoandeggens.seeyouagainbe.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.global.ControllerTest;
import tetoandeggens.seeyouagainbe.member.dto.request.UpdatePushEnabledRequest;
import tetoandeggens.seeyouagainbe.member.service.MemberService;

@WebMvcTest(MemberController.class)
@DisplayName("MemberController 테스트")
class MemberControllerTest extends ControllerTest {

    @MockitoBean
    private MemberService memberService;

    private static final Long TEST_MEMBER_ID = 1L;

    @Nested
    @DisplayName("PUT /member/push - 푸시 알림 토글 업데이트")
    class UpdatePushEnabledTests {

        @Test
        @DisplayName("푸시 알림 활성화 - 성공")
        void updatePushEnabled_EnablePush_Success() throws Exception {
            willDoNothing().given(memberService)
                    .updatePushEnabled(anyLong(), any(UpdatePushEnabledRequest.class));

            String requestBody = """
            {
                "isPushEnabled": true
            }
            """;

            mockMvc.perform(put("/member/push")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(mockUser(TEST_MEMBER_ID)))
                    // HTTP 응답 코드는 200
                    .andExpect(status().isOk())
                    // JSON 내부 status는 204
                    .andExpect(jsonPath("$.status").value(204))
                    .andExpect(jsonPath("$.message").value("NO_CONTENT"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }


        @Test
        @DisplayName("푸시 알림 비활성화 - 성공")
        void updatePushEnabled_DisablePush_Success() throws Exception {
            willDoNothing().given(memberService)
                    .updatePushEnabled(anyLong(), any(UpdatePushEnabledRequest.class));

            String requestBody = """
            {
                "isPushEnabled": false
            }
            """;

            mockMvc.perform(put("/member/push")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(mockUser(TEST_MEMBER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204))
                    .andExpect(jsonPath("$.message").value("NO_CONTENT"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }


        @Test
        @DisplayName("isPushEnabled null - 검증 실패")
        void updatePushEnabled_NullValue_ValidationFailed() throws Exception {
            String requestBody = """
                    {
                        "isPushEnabled": null
                    }
                    """;

            mockMvc.perform(put("/member/push")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(mockUser(TEST_MEMBER_ID)))
                    .andExpect(status().isBadRequest());
        }
    }
}