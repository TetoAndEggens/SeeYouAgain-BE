package tetoandeggens.seeyouagainbe.board.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalLocationRepository;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalRepository;
import tetoandeggens.seeyouagainbe.animal.repository.BreedTypeRepository;
import tetoandeggens.seeyouagainbe.board.dto.request.BoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardDetailResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardListResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.PresignedUrlResponse;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.BoardTag;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.board.repository.BoardTagRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BoardErrorCode;
import tetoandeggens.seeyouagainbe.image.service.ImageService;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Service
@RequiredArgsConstructor
public class BoardService {

	private final BoardRepository boardRepository;
	private final BoardTagRepository boardTagRepository;
	private final AnimalRepository animalRepository;
	private final AnimalLocationRepository animalLocationRepository;
	private final BreedTypeRepository breedTypeRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ImageService imageService;

	@Transactional
	public PresignedUrlResponse writeAnimalBoard(BoardRequest request, Long memberId) {
		AnimalLocation savedAnimalLocation = createAndSaveAnimalLocation(request);
		Animal savedAnimal = createAndSaveAnimal(request, savedAnimalLocation);
		Board savedBoard = createAndSaveBoard(request, savedAnimal, memberId);

		saveBoardTags(request.tags(), savedBoard);

		List<String> presignedUrls = generatePresignedUrlsIfNeeded(request.count(), savedAnimal.getId());

		return new PresignedUrlResponse(presignedUrls);
	}

	@Transactional(readOnly = true)
	public BoardListResponse getAnimalBoardList(
		CursorPageRequest request, SortDirection sortDirection, String type) {
		ContentType contentType = null;
		if (type != null && !type.isBlank()) {
			contentType = ContentType.fromCode(type.toUpperCase());
		}

		List<BoardResponse> responses = boardRepository.getAnimalBoards(
			request, sortDirection, contentType);

		List<BoardResponse> responsesWithTags = attachTagsToResponses(responses);

		CursorPage<BoardResponse, Long> cursorPage = CursorPage.of(
			responsesWithTags,
			request.size(),
			BoardResponse::boardId
		);

		Long totalCount = boardRepository.getAnimalBoardsCount(contentType);

		return BoardListResponse.of(totalCount.intValue(), cursorPage);
	}

	@Transactional(readOnly = true)
	public BoardDetailResponse getAnimalBoard(Long boardId) {
		BoardDetailResponse response = boardRepository.getAnimalBoard(boardId);

		if (response == null) {
			throw new CustomException(BoardErrorCode.BOARD_NOT_FOUND);
		}

		return response;
	}

	@Transactional
	public void deleteAnimalBoard(Long boardId, Long memberId) {
		Board board = boardRepository.findById(boardId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));

		if (!board.getMember().getId().equals(memberId)) {
			throw new CustomException(BoardErrorCode.BOARD_FORBIDDEN);
		}

		board.updateIsDeleted(true);

		Animal animal = board.getAnimal();
		animal.updateIsDeleted(true);

		boardRepository.softDeleteByAnimalId(animal.getId());
		chatRoomRepository.softDeleteByBoardId(boardId);
	}

	private AnimalLocation createAndSaveAnimalLocation(BoardRequest request) {
		AnimalLocation animalLocation = AnimalLocation.builder()
			.address(request.address())
			.latitude(request.latitude())
			.longitude(request.longitude())
			.build();

		return animalLocationRepository.save(animalLocation);
	}

	private Animal createAndSaveAnimal(BoardRequest request, AnimalLocation animalLocation) {
		BreedType breedType = findBreedType(request.breedType());
		Species species = Species.fromCode(request.species());
		Sex sex = Sex.fromCode(request.sex());
		AnimalType animalType = AnimalType.fromCode(request.animalType());

		Animal animal = Animal.builder()
			.animalType(animalType)
			.sex(sex)
			.species(species)
			.color(request.color())
			.breedType(breedType)
			.animalLocation(animalLocation)
			.build();

		return animalRepository.save(animal);
	}

	private Board createAndSaveBoard(BoardRequest request, Animal animal, Long memberId) {
		ContentType contentType = ContentType.fromCode(request.animalType());

		Board board = Board.builder()
			.contentType(contentType)
			.title(request.title())
			.content(request.content())
			.animal(animal)
			.member(new Member(memberId))
			.build();

		return boardRepository.save(board);
	}

	private BreedType findBreedType(String breedTypeName) {
		if (breedTypeName == null || breedTypeName.isBlank()) {
			return null;
		}
		return breedTypeRepository.findByName(breedTypeName).orElse(null);
	}

	private void saveBoardTags(List<String> tags, Board board) {
		if (tags != null && !tags.isEmpty()) {
			boardTagRepository.bulkInsert(tags, board);
		}
	}

	private List<String> generatePresignedUrlsIfNeeded(Integer count, Long animalId) {
		if (count != null && count > 0) {
			return imageService.generatePresignedUrls(animalId, count);
		}
		return List.of();
	}

	private List<BoardResponse> attachTagsToResponses(List<BoardResponse> responses) {
		if (responses.isEmpty()) {
			return responses;
		}

		List<Long> boardIds = new ArrayList<>();
		for (BoardResponse response : responses) {
			boardIds.add(response.boardId());
		}

		Map<Long, List<String>> tagsMap = new HashMap<>();
		List<BoardTag> boardTags = boardTagRepository.findByBoardIdInWithBoard(boardIds);

		for (BoardTag boardTag : boardTags) {
			Long boardId = boardTag.getBoard().getId();
			tagsMap.computeIfAbsent(boardId, k -> new ArrayList<>()).add(boardTag.getName());
		}

		List<BoardResponse> result = new ArrayList<>();
		for (BoardResponse response : responses) {
			BoardResponse newResponse = BoardResponse.builder()
				.boardId(response.boardId())
				.title(response.title())
				.species(response.species())
				.breedType(response.breedType())
				.sex(response.sex())
				.address(response.address())
				.latitude(response.latitude())
				.longitude(response.longitude())
				.animalType(response.animalType())
				.memberNickname(response.memberNickname())
				.profile(response.profile())
				.createdAt(response.createdAt())
				.updatedAt(response.updatedAt())
				.tags(tagsMap.getOrDefault(response.boardId(), List.of()))
				.build();
			result.add(newResponse);
		}

		return result;
	}
}
