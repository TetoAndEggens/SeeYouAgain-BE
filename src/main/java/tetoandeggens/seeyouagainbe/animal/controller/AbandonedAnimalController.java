package tetoandeggens.seeyouagainbe.animal.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.service.AbandonedAnimalService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

@Tag(name = "Abandoned Animal", description = "유기 동물 관련 API")
@RestController
@RequestMapping("/abandoned-animal")
@RequiredArgsConstructor
public class AbandonedAnimalController {

	private final AbandonedAnimalService abandonedAnimalService;

	@GetMapping
	@Operation(
		summary = "유기 동물 리스트 조회 API",
		description = "유기 동물 리스트 조회 - 커서 페이징 적용")
	public ApiResponse<AbandonedAnimalListResponse> getAbandonedAnimalList(
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection,
		@RequestParam(required = false) String startDate,
		@RequestParam(required = false) String endDate,
		@RequestParam(required = false) Species species,
		@RequestParam(required = false) String breedType,
		@RequestParam(required = false) NeuteredState neuteredState,
		@RequestParam(required = false) Sex sex,
		@RequestParam(required = false) String city,
		@RequestParam(required = false) String town
	) {
		AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(request, sortDirection,
			startDate, endDate, species, breedType, neuteredState, sex, city, town);

		return ApiResponse.ok(response);
	}
}
