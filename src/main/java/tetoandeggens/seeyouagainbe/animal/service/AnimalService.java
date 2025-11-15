package tetoandeggens.seeyouagainbe.animal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;

@Service
@RequiredArgsConstructor
public class AnimalService {

	private final AnimalRepository animalRepository;

	@Transactional(readOnly = true)
	public AnimalListResponse getAbandonedAnimalList(CursorPageRequest request, SortDirection sortDirection,
		String startDate, String endDate, Species species, String breedType, NeuteredState neuteredState, Sex sex,
		String city,
		String town) {

		List<AnimalResponse> responses = animalRepository.getAbandonedAnimals(request, sortDirection,
			AnimalType.ABANDONED,
			startDate, endDate, species, breedType, neuteredState, sex, city, town);

		CursorPage<AnimalResponse, Long> cursorPage = CursorPage.of(
			responses,
			request.size(),
			AnimalResponse::animalId
		);

		Long totalCount = animalRepository.getAbandonedAnimalsCount(AnimalType.ABANDONED, startDate, endDate, species,
			breedType,
			neuteredState, sex, city, town);

		return AnimalListResponse.of(totalCount.intValue(), cursorPage);
	}

	@Transactional(readOnly = true)
	public AnimalDetailResponse getAnimal(Long animalId) {
		AnimalDetailResponse response = animalRepository.getAnimal(animalId);

		if (response == null) {
			throw new CustomException(AnimalErrorCode.ANIMAL_NOT_FOUND);
		}

		return response;
	}

	@Transactional(readOnly = true)
	public AnimalListResponse getAnimalListWithCoordinates(
		CursorPageRequest request, SortDirection sortDirection, AnimalType animalType, Double minLongitude,
		Double minLatitude, Double maxLongitude, Double maxLatitude, String startDate, String endDate, Species species,
		String breedType, NeuteredState neuteredState, Sex sex, String city, String town
	) {
		List<AnimalResponse> responses = animalRepository.getAnimalListWithCoordinates(
			request, sortDirection, animalType, minLongitude, minLatitude, maxLongitude, maxLatitude,
			startDate, endDate, species, breedType, neuteredState, sex, city, town
		);

		CursorPage<AnimalResponse, Long> cursorPage = CursorPage.of(
			responses,
			request.size(),
			AnimalResponse::animalId
		);

		Long totalCount = animalRepository.getAnimalsCountWithCoordinates(
			animalType, minLongitude, minLatitude, maxLongitude, maxLatitude,
			startDate, endDate, species, breedType, neuteredState, sex, city, town
		);

		return AnimalListResponse.of(totalCount.intValue(), cursorPage);
	}
}