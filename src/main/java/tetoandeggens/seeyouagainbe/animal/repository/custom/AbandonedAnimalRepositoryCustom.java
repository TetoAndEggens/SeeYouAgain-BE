package tetoandeggens.seeyouagainbe.animal.repository.custom;

import java.util.List;

import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

public interface AbandonedAnimalRepositoryCustom {

	List<AbandonedAnimalResponse> getAbandonedAnimals(CursorPageRequest request, SortDirection sortDirection,
		String startDate, String endDate, Species species, String breedType, NeuteredState neuteredState, Sex sex,
		String city,
		String town);

	Long getAbandonedAnimalsCount(String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState,
		Sex sex, String city, String town);
}
