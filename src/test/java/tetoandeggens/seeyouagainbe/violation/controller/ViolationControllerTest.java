package tetoandeggens.seeyouagainbe.violation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.global.ControllerTest;
import tetoandeggens.seeyouagainbe.violation.dto.request.ViolationCreateRequest;
import tetoandeggens.seeyouagainbe.violation.service.ViolationService;

@WebMvcTest(controllers = ViolationController.class)
@DisplayName("Violation 컨트롤러 테스트")
class ViolationControllerTest extends ControllerTest {

    @MockitoBean
    private ViolationService violationService;

    private RequestPostProcessor mockUserWithUuid(String uuid) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUuid()).thenReturn(uuid);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of()
        );

        return authentication(auth);
    }

    @Nested
    @DisplayName("신고 등록 API 테스트")
    class CreateViolationTests {

        private final String TEST_UUID = UUID.randomUUID().toString();

        @Test
        @DisplayName("게시글 신고 - 성공")
        void createViolation_Board_Success() throws Exception {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    1L,
                    null,
                    ReportReason.SPAM,
                    "스팸 게시글입니다."
            );

            doNothing().when(violationService).createViolation(any(UUID.class), any(ViolationCreateRequest.class));

            // when & then
            mockMvc.perform(post("/violation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request))
                            .with(mockUserWithUuid(TEST_UUID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(violationService).createViolation(any(UUID.class), any(ViolationCreateRequest.class));
        }

        @Test
        @DisplayName("채팅방 신고 - 성공")
        void createViolation_ChatRoom_Success() throws Exception {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    null,
                    1L,
                    ReportReason.ABUSE,
                    "욕설을 사용했습니다."
            );

            doNothing().when(violationService).createViolation(any(UUID.class), any(ViolationCreateRequest.class));

            // when & then
            mockMvc.perform(post("/violation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request))
                            .with(mockUserWithUuid(TEST_UUID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(violationService).createViolation(any(UUID.class), any(ViolationCreateRequest.class));
        }

        @Test
        @DisplayName("신고 등록 - reason null이면 실패")
        void createViolation_ValidationFail_NullReason() throws Exception {
            // given
            String requestBody = "{\"boardId\":1,\"chatRoomId\":null,\"reason\":null,\"detailReason\":\"테스트\"}";

            // when & then
            mockMvc.perform(post("/violation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(mockUserWithUuid(TEST_UUID)))
                    .andExpect(status().isBadRequest());

            verify(violationService, never()).createViolation(any(UUID.class), any(ViolationCreateRequest.class));
        }

        @Test
        @DisplayName("신고 등록 - detailReason은 null 허용")
        void createViolation_Success_NullDetailReason() throws Exception {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    1L,
                    null,
                    ReportReason.ETC,
                    null
            );

            doNothing().when(violationService).createViolation(any(UUID.class), any(ViolationCreateRequest.class));

            // when & then
            mockMvc.perform(post("/violation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request))
                            .with(mockUserWithUuid(TEST_UUID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(violationService).createViolation(any(UUID.class), any(ViolationCreateRequest.class));
        }
    }
}