package tetoandeggens.seeyouagainbe.animal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.repository.AbandonedAnimalRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AbandonedAnimalErrorCode;

@Service
@RequiredArgsConstructor
public class AbandonedAnimalService {

	private final AbandonedAnimalRepository abandonedAnimalRepository;

	@Transactional(readOnly = true)
	public AbandonedAnimalListResponse getAbandonedAnimalList(CursorPageRequest request, SortDirection sortDirection,
		String startDate, String endDate, Species species, String breedType, NeuteredState neuteredState, Sex sex,
		String city,
		String town) {

		List<AbandonedAnimalResponse> responses = abandonedAnimalRepository.getAbandonedAnimals(request, sortDirection,
			startDate, endDate, species, breedType, neuteredState, sex, city, town);

		CursorPage<AbandonedAnimalResponse, Long> cursorPage = CursorPage.of(
			responses,
			request.size(),
			AbandonedAnimalResponse::abandonedAnimalId
		);

		Long totalCount = abandonedAnimalRepository.getAbandonedAnimalsCount(startDate, endDate, species, breedType,
			neuteredState, sex, city, town);

		return AbandonedAnimalListResponse.of(totalCount.intValue(), cursorPage);
	}

	@Transactional(readOnly = true)
	public AbandonedAnimalDetailResponse getAbandonedAnimal(Long abandonedAnimalId) {
		AbandonedAnimalDetailResponse response = abandonedAnimalRepository.getAbandonedAnimal(abandonedAnimalId);

		if (response == null) {
			throw new CustomException(AbandonedAnimalErrorCode.ABANDONED_ANIMAL_NOT_FOUND);
		}

		return response;
	}
}
