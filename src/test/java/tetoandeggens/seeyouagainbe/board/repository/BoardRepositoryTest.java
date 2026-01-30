package tetoandeggens.seeyouagainbe.board.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalS3Profile;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.board.dto.response.MyBoardResponse;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.BoardTag;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@DisplayName("BoardRepository QueryDSL 복잡한 쿼리 테스트")
class BoardRepositoryTest extends RepositoryTest {

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private EntityManager entityManager;

	private Member testMember;
	private BreedType chihuahua;

	@BeforeEach
	void setUp() {
		boardRepository.deleteAll();

		testMember = Member.builder()
			.loginId("testuser")
			.password("password")
			.nickName("테스트유저")
			.phoneNumber("010-1234-5678")
			.profile("https://profile.com/image.jpg")
			.build();
		entityManager.persist(testMember);

		chihuahua = BreedType.builder()
			.name("치와와")
			.type("DOG")
			.code(UUID.randomUUID().toString())
			.build();
		entityManager.persist(chihuahua);

		entityManager.flush();
		entityManager.clear();
	}

	@Nested
	@DisplayName("게시글 카운트 조회 테스트")
	class GetAnimalBoardsCountTests {

		@Test
		@DisplayName("게시글 카운트 조회 - 전체 카운트 성공")
		void getCount_Success_All() {
			// given
			Board board1 = createBoard("게시글1", ContentType.MISSING);
			Board board2 = createBoard("게시글2", ContentType.WITNESS);
			entityManager.persist(board1);
			entityManager.persist(board2);

			entityManager.flush();
			entityManager.clear();

			// when
			Long count = boardRepository.getAnimalBoardsCount(null, null, null, null, null, null, null, null, null);

			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("게시글 카운트 조회 - ContentType 필터링 카운트 성공")
		void getCount_Success_WithContentTypeFilter() {
			// given
			Board board1 = createBoard("실종1", ContentType.MISSING);
			Board board2 = createBoard("실종2", ContentType.MISSING);
			Board board3 = createBoard("목격1", ContentType.WITNESS);
			entityManager.persist(board1);
			entityManager.persist(board2);
			entityManager.persist(board3);

			entityManager.flush();
			entityManager.clear();

			// when
			Long count = boardRepository.getAnimalBoardsCount(ContentType.MISSING, null, null, null, null, null, null, null, null);

			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("게시글 카운트 조회 - 삭제된 게시글 제외 성공")
		void getCount_Success_ExcludingDeleted() {
			// given
			Board normalBoard = createBoard("정상 게시글", ContentType.MISSING);
			Board deletedBoard = createBoard("삭제된 게시글", ContentType.MISSING);
			deletedBoard.updateIsDeleted(true);
			entityManager.persist(normalBoard);
			entityManager.persist(deletedBoard);

			entityManager.flush();
			entityManager.clear();

			// when
			Long count = boardRepository.getAnimalBoardsCount(null, null, null, null, null, null, null, null, null);

			// then
			assertThat(count).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("이미지 ID 검증 테스트")
	class CountValidImageIdsTests {

		@Test
		@DisplayName("이미지 ID 검증 - 모두 유효한 경우")
		void countValidImageIds_Success_AllValid() {
			// given
			Board board = createBoard("게시글", ContentType.MISSING);
			entityManager.persist(board);

			Animal animal = board.getAnimal();
			AnimalS3Profile profile1 = createAndSaveProfile(animal, "profile1.jpg");
			AnimalS3Profile profile2 = createAndSaveProfile(animal, "profile2.jpg");

			entityManager.flush();
			entityManager.clear();

			List<Long> imageIds = List.of(profile1.getId(), profile2.getId());

			// when
			long count = boardRepository.countValidImageIds(imageIds, animal.getId());

			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("이미지 ID 검증 - 일부만 유효한 경우")
		void countValidImageIds_Success_PartiallyValid() {
			// given
			Board board = createBoard("게시글", ContentType.MISSING);
			entityManager.persist(board);

			Animal animal = board.getAnimal();
			AnimalS3Profile profile1 = createAndSaveProfile(animal, "profile1.jpg");

			entityManager.flush();
			entityManager.clear();

			List<Long> imageIds = List.of(profile1.getId(), 999L);

			// when
			long count = boardRepository.countValidImageIds(imageIds, animal.getId());

			// then
			assertThat(count).isEqualTo(1);
		}

		@Test
		@DisplayName("이미지 ID 검증 - 다른 동물의 이미지인 경우")
		void countValidImageIds_Success_DifferentAnimal() {
			// given
			Board board1 = createBoard("게시글1", ContentType.MISSING);
			Board board2 = createBoard("게시글2", ContentType.MISSING);
			entityManager.persist(board1);
			entityManager.persist(board2);

			Animal animal1 = board1.getAnimal();
			Animal animal2 = board2.getAnimal();

			AnimalS3Profile profile1 = createAndSaveProfile(animal1, "profile1.jpg");
			AnimalS3Profile profile2 = createAndSaveProfile(animal2, "profile2.jpg");

			entityManager.flush();
			entityManager.clear();

			List<Long> imageIds = List.of(profile1.getId(), profile2.getId());

			// when
			long count = boardRepository.countValidImageIds(imageIds, animal1.getId());

			// then
			assertThat(count).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("태그 ID 검증 테스트")
	class CountValidTagInfoTests {

		@Test
		@DisplayName("태그 ID 검증 - 모두 유효한 경우")
		void countValidTagIds_Success_AllValid() {
			// given
			Board board = createBoard("게시글", ContentType.MISSING);
			entityManager.persist(board);

			BoardTag tag1 = BoardTag.builder().name("태그1").board(board).build();
			BoardTag tag2 = BoardTag.builder().name("태그2").board(board).build();
			entityManager.persist(tag1);
			entityManager.persist(tag2);

			entityManager.flush();
			entityManager.clear();

			List<Long> tagIds = List.of(tag1.getId(), tag2.getId());

			// when
			long count = boardRepository.countValidTagIds(tagIds, board.getId());

			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("태그 ID 검증 - 일부만 유효한 경우")
		void countValidTagIds_Success_PartiallyValid() {
			// given
			Board board = createBoard("게시글", ContentType.MISSING);
			entityManager.persist(board);

			BoardTag tag1 = BoardTag.builder().name("태그1").board(board).build();
			entityManager.persist(tag1);

			entityManager.flush();
			entityManager.clear();

			List<Long> tagIds = List.of(tag1.getId(), 999L);

			// when
			long count = boardRepository.countValidTagIds(tagIds, board.getId());

			// then
			assertThat(count).isEqualTo(1);
		}

		@Test
		@DisplayName("태그 ID 검증 - 다른 게시글의 태그인 경우")
		void countValidTagIds_Success_DifferentBoard() {
			// given
			Board board1 = createBoard("게시글1", ContentType.MISSING);
			Board board2 = createBoard("게시글2", ContentType.MISSING);
			entityManager.persist(board1);
			entityManager.persist(board2);

			BoardTag tag1 = BoardTag.builder().name("태그1").board(board1).build();
			BoardTag tag2 = BoardTag.builder().name("태그2").board(board2).build();
			entityManager.persist(tag1);
			entityManager.persist(tag2);

			entityManager.flush();
			entityManager.clear();

			List<Long> tagIds = List.of(tag1.getId(), tag2.getId());

			// when
			long count = boardRepository.countValidTagIds(tagIds, board1.getId());

			// then
			assertThat(count).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("게시글 fetch join 조회 테스트")
	class FindByIdWithAnimalTests {

		@Test
		@DisplayName("게시글 fetch join 조회 - 성공")
		void findByIdWithAnimal_Success() {
			// given
			Board board = createBoard("게시글", ContentType.MISSING);
			entityManager.persist(board);

			entityManager.flush();
			entityManager.clear();

			// when
			Board result = boardRepository.findByIdWithAnimal(board.getId());

			// then
			assertThat(result).isNotNull();
			assertThat(result.getId()).isEqualTo(board.getId());
			assertThat(result.getAnimal()).isNotNull();
			assertThat(result.getAnimal().getAnimalLocation()).isNotNull();
			assertThat(result.getAnimal().getBreedType()).isNotNull();
		}

		@Test
		@DisplayName("게시글 fetch join 조회 - 존재하지 않는 ID로 조회시 null 반환")
		void findByIdWithAnimal_ReturnsNull_WhenNotExists() {
			// when
			Board result = boardRepository.findByIdWithAnimal(999L);

			// then
			assertThat(result).isNull();
		}
	}

	@Nested
	@DisplayName("내가 작성한 게시글 목록 조회 테스트")
	class GetMyBoardsTests {

		private Member testMember;
		private Member otherMember;

		@BeforeEach
		void setUpMembers() {
			testMember = Member.builder()
					.loginId("testuser@example.com")
					.password("password123!")
					.nickName("테스트유저")
					.phoneNumber("01012345678")
					.build();
			entityManager.persist(testMember);

			otherMember = Member.builder()
					.loginId("other@example.com")
					.password("password123!")
					.nickName("다른유저")
					.phoneNumber("01087654321")
					.build();
			entityManager.persist(otherMember);

			entityManager.flush();
			entityManager.clear();
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 성공")
		void getMyBoards_Success() {
			// given
			Board myBoard1 = createBoardForMember("내 게시글1", ContentType.MISSING, testMember);
			Board myBoard2 = createBoardForMember("내 게시글2", ContentType.WITNESS, testMember);
			Board otherBoard = createBoardForMember("다른사람 게시글", ContentType.MISSING, otherMember);

			entityManager.persist(myBoard1);
			entityManager.persist(myBoard2);
			entityManager.persist(otherBoard);
			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			List<MyBoardResponse> responses = boardRepository.getMyBoards(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(responses).hasSize(2);
			assertThat(responses).extracting("title")
					.containsExactlyInAnyOrder("내 게시글1", "내 게시글2");
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 최신순 정렬")
		void getMyBoards_SortedByLatest() {
			// given
			Board board1 = createBoardForMember("게시글1", ContentType.MISSING, testMember);
			Board board2 = createBoardForMember("게시글2", ContentType.WITNESS, testMember);
			entityManager.persist(board1);
			entityManager.persist(board2);
			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			List<MyBoardResponse> responses = boardRepository.getMyBoards(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(responses).isNotEmpty();
			if (responses.size() >= 2) {
				assertThat(responses.get(0).boardId())
						.isGreaterThan(responses.get(1).boardId());
			}
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 커서 페이징")
		void getMyBoards_WithCursorPaging() {
			// given
			for (int i = 1; i <= 5; i++) {
				Board board = createBoardForMember("게시글" + i, ContentType.MISSING, testMember);
				entityManager.persist(board);
			}
			entityManager.flush();
			entityManager.clear();

			CursorPageRequest firstRequest = new CursorPageRequest(null, 2);
			List<MyBoardResponse> firstPage = boardRepository.getMyBoards(
					firstRequest,
					SortDirection.LATEST,
					testMember.getId()
			);

			assertThat(firstPage).hasSizeLessThanOrEqualTo(3);
			Long cursorId = firstPage.get(1).boardId();

			// when
			CursorPageRequest secondRequest = new CursorPageRequest(cursorId, 2);
			List<MyBoardResponse> secondPage = boardRepository.getMyBoards(
					secondRequest,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(secondPage).isNotEmpty();
			assertThat(secondPage).allMatch(board -> !board.boardId().equals(cursorId));
		}

		@Test
		@DisplayName("내가 작성한 게시글 목록 조회 - 삭제된 게시글 제외")
		void getMyBoards_ExcludingDeleted() {
			// given
			Board normalBoard = createBoardForMember("정상 게시글", ContentType.MISSING, testMember);
			Board deletedBoard = createBoardForMember("삭제된 게시글", ContentType.MISSING, testMember);
			deletedBoard.updateIsDeleted(true);
			entityManager.persist(normalBoard);
			entityManager.persist(deletedBoard);
			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			List<MyBoardResponse> responses = boardRepository.getMyBoards(
					request,
					SortDirection.LATEST,
					testMember.getId()
			);

			// then
			assertThat(responses).hasSize(1);
			assertThat(responses.get(0).title()).isEqualTo("정상 게시글");
		}

		@Test
		@DisplayName("내가 작성한 게시글 총 개수 조회 - 성공")
		void getMyBoardsCount_Success() {
			// given
			Board board1 = createBoardForMember("게시글1", ContentType.MISSING, testMember);
			Board board2 = createBoardForMember("게시글2", ContentType.WITNESS, testMember);
			Board otherBoard = createBoardForMember("다른사람 게시글", ContentType.MISSING, otherMember);
			entityManager.persist(board1);
			entityManager.persist(board2);
			entityManager.persist(otherBoard);
			entityManager.flush();
			entityManager.clear();

			// when
			Long count = boardRepository.getMyBoardsCount(testMember.getId());

			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("내가 작성한 게시글 총 개수 조회 - 삭제된 게시글 제외")
		void getMyBoardsCount_ExcludingDeleted() {
			// given
			Board normalBoard = createBoardForMember("정상 게시글", ContentType.MISSING, testMember);
			Board deletedBoard = createBoardForMember("삭제된 게시글", ContentType.MISSING, testMember);
			deletedBoard.updateIsDeleted(true);
			entityManager.persist(normalBoard);
			entityManager.persist(deletedBoard);
			entityManager.flush();
			entityManager.clear();

			// when
			Long count = boardRepository.getMyBoardsCount(testMember.getId());

			// then
			assertThat(count).isEqualTo(1);
		}

		private Board createBoardForMember(String title, ContentType contentType, Member member) {
			AnimalLocation location = AnimalLocation.builder()
					.address("서울특별시 강남구")
					.latitude(37.4979)
					.longitude(127.0276)
					.build();
			entityManager.persist(location);

			Animal animal = Animal.builder()
					.animalType(AnimalType.MISSING)
					.sex(Sex.M)
					.species(Species.DOG)
					.color("갈색")
					.animalLocation(location)
					.neuteredState(NeuteredState.Y)
					.city("서울특별시")
					.town("강남구")
					.build();
			entityManager.persist(animal);

			return Board.builder()
					.contentType(contentType)
					.title(title)
					.content("내용")
					.animal(animal)
					.member(member)
					.build();
		}
	}

	private Board createBoard(String title, ContentType contentType) {
		AnimalLocation location = AnimalLocation.builder()
			.address("서울특별시 강남구")
			.latitude(37.4979)
			.longitude(127.0276)
			.build();
		entityManager.persist(location);

		Animal animal = Animal.builder()
			.animalType(contentType == ContentType.MISSING ? AnimalType.MISSING : AnimalType.WITNESS)
			.sex(Sex.M)
			.species(Species.DOG)
			.color("갈색")
			.breedType(chihuahua)
			.animalLocation(location)
			.build();
		entityManager.persist(animal);

		return Board.builder()
			.contentType(contentType)
			.title(title)
			.content("테스트 내용")
			.animal(animal)
			.member(testMember)
			.build();
	}

	private AnimalS3Profile createAndSaveProfile(Animal animal, String profileUrl) {
		AnimalS3Profile profile = AnimalS3Profile.builder()
			.animal(animal)
			.profile(profileUrl)
			.build();
		entityManager.persist(profile);
		return profile;
	}
}