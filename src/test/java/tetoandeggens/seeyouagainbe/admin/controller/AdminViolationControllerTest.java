package tetoandeggens.seeyouagainbe.admin.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import tetoandeggens.seeyouagainbe.admin.dto.request.ViolationProcessRequest;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationDetailResponse;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationListResponse;
import tetoandeggens.seeyouagainbe.admin.service.AdminViolationService;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AdminErrorCode;
import tetoandeggens.seeyouagainbe.global.response.PageResponse;

@WebMvcTest(AdminViolationController.class)
@DisplayName("AdminViolationController 통합 테스트")
@WithMockUser(roles = "ADMIN")
class AdminViolationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminViolationService adminViolationService;

    @Nested
    @DisplayName("신고 목록 조회 API")
    class GetViolationListTests {

        @Test
        @DisplayName("신고 목록 조회 - 성공 (필터 없음)")
        void getViolationList_Success_WithoutFilter() throws Exception {
            // given
            ViolationListResponse response1 = new ViolationListResponse(
                    1L, ViolatedStatus.WAITING, ReportReason.SPAM,
                    "신고자1", "피신고자1", "BOARD", 1L, LocalDateTime.now()
            );

            ViolationListResponse response2 = new ViolationListResponse(
                    2L, ViolatedStatus.VIOLATED, ReportReason.ABUSE,
                    "신고자2", "피신고자2", "CHAT_ROOM", 2L, LocalDateTime.now()
            );

            Page<ViolationListResponse> page = new PageImpl<>(
                    List.of(response1, response2),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    2
            );

            // PageResponse를 직접 생성
            PageResponse<ViolationListResponse> expectedResponse = PageResponse.of(page);

            given(adminViolationService.getViolationList(isNull(), any(Pageable.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/admin/violations")
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].violationId").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalPages").value(1));

            verify(adminViolationService).getViolationList(isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("신고 목록 조회 - 성공 (WAITING 상태 필터)")
        void getViolationList_Success_WithWaitingFilter() throws Exception {
            // given
            ViolationListResponse response = new ViolationListResponse(
                    1L, ViolatedStatus.WAITING, ReportReason.SPAM,
                    "신고자1", "피신고자1", "BOARD", 1L, LocalDateTime.now()
            );

            Page<ViolationListResponse> page = new PageImpl<>(
                    List.of(response),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1
            );

            PageResponse<ViolationListResponse> expectedResponse = PageResponse.of(page);

            given(adminViolationService.getViolationList(eq(ViolatedStatus.WAITING), any(Pageable.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/admin/violations")
                            .param("status", "WAITING")
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].violatedStatus").value("WAITING"));

            verify(adminViolationService).getViolationList(eq(ViolatedStatus.WAITING), any(Pageable.class));
        }

        @Test
        @DisplayName("신고 목록 조회 - 성공 (VIOLATED 상태 필터)")
        void getViolationList_Success_WithViolatedFilter() throws Exception {
            // given
            ViolationListResponse response = new ViolationListResponse(
                    1L, ViolatedStatus.VIOLATED, ReportReason.SPAM,
                    "신고자1", "피신고자1", "BOARD", 1L, LocalDateTime.now()
            );

            Page<ViolationListResponse> page = new PageImpl<>(
                    List.of(response),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1
            );

            PageResponse<ViolationListResponse> expectedResponse = PageResponse.of(page);

            given(adminViolationService.getViolationList(eq(ViolatedStatus.VIOLATED), any(Pageable.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/admin/violations")
                            .param("status", "VIOLATED")
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].violatedStatus").value("VIOLATED"));

            verify(adminViolationService).getViolationList(eq(ViolatedStatus.VIOLATED), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("신고 상세 조회 API")
    class GetViolationDetailTests {

        @Test
        @DisplayName("신고 상세 조회 - 성공")
        void getViolationDetail_Success() throws Exception {
            // given
            Long violationId = 1L;
            ViolationDetailResponse response = new ViolationDetailResponse(
                    violationId, ViolatedStatus.WAITING, ReportReason.SPAM,
                    "스팸 게시글입니다.", null, null, null, null, LocalDateTime.now()
            );

            given(adminViolationService.getViolationDetail(violationId))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/admin/violations/{violationId}", violationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.violationId").value(violationId))
                    .andExpect(jsonPath("$.data.reason").value("SPAM"))
                    .andExpect(jsonPath("$.data.detailReason").value("스팸 게시글입니다."));

            verify(adminViolationService).getViolationDetail(violationId);
        }

        @Test
        @DisplayName("신고 상세 조회 - 실패 (신고 내역을 찾을 수 없음)")
        void getViolationDetail_Fail_ViolationNotFound() throws Exception {
            // given
            Long violationId = 999L;
            given(adminViolationService.getViolationDetail(violationId))
                    .willThrow(new CustomException(AdminErrorCode.VIOLATION_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/admin/violations/{violationId}", violationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ADMIN_001"));

            verify(adminViolationService).getViolationDetail(violationId);
        }
    }

    @Nested
    @DisplayName("신고 처리 API")
    class ProcessViolationTests {

        @Test
        @DisplayName("신고 처리 - 성공 (위반 처리 + 삭제)")
        void processViolation_Success_ViolatedWithDelete() throws Exception {
            // given
            Long violationId = 1L;
            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED, true
            );

            willDoNothing().given(adminViolationService)
                    .processViolation(violationId, request);

            // when & then
            mockMvc.perform(patch("/admin/violations/{violationId}/process", violationId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204))
                    .andExpect(jsonPath("$.message").value("NO_CONTENT"));

            verify(adminViolationService).processViolation(violationId, request);
        }

        @Test
        @DisplayName("신고 처리 - 성공 (위반 처리 + 삭제 안함)")
        void processViolation_Success_ViolatedWithoutDelete() throws Exception {
            // given
            Long violationId = 1L;
            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED, false
            );

            willDoNothing().given(adminViolationService)
                    .processViolation(violationId, request);

            // when & then
            mockMvc.perform(patch("/admin/violations/{violationId}/process", violationId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(adminViolationService).processViolation(violationId, request);
        }

        @Test
        @DisplayName("신고 처리 - 성공 (위반 아님 처리)")
        void processViolation_Success_Normal() throws Exception {
            // given
            Long violationId = 1L;
            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.NORMAL, false
            );

            willDoNothing().given(adminViolationService)
                    .processViolation(violationId, request);

            // when & then
            mockMvc.perform(patch("/admin/violations/{violationId}/process", violationId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(adminViolationService).processViolation(violationId, request);
        }

        @Test
        @DisplayName("신고 처리 - 성공 (deleteContent가 null)")
        void processViolation_Success_DeleteContentNull() throws Exception {
            // given
            Long violationId = 1L;
            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED, null
            );

            willDoNothing().given(adminViolationService)
                    .processViolation(violationId, request);

            // when & then
            mockMvc.perform(patch("/admin/violations/{violationId}/process", violationId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(adminViolationService).processViolation(violationId, request);
        }

        @Test
        @DisplayName("신고 처리 - 실패 (violatedStatus가 null)")
        void processViolation_Fail_ViolatedStatusNull() throws Exception {
            // given
            String requestJson = """
                    {
                        "violatedStatus": null,
                        "deleteContent": true
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/admin/violations/1/process")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(adminViolationService, never()).processViolation(anyLong(), any());
        }

        @Test
        @DisplayName("신고 처리 - 실패 (신고 내역을 찾을 수 없음)")
        void processViolation_Fail_ViolationNotFound() throws Exception {
            // given
            Long violationId = 999L;
            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED, true
            );

            willThrow(new CustomException(AdminErrorCode.VIOLATION_NOT_FOUND))
                    .given(adminViolationService)
                    .processViolation(violationId, request);

            // when & then
            mockMvc.perform(patch("/admin/violations/{violationId}/process", violationId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ADMIN_001"));

            verify(adminViolationService).processViolation(violationId, request);
        }
    }
}