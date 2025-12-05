package tetoandeggens.seeyouagainbe.board.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
import tetoandeggens.seeyouagainbe.board.dto.response.PresignedUrlResponse;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.BoardTag;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.board.repository.BoardTagRepository;
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