package tetoandeggens.seeyouagainbe.board.controller;

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

import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.board.dto.request.UpdatingBoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.request.WritingBoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardDetailResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardListResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.PresignedUrlResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.ProfileInfo;
import tetoandeggens.seeyouagainbe.board.dto.response.TagInfo;
import tetoandeggens.seeyouagainbe.board.service.BoardService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.ControllerTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BoardErrorCode;

@WebMvcTest(controllers = BoardController.class)
@DisplayName("Board 컨트롤러 테스트")
class BoardControllerTest extends ControllerTest {

	@MockitoBean
	private BoardService boardService;

	@Nested
	@DisplayName("게시글 작성 API 테스트")
	class WriteAnimalBoardTests {

		@Test
		@DisplayName("게시글 작성 - 성공")
		void writeAnimalBoard_Success() throws Exception {
			// given
			WritingBoardRequest request = new WritingBoardRequest(
				"실종 강아지를 찾습니다",
				"강남역 근처에서 실종되었습니다",
				"DOG",
				"치와와",
				"M",
				"갈색",
				"서울특별시 강남구 테헤란로",
				37.4979,
				127.0276,
				"MISSING",
				2,
				List.of("강아지", "실종")
			);

			PresignedUrlResponse response = new PresignedUrlResponse(
				List.of("https://s3.amazonaws.com/presigned-url-1", "https://s3.amazonaws.com/presigned-url-2")
			);

			given(boardService.writeAnimalBoard(any(WritingBoardRequest.class), anyLong()))
				.willReturn(response);

			// when & then
			mockMvc.perform(post("/board")
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJsonString(request))
					.with(mockUser(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.presignedUrls").isArray())
				.andExpect(jsonPath("$.data.presignedUrls.length()").value(2));

			verify(boardService).writeAnimalBoard(any(WritingBoardRequest.class), eq(1L));
		}

		@Test
		@DisplayName("게시글 작성 - 제목 없이 요청시 실패")
		void writeAnimalBoard_Fail_WithoutTitle() throws Exception {
			// given
			WritingBoardRequest request = new WritingBoardRequest(
				"",
				"내용",
				"DOG",
				"치와와",
				"M",
				"갈색",
				"서울특별시 강남구",
				37.4979,
				127.0276,
				"MISSING",
				2,
				List.of("강아지")
			);

			// when & then
			mockMvc.perform(post("/board")
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJsonString(request))
					.with(mockUser(1L)))
				.andExpect(status().isBadRequest());

			verify(boardService, never()).writeAnimalBoard(any(), anyLong());
		}

		@Test
		@DisplayName("게시글 작성 - 내용 없이 요청시 실패")
		void writeAnimalBoard_Fail_WithoutContent() throws Exception {
			// given
			WritingBoardRequest request = new WritingBoardRequest(
				"제목",
				"",
				"DOG",
				"치와와",
				"M",
				"갈색",
				"서울특별시 강남구",
				37.4979,
				127.0276,
				"MISSING",
				2,
				List.of("강아지")
			);

			// when & then
			mockMvc.perform(post("/board")
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJsonString(request))
					.with(mockUser(1L)))
				.andExpect(status().isBadRequest());

			verify(boardService, never()).writeAnimalBoard(any(), anyLong());
		}

		@Test
		@DisplayName("게시글 작성 - 동물 타입 없이 요청시 실패")
		void writeAnimalBoard_Fail_WithoutAnimalType() throws Exception {
			// given
			WritingBoardRequest request = new WritingBoardRequest(
				"제목",
				"내용",
				"DOG",
				"치와와",
				"M",
				"갈색",
				"서울특별시 강남구",
				37.4979,
				127.0276,
				"",
				2,
				List.of("강아지")
			);

			// when & then
			mockMvc.perform(post("/board")
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJsonString(request))
					.with(mockUser(1L)))
				.andExpect(status().isBadRequest());

			verify(boardService, never()).writeAnimalBoard(any(), anyLong());
		}
	}

	@Nested
	@DisplayName("게시글 리스트 조회 API 테스트")
	class GetAnimalBoardListTests {

		@Test
		@DisplayName("게시글 리스트 조회 - 필수 파라미터만으로 성공")
		void getBoardList_Success_WithRequiredParamsOnly() throws Exception {
			// given
			BoardResponse boardResponse = BoardResponse.builder()
				.boardId(1L)
				.title("실종 강아지를 찾습니다")
				.species(Species.DOG)
				.breedType("치와와")
				.sex(Sex.M)
				.address("서울특별시 강남구")
				.latitude(37.4979)
				.longitude(127.0276)
				.animalType(AnimalType.MISSING)
				.memberNickname("홍길동")
				.profile("https://profile.com/image.jpg")
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.tags(List.of("강아지", "실종"))
				.build();

			CursorPage<BoardResponse, Long> cursorPage = CursorPage.of(
				List.of(boardResponse),
				10,
				BoardResponse::boardId
			);

			BoardListResponse response = BoardListResponse.of(1, cursorPage);

			given(boardService.getAnimalBoardList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				isNull(),
				any()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/board/list")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.boardCount").value(1))
				.andExpect(jsonPath("$.data.board.data[0].boardId").value(1))
				.andExpect(jsonPath("$.data.board.data[0].title").value("실종 강아지를 찾습니다"));

			verify(boardService).getAnimalBoardList(
				any(CursorPageRequest.class),
				eq(SortDirection.LATEST),
				isNull(),
				any()
			);
		}

		@Test
		@DisplayName("게시글 리스트 조회 - 타입 필터와 함께 성공")
		void getBoardList_Success_WithTypeFilter() throws Exception {
			// given
			BoardListResponse response = BoardListResponse.of(
				1,
				CursorPage.of(List.of(), 10, BoardResponse::boardId)
			);

			given(boardService.getAnimalBoardList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				eq("MISSING"),
				any()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/board/list/MISSING")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(boardService).getAnimalBoardList(
				any(CursorPageRequest.class),
				eq(SortDirection.LATEST),
				eq("MISSING"),
				any()
			);
		}

		@Test
		@DisplayName("게시글 리스트 조회 - OLDEST 정렬로 성공")
		void getBoardList_Success_WithOldestSort() throws Exception {
			// given
			BoardListResponse response = BoardListResponse.of(
				0,
				CursorPage.of(List.of(), 10, BoardResponse::boardId)
			);

			given(boardService.getAnimalBoardList(
				any(CursorPageRequest.class),
				eq(SortDirection.OLDEST),
				isNull(),
				any()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/board/list")
					.param("size", "10")
					.param("sortDirection", "OLDEST"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(boardService).getAnimalBoardList(
				any(CursorPageRequest.class),
				eq(SortDirection.OLDEST),
				isNull(),
				any()
			);
		}

		@Test
		@DisplayName("게시글 리스트 조회 - 커서 ID와 함께 성공")
		void getBoardList_Success_WithCursorId() throws Exception {
			// given
			BoardListResponse response = BoardListResponse.of(
				0,
				CursorPage.of(List.of(), 10, BoardResponse::boardId)
			);

			given(boardService.getAnimalBoardList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				isNull(),
				any()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/board/list")
					.param("cursorId", "10")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(boardService).getAnimalBoardList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				isNull(),
				any()
			);
		}
	}

	@Nested
	@DisplayName("게시글 상세 조회 API 테스트")
	class GetAnimalBoardTests {

		@Test
		@DisplayName("게시글 상세 조회 - 성공")
		void getBoard_Success() throws Exception {
			// given
			Long boardId = 1L;
			BoardDetailResponse response = new BoardDetailResponse(
				boardId,
				"실종 강아지를 찾습니다",
				"강남역 근처에서 실종되었습니다",
				Species.DOG,
				"치와와",
				Sex.M,
				"갈색",
				"서울특별시 강남구",
				37.4979,
				127.0276,
				AnimalType.MISSING,
				"홍길동",
				LocalDateTime.now(),
				LocalDateTime.now(),
				List.of(
					new TagInfo(1L, "강아지"),
					new TagInfo(2L, "실종")
				),
				List.of(
					new ProfileInfo(1L, "profile1.jpg"),
					new ProfileInfo(2L, "profile2.jpg")
				),
				true
			);

			given(boardService.getAnimalBoard(eq(boardId), any()))
				.willReturn(response);

			// when & then
			mockMvc.perform(get("/board/{boardId}", boardId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.boardId").value(boardId))
				.andExpect(jsonPath("$.data.title").value("실종 강아지를 찾습니다"))
				.andExpect(jsonPath("$.data.content").value("강남역 근처에서 실종되었습니다"))
				.andExpect(jsonPath("$.data.tags").isArray())
				.andExpect(jsonPath("$.data.tags.length()").value(2))
				.andExpect(jsonPath("$.data.profiles").isArray())
				.andExpect(jsonPath("$.data.profiles.length()").value(2));

			verify(boardService).getAnimalBoard(eq(boardId), any());
		}

		@Test
		@DisplayName("게시글 상세 조회 - 존재하지 않는 ID로 조회시 예외 발생")
		void getBoard_Fail_WhenNotExists() throws Exception {
			// given
			Long boardId = 999L;

			given(boardService.getAnimalBoard(eq(boardId), any()))
				.willThrow(new CustomException(BoardErrorCode.BOARD_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/board/{boardId}", boardId))
				.andExpect(status().isNotFound());

			verify(boardService).getAnimalBoard(eq(boardId), any());
		}
	}

	@Nested
	@DisplayName("게시글 삭제 API 테스트")
	class DeleteAnimalBoardTests {

		@Test
		@DisplayName("게시글 삭제 - 성공")
		void deleteBoard_Success() throws Exception {
			// given
			Long boardId = 1L;
			Long memberId = 1L;

			willDoNothing().given(boardService).deleteAnimalBoard(boardId, memberId);

			// when & then
			mockMvc.perform(delete("/board/{boardId}", boardId)
					.with(mockUser(memberId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(204));

			verify(boardService).deleteAnimalBoard(boardId, memberId);
		}

		@Test
		@DisplayName("게시글 삭제 - 존재하지 않는 게시글 삭제시 예외 발생")
		void deleteBoard_Fail_WhenNotExists() throws Exception {
			// given
			Long boardId = 999L;
			Long memberId = 1L;

			willThrow(new CustomException(BoardErrorCode.BOARD_NOT_FOUND))
				.given(boardService).deleteAnimalBoard(boardId, memberId);

			// when & then
			mockMvc.perform(delete("/board/{boardId}", boardId)
					.with(mockUser(memberId)))
				.andExpect(status().isNotFound());

			verify(boardService).deleteAnimalBoard(boardId, memberId);
		}

		@Test
		@DisplayName("게시글 삭제 - 권한 없는 사용자가 삭제 요청시 예외 발생")
		void deleteBoard_Fail_WhenForbidden() throws Exception {
			// given
			Long boardId = 1L;
			Long memberId = 2L;

			willThrow(new CustomException(BoardErrorCode.BOARD_FORBIDDEN))
				.given(boardService).deleteAnimalBoard(boardId, memberId);

			// when & then
			mockMvc.perform(delete("/board/{boardId}", boardId)
					.with(mockUser(memberId)))
				.andExpect(status().isForbidden());

			verify(boardService).deleteAnimalBoard(boardId, memberId);
		}
	}

	@Nested
	@DisplayName("게시글 수정 API 테스트")
	class UpdateAnimalBoardTests {

		@Test
		@DisplayName("게시글 수정 - 성공")
		void updateBoard_Success() throws Exception {
			// given
			Long boardId = 1L;
			Long memberId = 1L;

			UpdatingBoardRequest request = new UpdatingBoardRequest(
				"수정된 제목",
				"수정된 내용",
				"CAT",
				"코리안 숏헤어",
				"F",
				"흰색",
				"서울특별시 서초구",
				37.4833,
				127.0322,
				"WITNESS",
				1,
				List.of("고양이", "목격"),
				List.of(1L),
				List.of(2L)
			);

			PresignedUrlResponse response = new PresignedUrlResponse(
				List.of("https://s3.amazonaws.com/presigned-url-1")
			);

			given(boardService.updateAnimalBoard(eq(boardId), any(UpdatingBoardRequest.class), eq(memberId)))
				.willReturn(response);

			// when & then
			mockMvc.perform(put("/board/{boardId}", boardId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJsonString(request))
					.with(mockUser(memberId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.presignedUrls").isArray())
				.andExpect(jsonPath("$.data.presignedUrls.length()").value(1));

			verify(boardService).updateAnimalBoard(eq(boardId), any(UpdatingBoardRequest.class), eq(memberId));
		}

		@Test
		@DisplayName("게시글 수정 - 존재하지 않는 게시글 수정시 예외 발생")
		void updateBoard_Fail_WhenNotExists() throws Exception {
			// given
			Long boardId = 999L;
			Long memberId = 1L;

			UpdatingBoardRequest request = new UpdatingBoardRequest(
				"수정된 제목",
				"수정된 내용",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"MISSING",
				null,
				null,
				null,
				null
			);

			given(boardService.updateAnimalBoard(eq(boardId), any(UpdatingBoardRequest.class), eq(memberId)))
				.willThrow(new CustomException(BoardErrorCode.BOARD_NOT_FOUND));

			// when & then
			mockMvc.perform(put("/board/{boardId}", boardId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJsonString(request))
					.with(mockUser(memberId)))
				.andExpect(status().isNotFound());

			verify(boardService).updateAnimalBoard(eq(boardId), any(UpdatingBoardRequest.class), eq(memberId));
		}

		@Test
		@DisplayName("게시글 수정 - 권한 없는 사용자가 수정 요청시 예외 발생")
		void updateBoard_Fail_WhenForbidden() throws Exception {
			// given
			Long boardId = 1L;
			Long memberId = 2L;

			UpdatingBoardRequest request = new UpdatingBoardRequest(
				"수정된 제목",
				"수정된 내용",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"MISSING",
				null,
				null,
				null,
				null
			);

			given(boardService.updateAnimalBoard(eq(boardId), any(UpdatingBoardRequest.class), eq(memberId)))
				.willThrow(new CustomException(BoardErrorCode.BOARD_FORBIDDEN));

			// when & then
			mockMvc.perform(put("/board/{boardId}", boardId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJsonString(request))
					.with(mockUser(memberId)))
				.andExpect(status().isForbidden());

			verify(boardService).updateAnimalBoard(eq(boardId), any(UpdatingBoardRequest.class), eq(memberId));
		}
	}
}