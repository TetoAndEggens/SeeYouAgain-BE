package tetoandeggens.seeyouagainbe.board.service;

import java.util.List;

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
import tetoandeggens.seeyouagainbe.board.dto.request.AnimalBoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.response.AnimalBoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardListResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardResponse;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.image.service.ImageService;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Service
@RequiredArgsConstructor
public class BoardService {

	private final BoardRepository boardRepository;
	private final AnimalRepository animalRepository;
	private final AnimalLocationRepository animalLocationRepository;
	private final BreedTypeRepository breedTypeRepository;
	private final ImageService imageService;

	@Transactional
	public AnimalBoardResponse writeAnimalBoard(AnimalBoardRequest request, Long memberId) {
		BreedType breedType = null;
		if (request.breedType() != null && !request.breedType().isBlank()) {
			breedType = breedTypeRepository.findByName(request.breedType())
				.orElse(null);
		}

		Species species = Species.fromCode(request.species());
		Sex sex = Sex.fromCode(request.sex());
		AnimalType animalType = AnimalType.fromCode(request.animalType());
		ContentType contentType = ContentType.fromCode(request.animalType());

		AnimalLocation animalLocation = AnimalLocation.builder()
			.address(request.address())
			.latitude(request.latitude())
			.longitude(request.longitude())
			.build();

		AnimalLocation savedAnimalLocation = animalLocationRepository.save(animalLocation);

		Animal animal = Animal.builder()
			.animalType(animalType)
			.sex(sex)
			.species(species)
			.color(request.color())
			.breedType(breedType)
			.animalLocation(savedAnimalLocation)
			.build();

		Animal savedAnimal = animalRepository.save(animal);

		Board board = Board.builder()
			.contentType(contentType)
			.title(request.title())
			.content(request.content())
			.animal(savedAnimal)
			.member(new Member(memberId))
			.build();

		boardRepository.save(board);

		List<String> presignedUrls = List.of();
		if (request.count() != null && request.count() > 0) {
			presignedUrls = imageService.generatePresignedUrls(savedAnimal.getId(), request.count());
		}

		return new AnimalBoardResponse(presignedUrls);
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

		CursorPage<BoardResponse, Long> cursorPage = CursorPage.of(
			responses,
			request.size(),
			BoardResponse::boardId
		);

		Long totalCount = boardRepository.getAnimalBoardsCount(contentType);

		return BoardListResponse.of(totalCount.intValue(), cursorPage);
	}
}
