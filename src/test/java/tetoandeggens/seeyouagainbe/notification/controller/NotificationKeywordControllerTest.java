package tetoandeggens.seeyouagainbe.notification.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.global.ControllerTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.FcmErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.NotificationKeywordErrorCode;
import tetoandeggens.seeyouagainbe.notification.dto.request.BulkUpdateKeywordsRequest;
import tetoandeggens.seeyouagainbe.notification.dto.request.NotificationKeywordRequest;
import tetoandeggens.seeyouagainbe.notification.dto.response.BulkUpdateKeywordsResponse;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;
import tetoandeggens.seeyouagainbe.notification.service.NotificationKeywordService;

@WebMvcTest(NotificationKeywordController.class)
@DisplayName("NotificationKeywordController 단위 테스트")
class NotificationKeywordControllerTest extends ControllerTest {

    private static final String BASE_URL = "/notification/keyword";
    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_KEYWORD = "골든리트리버";
    private static final Long TEST_KEYWORD_ID = 1L;

    @MockitoBean
    private NotificationKeywordService notificationKeywordService;

    @Nested
    @DisplayName("키워드 구독 API 테스트")
    class SubscribeKeywordTests {

        @Test
        @DisplayName("키워드 구독 - 성공")
        void subscribeKeyword_Success() throws Exception {
            // given
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );
            NotificationKeywordResponse response = new NotificationKeywordResponse(
                    TEST_KEYWORD_ID,
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(notificationKeywordService.subscribe(anyLong(), any(NotificationKeywordRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.id").value(TEST_KEYWORD_ID))
                    .andExpect(jsonPath("$.data.keyword").value(TEST_KEYWORD))
                    .andExpect(jsonPath("$.data.keywordType").value("ABANDONED"))
                    .andExpect(jsonPath("$.data.keywordCategoryType").value("BREED"));

            verify(notificationKeywordService).subscribe(anyLong(), any(NotificationKeywordRequest.class));
        }

        @Test
        @DisplayName("키워드 구독 - keyword가 null이면 실패")
        void subscribeKeyword_ValidationFail_NullKeyword() throws Exception {
            // given
            String requestBody = "{\"keyword\":null,\"keywordType\":\"ABANDONED\"," +
                    "\"keywordCategoryType\":\"BREED\"}";

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(notificationKeywordService, never()).subscribe(anyLong(), any());
        }

        @Test
        @DisplayName("키워드 구독 - keywordType이 null이면 실패")
        void subscribeKeyword_ValidationFail_NullKeywordType() throws Exception {
            // given
            String requestBody = "{\"keyword\":\"" + TEST_KEYWORD + "\",\"keywordType\":null," +
                    "\"keywordCategoryType\":\"BREED\"}";

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(notificationKeywordService, never()).subscribe(anyLong(), any());
        }

        @Test
        @DisplayName("키워드 구독 - keywordCategoryType이 null이면 실패")
        void subscribeKeyword_ValidationFail_NullKeywordCategoryType() throws Exception {
            // given
            String requestBody = "{\"keyword\":\"" + TEST_KEYWORD + "\"," +
                    "\"keywordType\":\"ABANDONED\",\"keywordCategoryType\":null}";

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(notificationKeywordService, never()).subscribe(anyLong(), any());
        }

        @Test
        @DisplayName("키워드 구독 - keyword가 빈 문자열이면 실패")
        void subscribeKeyword_ValidationFail_BlankKeyword() throws Exception {
            // given
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    "",
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(notificationKeywordService, never()).subscribe(anyLong(), any());
        }

        @Test
        @DisplayName("키워드 구독 - 이미 구독 중이면 실패")
        void subscribeKeyword_Fail_AlreadySubscribed() throws Exception {
            // given
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(notificationKeywordService.subscribe(anyLong(), any(NotificationKeywordRequest.class)))
                    .willThrow(new CustomException(NotificationKeywordErrorCode.KEYWORD_ALREADY_SUBSCRIBED));

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isConflict());

            verify(notificationKeywordService).subscribe(anyLong(), any(NotificationKeywordRequest.class));
        }
    }

    @Nested
    @DisplayName("키워드 구독 해제 API 테스트")
    class UnsubscribeKeywordTests {

        @Test
        @DisplayName("키워드 구독 해제 - 성공")
        void unsubscribeKeyword_Success() throws Exception {
            // given
            doNothing().when(notificationKeywordService).unsubscribe(anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + TEST_KEYWORD_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(notificationKeywordService).unsubscribe(anyLong(), eq(TEST_KEYWORD_ID));
        }

        @Test
        @DisplayName("키워드 구독 해제 - 키워드가 존재하지 않으면 실패")
        void unsubscribeKeyword_Fail_KeywordNotFound() throws Exception {
            // given
            doThrow(new CustomException(NotificationKeywordErrorCode.KEYWORD_NOT_FOUND))
                    .when(notificationKeywordService).unsubscribe(anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + TEST_KEYWORD_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(notificationKeywordService).unsubscribe(anyLong(), eq(TEST_KEYWORD_ID));
        }
    }

    @Nested
    @DisplayName("키워드 목록 조회 API 테스트")
    class GetSubscribedKeywordsTests {

        @Test
        @DisplayName("키워드 목록 조회 - 성공")
        void getSubscribedKeywords_Success() throws Exception {
            // given
            List<NotificationKeywordResponse> responses = List.of(
                    new NotificationKeywordResponse(1L, TEST_KEYWORD,
                            KeywordType.ABANDONED, KeywordCategoryType.BREED),
                    new NotificationKeywordResponse(2L, "서울",
                            KeywordType.WITNESS, KeywordCategoryType.LOCATION)
            );

            given(notificationKeywordService.getSubscribedKeywords(anyLong()))
                    .willReturn(responses);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].keyword").value(TEST_KEYWORD))
                    .andExpect(jsonPath("$.data[0].keywordCategoryType").value("BREED"))
                    .andExpect(jsonPath("$.data[1].keyword").value("서울"))
                    .andExpect(jsonPath("$.data[1].keywordCategoryType").value("LOCATION"));

            verify(notificationKeywordService).getSubscribedKeywords(anyLong());
        }

        @Test
        @DisplayName("키워드 목록 조회 - 빈 리스트 반환")
        void getSubscribedKeywords_ReturnsEmptyList() throws Exception {
            // given
            given(notificationKeywordService.getSubscribedKeywords(anyLong()))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(notificationKeywordService).getSubscribedKeywords(anyLong());
        }
    }

    @Nested
    @DisplayName("키워드 일괄 업데이트 API 테스트")
    class BulkUpdateKeywordsTests {

        @Test
        @DisplayName("키워드 일괄 업데이트 - 성공 (추가와 삭제 모두)")
        void bulkUpdateKeywords_Success() throws Exception {
            // given
            List<NotificationKeywordRequest> keywordsToAdd = List.of(
                    new NotificationKeywordRequest(TEST_KEYWORD, KeywordType.ABANDONED,
                            KeywordCategoryType.BREED)
            );
            List<Long> keywordIdsToDelete = List.of(1L);

            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    keywordsToAdd,
                    keywordIdsToDelete
            );

            List<NotificationKeywordResponse> addedKeywords = List.of(
                    new NotificationKeywordResponse(2L, TEST_KEYWORD,
                            KeywordType.ABANDONED, KeywordCategoryType.BREED)
            );

            BulkUpdateKeywordsResponse response = BulkUpdateKeywordsResponse.of(
                    addedKeywords,
                    keywordIdsToDelete
            );

            given(notificationKeywordService.bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/updateAll")
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.addedKeywords").isArray())
                    .andExpect(jsonPath("$.data.addedKeywords.length()").value(1))
                    .andExpect(jsonPath("$.data.deletedKeywordIds").isArray())
                    .andExpect(jsonPath("$.data.deletedKeywordIds.length()").value(1));

            verify(notificationKeywordService).bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class));
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 추가만 있는 경우 성공")
        void bulkUpdateKeywords_Success_OnlyAdd() throws Exception {
            // given
            List<NotificationKeywordRequest> keywordsToAdd = List.of(
                    new NotificationKeywordRequest(TEST_KEYWORD, KeywordType.ABANDONED,
                            KeywordCategoryType.BREED)
            );

            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    keywordsToAdd,
                    List.of()
            );

            List<NotificationKeywordResponse> addedKeywords = List.of(
                    new NotificationKeywordResponse(1L, TEST_KEYWORD,
                            KeywordType.ABANDONED, KeywordCategoryType.BREED)
            );

            BulkUpdateKeywordsResponse response = BulkUpdateKeywordsResponse.of(
                    addedKeywords,
                    List.of()
            );

            given(notificationKeywordService.bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/updateAll")
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.addedKeywords.length()").value(1))
                    .andExpect(jsonPath("$.data.deletedKeywordIds").isEmpty());

            verify(notificationKeywordService).bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class));
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 삭제만 있는 경우 성공")
        void bulkUpdateKeywords_Success_OnlyDelete() throws Exception {
            // given
            List<Long> keywordIdsToDelete = List.of(1L, 2L);

            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    List.of(),
                    keywordIdsToDelete
            );

            BulkUpdateKeywordsResponse response = BulkUpdateKeywordsResponse.of(
                    List.of(),
                    keywordIdsToDelete
            );

            given(notificationKeywordService.bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/updateAll")
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.addedKeywords").isEmpty())
                    .andExpect(jsonPath("$.data.deletedKeywordIds.length()").value(2));

            verify(notificationKeywordService).bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class));
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 빈 요청이면 빈 응답 반환")
        void bulkUpdateKeywords_ReturnsEmpty_WhenEmptyRequest() throws Exception {
            // given
            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    List.of(),
                    List.of()
            );

            BulkUpdateKeywordsResponse response = BulkUpdateKeywordsResponse.of(
                    List.of(),
                    List.of()
            );

            given(notificationKeywordService.bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/updateAll")
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.addedKeywords").isEmpty())
                    .andExpect(jsonPath("$.data.deletedKeywordIds").isEmpty());

            verify(notificationKeywordService).bulkUpdateKeywords(anyLong(),
                    any(BulkUpdateKeywordsRequest.class));
        }
    }
}