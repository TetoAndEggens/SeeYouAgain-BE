package tetoandeggens.seeyouagainbe.animal.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.service.AnimalService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

@Tag(name = "Animal", description = "유기 동물 관련 API")
@RestController
@RequestMapping("/animal")
@RequiredArgsConstructor
public class AnimalController {

	private final AnimalService animalService;

	@GetMapping
	@Operation(
		summary = "유기 동물 리스트 조회 API",
		description = "유기 동물 리스트 조회 - 커서 페이징 적용")
	public ApiResponse<AnimalListResponse> getAnimalList(
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
		AnimalListResponse response = animalService.getAbandonedAnimalList(request, sortDirection,
			startDate, endDate, species, breedType, neuteredState, sex, city, town);

		return ApiResponse.ok(response);
	}

	@GetMapping("/{animalId}")
	@Operation(
		summary = "유기 동물 조회 API",
		description = "유기 동물 조회")
	public ApiResponse<AnimalDetailResponse> getAnimal(
		@PathVariable Long animalId
	) {
		AnimalDetailResponse response = animalService.getAnimal(animalId);

		return ApiResponse.ok(response);
	}

	@GetMapping("/map")
	@Operation(
		summary = "좌표 기준 범위 내, 유기 동물들 조회 API",
		description = "좌표 기준 범위 내, 유기 동물들 조회 - 커서 페이징 적용")
	public ApiResponse<AnimalListResponse> getAnimalListWithCoordinates(
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection,
		@RequestParam Double minLongitude,
		@RequestParam Double minLatitude,
		@RequestParam Double maxLongitude,
		@RequestParam Double maxLatitude,
		@RequestParam(required = false) AnimalType animalType,
		@RequestParam(required = false) String startDate,
		@RequestParam(required = false) String endDate,
		@RequestParam(required = false) Species species,
		@RequestParam(required = false) String breedType,
		@RequestParam(required = false) NeuteredState neuteredState,
		@RequestParam(required = false) Sex sex,
		@RequestParam(required = false) String city,
		@RequestParam(required = false) String town
	) {
		AnimalListResponse response = animalService.getAnimalListWithCoordinates(
			request, sortDirection, animalType, minLongitude, minLatitude, maxLongitude, maxLatitude, startDate,
			endDate, species, breedType, neuteredState, sex, city, town
		);

		return ApiResponse.ok(response);
	}
}