package tetoandeggens.seeyouagainbe.animal.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkResponse;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.service.BookMarkService;
import tetoandeggens.seeyouagainbe.global.ControllerTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BookMarkErrorCode;

@WebMvcTest(controllers = BookMarkController.class)
@DisplayName("BookMarkController API 테스트")
class BookMarkControllerTest extends ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookMarkService bookMarkService;

    private static final String BASE_URL = "/bookmark";
    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_ANIMAL_ID = 100L;
    private static final Long TEST_BOOKMARK_ID = 10L;

    @Nested
    @DisplayName("GET /bookmark - 내 북마크 목록 조회 API 테스트")
    class GetMyBookMarksTests {

        @Test
        @DisplayName("내 북마크 목록 조회 - 성공")
        void getMyBookMarks_Success() throws Exception {
            // given
            List<BookMarkAnimalResponse> responses = List.of(
                    BookMarkAnimalResponse.builder()
                            .bookMarkId(1L)
                            .animalId(100L)
                            .species(Species.DOG)
                            .breedType("치와와")
                            .processState("보호중")
                            .build(),
                    BookMarkAnimalResponse.builder()
                            .bookMarkId(2L)
                            .animalId(101L)
                            .species(Species.CAT)
                            .breedType("페르시안")
                            .processState("보호중")
                            .build()
            );

            given(bookMarkService.getMyBookMarks(TEST_MEMBER_ID))
                    .willReturn(responses);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].bookMarkId").value(1))
                    .andExpect(jsonPath("$.data[0].animalId").value(100))
                    .andExpect(jsonPath("$.data[0].species").value("DOG"))
                    .andExpect(jsonPath("$.data[0].breedType").value("치와와"))
                    .andExpect(jsonPath("$.data[0].processState").value("보호중"))
                    .andExpect(jsonPath("$.data[1].bookMarkId").value(2))
                    .andExpect(jsonPath("$.data[1].animalId").value(101))
                    .andExpect(jsonPath("$.data[1].species").value("CAT"))
                    .andExpect(jsonPath("$.data[1].breedType").value("페르시안"));

            verify(bookMarkService).getMyBookMarks(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("북마크가 없으면 빈 배열 반환")
        void getMyBookMarks_ReturnsEmptyArray_WhenNoBookmarks() throws Exception {
            // given
            given(bookMarkService.getMyBookMarks(TEST_MEMBER_ID))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(bookMarkService).getMyBookMarks(TEST_MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("POST /bookmark/animals/{animalId} - 북마크 토글 API 테스트")
    class ToggleBookMarkTests {

        @Test
        @DisplayName("북마크 추가 - 성공")
        void toggleBookMark_AddBookmark_Success() throws Exception {
            // given
            BookMarkResponse response = BookMarkResponse.builder()
                    .bookMarkId(TEST_BOOKMARK_ID)
                    .isBookMarked(true)
                    .build();

            given(bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/animals/{animalId}", TEST_ANIMAL_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.bookMarkId").value(TEST_BOOKMARK_ID))
                    .andExpect(jsonPath("$.data.isBookMarked").value(true));

            verify(bookMarkService).toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID);
        }

        @Test
        @DisplayName("북마크 삭제 - 성공 (이미 북마크된 경우)")
        void toggleBookMark_RemoveBookmark_Success() throws Exception {
            // given
            BookMarkResponse response = BookMarkResponse.builder()
                    .bookMarkId(TEST_BOOKMARK_ID)
                    .isBookMarked(false)
                    .build();

            given(bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/animals/{animalId}", TEST_ANIMAL_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.bookMarkId").value(TEST_BOOKMARK_ID))
                    .andExpect(jsonPath("$.data.isBookMarked").value(false));

            verify(bookMarkService).toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID);
        }

        @Test
        @DisplayName("북마크 토글 - 동물이 존재하지 않으면 실패")
        void toggleBookMark_Fail_AnimalNotFound() throws Exception {
            // given
            given(bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .willThrow(new CustomException(AnimalErrorCode.ANIMAL_NOT_FOUND));

            // when & then
            mockMvc.perform(post(BASE_URL + "/animals/{animalId}", TEST_ANIMAL_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(bookMarkService).toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID);
        }

        @Test
        @DisplayName("북마크 토글 - 실종 동물은 북마크할 수 없음")
        void toggleBookMark_Fail_OnlyAbandonedAnimalCanBeBookmarked() throws Exception {
            // given
            given(bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .willThrow(new CustomException(BookMarkErrorCode.ONLY_ABANDONED_ANIMAL_CAN_BE_BOOKMARKED));

            // when & then
            mockMvc.perform(post(BASE_URL + "/animals/{animalId}", TEST_ANIMAL_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(bookMarkService).toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("북마크 추가 후 목록 조회 - 성공 시나리오")
        void addBookmarkAndRetrieveList_Success() throws Exception {
            // given - 북마크 추가
            BookMarkResponse toggleResponse = BookMarkResponse.builder()
                    .bookMarkId(TEST_BOOKMARK_ID)
                    .isBookMarked(true)
                    .build();

            given(bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .willReturn(toggleResponse);

            // when - 북마크 추가
            mockMvc.perform(post(BASE_URL + "/animals/{animalId}", TEST_ANIMAL_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isBookMarked").value(true));

            // given - 목록 조회
            List<BookMarkAnimalResponse> listResponse = List.of(
                    BookMarkAnimalResponse.builder()
                            .bookMarkId(TEST_BOOKMARK_ID)
                            .animalId(TEST_ANIMAL_ID)
                            .species(Species.DOG)
                            .breedType("치와와")
                            .processState("보호중")
                            .build()
            );

            given(bookMarkService.getMyBookMarks(TEST_MEMBER_ID))
                    .willReturn(listResponse);

            // when - 목록 조회
            mockMvc.perform(get(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].bookMarkId").value(TEST_BOOKMARK_ID));

            verify(bookMarkService).toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID);
            verify(bookMarkService).getMyBookMarks(TEST_MEMBER_ID);
        }
    }
}