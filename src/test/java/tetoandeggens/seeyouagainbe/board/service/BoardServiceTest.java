package tetoandeggens.seeyouagainbe.board.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.EntityManager;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalLocationRepository;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalRepository;
import tetoandeggens.seeyouagainbe.animal.repository.BreedTypeRepository;
import tetoandeggens.seeyouagainbe.board.dto.request.UpdatingBoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.request.WritingBoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.response.MyBoardListResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.MyBoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.PresignedUrlResponse;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.BoardTag;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.board.repository.BoardTagRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BoardErrorCode;
import tetoandeggens.seeyouagainbe.image.service.ImageService;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("BoardService 통합 테스트")
class BoardServiceTest extends ServiceTest {

	@Autowired
	private BoardService boardService;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private BoardTagRepository boardTagRepository;

	@Autowired
	private AnimalRepository animalRepository;

	@Autowired
	private AnimalLocationRepository animalLocationRepository;

	@Autowired
	private BreedTypeRepository breedTypeRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private ImageService imageService;

	private Member testMember;
	private BreedType chihuahua;

	@BeforeEach
	void setUp() {
		boardTagRepository.deleteAll();
		boardRepository.deleteAll();
		animalRepository.deleteAll();
		animalLocationRepository.deleteAll();
		memberRepository.deleteAll();
		breedTypeRepository.deleteAll();

		testMember = Member.builder()
			.loginId("testuser")
			.password("password")
			.nickName("테스트유저")
			.phoneNumber("010-1234-5678")
			.profile("https://profile.com/image.jpg")
			.build();
		memberRepository.save(testMember);

		chihuahua = BreedType.builder()
			.name("치와와")
			.type("DOG")
			.code(UUID.randomUUID().toString())
			.build();
		breedTypeRepository.save(chihuahua);

		entityManager.flush();
		entityManager.clear();
	}

	@Nested
	@DisplayName("게시글 작성 테스트")
	class WriteAnimalBoardTests {

		@Test
		@DisplayName("게시글 작성 - 이미지 없이 성공")
		void writeBoard_Success_WithoutImages() {
			// given
			WritingBoardRequest request = new WritingBoardRequest(
				"실종 강아지를 찾습니다",
				"강남역 근처에서 실종되었습니다",
				"417000",
				"치와와",
				"M",
				"갈색",
				"서울특별시 강남구 테헤란로",
				37.4979,
				127.0276,
				"MISSING",
				"Y",
				"서울특별시",
				"강남구",
				false,
				null,
				List.of("강아지", "실종")
			);

			given(imageService.generatePresignedUrls(anyLong(), anyInt())).willReturn(List.of());

			// when
			PresignedUrlResponse response = boardService.writeAnimalBoard(request, testMember.getId());

			// then
			assertThat(response).isNotNull();
			assertThat(response.presignedUrls()).isEmpty();

			List<Board> boards = boardRepository.findAll();
			assertThat(boards).hasSize(1);

			Board savedBoard = boards.get(0);
			assertThat(savedBoard.getTitle()).isEqualTo("실종 강아지를 찾습니다");
			assertThat(savedBoard.getContent()).isEqualTo("강남역 근처에서 실종되었습니다");
			assertThat(savedBoard.getContentType()).isEqualTo(ContentType.MISSING);

			Animal savedAnimal = savedBoard.getAnimal();
			assertThat(savedAnimal.getSpecies()).isEqualTo(Species.DOG);
			assertThat(savedAnimal.getSex()).isEqualTo(Sex.M);
			assertThat(savedAnimal.getColor()).isEqualTo("갈색");
			assertThat(savedAnimal.getAnimalType()).isEqualTo(AnimalType.MISSING);
			assertThat(savedAnimal.getNeuteredState()).isEqualTo(NeuteredState.Y);
			assertThat(savedAnimal.getCity()).isEqualTo("서울특별시");
			assertThat(savedAnimal.getTown()).isEqualTo("강남구");

			List<BoardTag> tags = boardTagRepository.findAll();
			assertThat(tags).hasSize(2);
			assertThat(tags).extracting(BoardTag::getName)
				.containsExactlyInAnyOrder("강아지", "실종");
		}

		@Test
		@DisplayName("게시글 작성 - 이미지와 함께 성공")
		void writeBoard_Success_WithImages() {
			// given
			WritingBoardRequest request = new WritingBoardRequest(
				"목격 고양이 정보",
				"서초구에서 고양이를 목격했습니다",
				"422400",
				"코리안 숏헤어",
				"F",
				"흰색",
				"서울특별시 서초구",
				37.4833,
				127.0322,
				"WITNESS",
				"N",
				"서울특별시",
				"서초구",
				true,
				2,
				List.of("고양이", "목격")
			);

			given(imageService.generatePresignedUrls(anyLong(), eq(2)))
				.willReturn(List.of(
					"https://s3.amazonaws.com/presigned-url-1",
					"https://s3.amazonaws.com/presigned-url-2"
				));

			// when
			PresignedUrlResponse response = boardService.writeAnimalBoard(request, testMember.getId());

			// then
			assertThat(response.presignedUrls()).hasSize(2);
			assertThat(response.presignedUrls()).containsExactly(
				"https://s3.amazonaws.com/presigned-url-1",
				"https://s3.amazonaws.com/presigned-url-2"
			);

			List<Board> boards = boardRepository.findAll();
			assertThat(boards).hasSize(1);
			assertThat(boards.get(0).getContentType()).isEqualTo(ContentType.WITNESS);

			verify(imageService).generatePresignedUrls(anyLong(), eq(2));
		}

		@Test
		@DisplayName("게시글 작성 - 품종 없이 성공")
		void writeBoard_Success_WithoutBreedType() {
			// given
			WritingBoardRequest request = new WritingBoardRequest(
				"믹스견 실종",
				"믹스견을 찾습니다",
				"417000",
				null,
				"M",
				"검정색",
				"서울특별시 강남구",
				37.4979,
				127.0276,
				"MISSING",
				"U",
				"서울특별시",
				"강남구",
				false,
				null,
				List.of("믹스견")
			);

			given(imageService.generatePresignedUrls(anyLong(), anyInt())).willReturn(List.of());

			// when
			PresignedUrlResponse response = boardService.writeAnimalBoard(request, testMember.getId());

			// then
			assertThat(response).isNotNull();

			List<Board> boards = boardRepository.findAll();
			assertThat(boards).hasSize(1);

			Animal savedAnimal = boards.get(0).getAnimal();
			assertThat(savedAnimal.getBreedType()).isNull();
		}
	}

	@Nested
	@DisplayName("게시글 삭제 테스트")
	class DeleteAnimalBoardTests {

		@Test
		@DisplayName("게시글 삭제 - 성공")
		void deleteBoard_Success() {
			// given
			Board board = createBoard("삭제할 게시글", "내용", ContentType.MISSING, testMember);
			boardRepository.save(board);

			entityManager.flush();
			entityManager.clear();

			// when
			boardService.deleteAnimalBoard(board.getId(), testMember.getId());

			// then
			entityManager.flush();
			entityManager.clear();

			Board deletedBoard = boardRepository.findById(board.getId()).orElseThrow();
			assertThat(deletedBoard.getIsDeleted()).isTrue();
			assertThat(deletedBoard.getAnimal().getIsDeleted()).isTrue();
		}

		@Test
		@DisplayName("게시글 삭제 - 존재하지 않는 게시글 삭제시 예외 발생")
		void deleteBoard_ThrowsException_WhenNotExists() {
			// when & then
			assertThatThrownBy(() -> boardService.deleteAnimalBoard(999L, testMember.getId()))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(BoardErrorCode.BOARD_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("게시글 삭제 - 권한 없는 사용자가 삭제 요청시 예외 발생")
		void deleteBoard_ThrowsException_WhenForbidden() {
			// given
			Board board = createBoard("삭제할 게시글", "내용", ContentType.MISSING, testMember);
			boardRepository.save(board);

			entityManager.flush();
			entityManager.clear();

			Long otherMemberId = 999L;

			// when & then
			assertThatThrownBy(() -> boardService.deleteAnimalBoard(board.getId(), otherMemberId))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(BoardErrorCode.BOARD_FORBIDDEN.getMessage());
		}
	}

	@Nested
	@DisplayName("게시글 수정 테스트")
	class UpdateAnimalBoardTests {

		@Test
		@DisplayName("게시글 수정 - 제목과 내용만 수정 성공")
		void updateBoard_Success_TitleAndContentOnly() {
			// given
			Board board = createBoard("원래 제목", "원래 내용", ContentType.MISSING, testMember);
			boardRepository.save(board);

			entityManager.flush();
			entityManager.clear();

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
				null,
				null,
				null,
				"MISSING",
				null,
				null,
				null,
				null
			);

			given(imageService.generatePresignedUrls(anyLong(), anyInt())).willReturn(List.of());

			// when
			PresignedUrlResponse response = boardService.updateAnimalBoard(
				board.getId(),
				request,
				testMember.getId()
			);

			// then
			assertThat(response).isNotNull();

			entityManager.flush();
			entityManager.clear();

			Board updatedBoard = boardRepository.findById(board.getId()).orElseThrow();
			assertThat(updatedBoard.getTitle()).isEqualTo("수정된 제목");
			assertThat(updatedBoard.getContent()).isEqualTo("수정된 내용");
		}

		@Test
		@DisplayName("게시글 수정 - 동물 정보 수정 성공")
		void updateBoard_Success_AnimalInfo() {
			// given
			Board board = createBoard("제목", "내용", ContentType.MISSING, testMember);
			boardRepository.save(board);

			entityManager.flush();
			entityManager.clear();

			UpdatingBoardRequest request = new UpdatingBoardRequest(
				null,
				null,
				"422400",
				null,
				"F",
				null,
				"흰색",
				null,
				null,
				null,
				null,
				null,
				"WITNESS",
				null,
				null,
				null,
				null
			);

			given(imageService.generatePresignedUrls(anyLong(), anyInt())).willReturn(List.of());

			// when
			boardService.updateAnimalBoard(board.getId(), request, testMember.getId());

			// then
			entityManager.flush();
			entityManager.clear();

			Board updatedBoard = boardRepository.findById(board.getId()).orElseThrow();
			assertThat(updatedBoard.getContentType()).isEqualTo(ContentType.WITNESS);
			assertThat(updatedBoard.getAnimal().getSpecies()).isEqualTo(Species.CAT);
			assertThat(updatedBoard.getAnimal().getSex()).isEqualTo(Sex.F);
			assertThat(updatedBoard.getAnimal().getColor()).isEqualTo("흰색");
		}

		@Test
		@DisplayName("게시글 수정 - 존재하지 않는 게시글 수정시 예외 발생")
		void updateBoard_ThrowsException_WhenNotExists() {
			// given
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
				null,
				null,
				null,
				"MISSING",
				null,
				null,
				null,
				null
			);

			// when & then
			assertThatThrownBy(() -> boardService.updateAnimalBoard(999L, request, testMember.getId()))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(BoardErrorCode.BOARD_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("게시글 수정 - 권한 없는 사용자가 수정 요청시 예외 발생")
		void updateBoard_ThrowsException_WhenForbidden() {
			// given
			Board board = createBoard("제목", "내용", ContentType.MISSING, testMember);
			boardRepository.save(board);

			entityManager.flush();
			entityManager.clear();

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
				null,
				null,
				null,
				"MISSING",
				null,
				null,
				null,
				null
			);

			Long otherMemberId = 999L;

			// when & then
			assertThatThrownBy(() -> boardService.updateAnimalBoard(board.getId(), request, otherMemberId))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(BoardErrorCode.BOARD_FORBIDDEN.getMessage());
		}
	}

	@Nested
	@DisplayName("내가 작성한 게시글 목록 조회 테스트")
	class GetMyBoardListTests {

		private Member otherMember;

		@BeforeEach
		void setUpForMyBoardList() {
			otherMember = Member.builder()
					.loginId("otheruser")
					.password("password")
					.nickName("다른유저")
					.phoneNumber("010-9876-5432")
					.profile("https://profile.com/other.jpg")
					.build();
			memberRepository.save(otherMember);

			entityManager.flush();
			entityManager.clear();
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 성공")
		void getMyBoardList_Success() {
			// given
			Board myBoard1 = createBoard("내 게시글1", "내용1", ContentType.MISSING, testMember);
			Board myBoard2 = createBoard("내 게시글2", "내용2", ContentType.WITNESS, testMember);
			Board otherBoard = createBoard("다른사람 게시글", "내용3", ContentType.MISSING, otherMember);

			boardRepository.saveAll(List.of(myBoard1, myBoard2, otherBoard));

			boardTagRepository.bulkInsert(List.of("강아지", "실종"), myBoard1);
			boardTagRepository.bulkInsert(List.of("고양이", "목격"), myBoard2);

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(response).isNotNull();
			assertThat(response.boardCount()).isEqualTo(2);
			assertThat(response.board().getData()).hasSize(2);
			assertThat(response.board().isHasNext()).isFalse();

			List<MyBoardResponse> boards = response.board().getData();
			assertThat(boards).extracting(MyBoardResponse::title)
					.containsExactly("내 게시글2", "내 게시글1");  // 최신순
			assertThat(boards.get(0).tags()).containsExactlyInAnyOrder("고양이", "목격");
			assertThat(boards.get(1).tags()).containsExactlyInAnyOrder("강아지", "실종");
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 최신순 정렬")
		void getMyBoardList_Success_LatestSort() {
			// given
			Board board1 = createBoard("첫번째 게시글", "내용", ContentType.MISSING, testMember);
			Board board2 = createBoard("두번째 게시글", "내용", ContentType.MISSING, testMember);
			Board board3 = createBoard("세번째 게시글", "내용", ContentType.MISSING, testMember);

			boardRepository.saveAll(List.of(board1, board2, board3));

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			List<MyBoardResponse> boards = response.board().getData();
			assertThat(boards).hasSize(3);
			assertThat(boards.get(0).boardId()).isGreaterThan(boards.get(1).boardId());
			assertThat(boards.get(1).boardId()).isGreaterThan(boards.get(2).boardId());
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 오래된순 정렬")
		void getMyBoardList_Success_OldestSort() {
			// given
			Board board1 = createBoard("첫번째 게시글", "내용", ContentType.MISSING, testMember);
			Board board2 = createBoard("두번째 게시글", "내용", ContentType.MISSING, testMember);
			Board board3 = createBoard("세번째 게시글", "내용", ContentType.MISSING, testMember);

			boardRepository.saveAll(List.of(board1, board2, board3));

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.OLDEST,
					testMember.getId()
			);

			// then
			List<MyBoardResponse> boards = response.board().getData();
			assertThat(boards).hasSize(3);
			assertThat(boards.get(0).boardId()).isLessThan(boards.get(1).boardId());
			assertThat(boards.get(1).boardId()).isLessThan(boards.get(2).boardId());
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 커서 페이징")
		void getMyBoardList_Success_WithCursorPaging() {
			// given - 7개로 증가 (2 + 2 + 2 + 1 = 7)
			List<Board> boards = new ArrayList<>();
			for (int i = 1; i <= 7; i++) {
				boards.add(createBoard("게시글" + i, "내용", ContentType.MISSING, testMember));
			}
			boardRepository.saveAll(boards);

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest firstRequest = new CursorPageRequest(null, 2);

			// when - 첫 번째 페이지
			MyBoardListResponse firstResponse = boardService.getMyBoardList(
					firstRequest,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then - 첫 번째 페이지 검증
			assertThat(firstResponse.board().getData()).hasSize(2);
			assertThat(firstResponse.board().isHasNext()).isTrue();
			assertThat(firstResponse.board().getNextCursor()).isNotNull();

			Long cursorId = firstResponse.board().getNextCursor();

			// when - 두 번째 페이지
			CursorPageRequest secondRequest = new CursorPageRequest(cursorId, 2);
			MyBoardListResponse secondResponse = boardService.getMyBoardList(
					secondRequest,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then - 두 번째 페이지 검증
			assertThat(secondResponse.board().getData()).hasSize(2);
			assertThat(secondResponse.board().isHasNext()).isTrue();  // 이제 true!
			assertThat(secondResponse.board().getData())
					.allMatch(board -> board.boardId() < cursorId);
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 삭제된 게시글 제외")
		void getMyBoardList_ExcludesDeletedBoards() {
			// given
			Board normalBoard = createBoard("정상 게시글", "내용", ContentType.MISSING, testMember);
			Board deletedBoard = createBoard("삭제된 게시글", "내용", ContentType.MISSING, testMember);

			boardRepository.saveAll(List.of(normalBoard, deletedBoard));

			entityManager.flush();
			entityManager.clear();

			// 게시글 삭제
			boardService.deleteAnimalBoard(deletedBoard.getId(), testMember.getId());

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(response.boardCount()).isEqualTo(1);
			assertThat(response.board().getData()).hasSize(1);
			assertThat(response.board().getData().get(0).title()).isEqualTo("정상 게시글");
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 게시글이 없는 경우")
		void getMyBoardList_EmptyWhenNoBoards() {
			// given
			Board otherBoard = createBoard("다른사람 게시글", "내용", ContentType.MISSING, otherMember);
			boardRepository.save(otherBoard);

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(response.boardCount()).isEqualTo(0);
			assertThat(response.board().getData()).isEmpty();
			assertThat(response.board().isEmpty()).isTrue();
			assertThat(response.board().isHasNext()).isFalse();
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 태그 정보 포함")
		void getMyBoardList_IncludesTags() {
			// given
			Board board = createBoard("태그 테스트", "내용", ContentType.MISSING, testMember);
			boardRepository.save(board);

			boardTagRepository.bulkInsert(List.of("태그1", "태그2", "태그3"), board);

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(response.board().getData()).hasSize(1);
			MyBoardResponse boardResponse = response.board().getData().get(0);
			assertThat(boardResponse.tags()).hasSize(3);
			assertThat(boardResponse.tags()).containsExactlyInAnyOrder("태그1", "태그2", "태그3");
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - animalType 정보 포함")
		void getMyBoardList_IncludesAnimalType() {
			// given
			Board missingBoard = createBoard("실종", "내용", ContentType.MISSING, testMember);
			Board witnessBoard = createBoard("목격", "내용", ContentType.WITNESS, testMember);

			boardRepository.saveAll(List.of(missingBoard, witnessBoard));

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(response.board().getData()).hasSize(2);
			assertThat(response.board().getData())
					.extracting(MyBoardResponse::animalType)
					.containsExactlyInAnyOrder(AnimalType.MISSING, AnimalType.WITNESS);
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 총 개수 정확성")
		void getMyBoardList_CorrectTotalCount() {
			// given
			for (int i = 1; i <= 15; i++) {
				Board board = createBoard("게시글" + i, "내용", ContentType.MISSING, testMember);
				boardRepository.save(board);
			}

			Board otherBoard = createBoard("다른사람", "내용", ContentType.MISSING, otherMember);
			boardRepository.save(otherBoard);

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			MyBoardListResponse response = boardService.getMyBoardList(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(response.boardCount()).isEqualTo(15);
			assertThat(response.board().getData()).hasSize(10);  // 첫 페이지 size만큼
			assertThat(response.board().isHasNext()).isTrue();
		}
	}

	private Board createBoard(String title, String content, ContentType contentType, Member member) {
		AnimalLocation location = AnimalLocation.builder()
			.address("서울특별시 강남구")
			.latitude(12.34)
			.longitude(56.78)
			.build();
		animalLocationRepository.save(location);

		Animal animal = Animal.builder()
			.animalType(contentType == ContentType.MISSING ? AnimalType.MISSING : AnimalType.WITNESS)
			.sex(Sex.M)
			.species(Species.DOG)
			.color("갈색")
			.breedType(chihuahua)
			.animalLocation(location)
			.build();
		animalRepository.save(animal);

		return Board.builder()
			.contentType(contentType)
			.title(title)
			.content(content)
			.animal(animal)
			.member(member)
			.build();
	}
}