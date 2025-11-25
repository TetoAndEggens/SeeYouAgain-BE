package tetoandeggens.seeyouagainbe.animal.repository.custom;

import java.util.List;

import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

public interface AnimalRepositoryCustom {

	List<AnimalResponse> getAbandonedAnimals(CursorPageRequest request, SortDirection sortDirection,
		AnimalType animalType,
		String startDate, String endDate, Species species, String breedType, NeuteredState neuteredState, Sex sex,
		String city,
		String town, Long memberId);

	Long getAbandonedAnimalsCount(AnimalType animalType, String startDate, String endDate, Species species,
		String breedType,
		NeuteredState neuteredState,
		Sex sex, String city, String town);

	AnimalDetailResponse getAnimal(Long animalId, Long memberId);

	List<AnimalResponse> getAnimalListWithCoordinates(
		CursorPageRequest request, SortDirection sortDirection, AnimalType animalType, Double minLongitude,
		Double minLatitude,
		Double maxLongitude, Double maxLatitude, String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town, Long memberId
	);

	Long getAnimalsCountWithCoordinates(
		AnimalType animalType, Double minLongitude, Double minLatitude, Double maxLongitude, Double maxLatitude,
		String startDate,
		String endDate, Species species, String breedType, NeuteredState neuteredState, Sex sex, String city,
		String town
	);
}