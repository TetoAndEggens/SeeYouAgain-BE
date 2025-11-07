package tetoandeggens.seeyouagainbe.animal.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.AbandonedAnimal;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.repository.AbandonedAnimalRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.ServiceTest;

@DisplayName("AbandonedAnimalService 통합 테스트")
class AbandonedAnimalServiceTest extends ServiceTest {

	@Autowired
	private AbandonedAnimalService abandonedAnimalService;

	@Autowired
	private AbandonedAnimalRepository abandonedAnimalRepository;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		abandonedAnimalRepository.deleteAll();
	}

	@Nested
	@DisplayName("유기 동물 리스트 조회 테스트")
	class GetAbandonedAnimalListTests {

		@Test
		@DisplayName("유기 동물 리스트 조회 - 필터 없이 전체 조회 성공")
		void getAbandonedAnimalList_Success_WithoutFilters() {
			// given
			AbandonedAnimal animal1 = createAbandonedAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			AbandonedAnimal animal2 = createAbandonedAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			abandonedAnimalRepository.saveAll(List.of(animal1, animal2));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(
				request,
				SortDirection.LATEST,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
			);

			// then
			assertThat(response.abandonedAnimalCount()).isEqualTo(2);
			assertThat(response.abandonedAnimal().getData()).hasSize(2);
			assertThat(response.abandonedAnimal().isHasNext()).isFalse();
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - breedType 필터로 조회 성공")
		void getAbandonedAnimalList_Success_WithBreedTypeFilter() {
			// given
			AbandonedAnimal chihuahuaAnimal = createAbandonedAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			AbandonedAnimal catAnimal = createAbandonedAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			abandonedAnimalRepository.saveAll(List.of(chihuahuaAnimal, catAnimal));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(
				request,
				SortDirection.LATEST,
				null,
				null,
				null,
				"치와와",
				null,
				null,
				null,
				null
			);

			// then
			assertThat(response.abandonedAnimalCount()).isEqualTo(1);
			assertThat(response.abandonedAnimal().getData()).hasSize(1);
			assertThat(response.abandonedAnimal().getData().get(0).breedType()).isEqualTo("치와와");
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - sex 필터로 조회 성공")
		void getAbandonedAnimalList_Success_WithSexFilter() {
			// given
			AbandonedAnimal male = createAbandonedAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			AbandonedAnimal female = createAbandonedAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			abandonedAnimalRepository.saveAll(List.of(male, female));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(
				request,
				SortDirection.LATEST,
				null,
				null,
				null,
				null,
				null,
				Sex.M,
				null,
				null
			);

			// then
			assertThat(response.abandonedAnimalCount()).isEqualTo(1);
			assertThat(response.abandonedAnimal().getData()).hasSize(1);
			assertThat(response.abandonedAnimal().getData().get(0).sex()).isEqualTo(Sex.M);
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - 여러 필터 조합으로 조회 성공")
		void getAbandonedAnimalList_Success_WithMultipleFilters() {
			// given
			AbandonedAnimal matchingAnimal = createAbandonedAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			AbandonedAnimal nonMatchingAnimal = createAbandonedAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"부산광역시",
				"해운대구"
			);
			abandonedAnimalRepository.saveAll(List.of(matchingAnimal, nonMatchingAnimal));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(
				request,
				SortDirection.LATEST,
				null,
				null,
				Species.DOG,
				null,
				NeuteredState.Y,
				Sex.M,
				"서울특별시",
				"강남구"
			);

			// then
			assertThat(response.abandonedAnimalCount()).isEqualTo(1);
			assertThat(response.abandonedAnimal().getData()).hasSize(1);
			assertThat(response.abandonedAnimal().getData().get(0).species()).isEqualTo(Species.DOG);
			assertThat(response.abandonedAnimal().getData().get(0).sex()).isEqualTo(Sex.M);
			assertThat(response.abandonedAnimal().getData().get(0).city()).isEqualTo("서울특별시");
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - LATEST 정렬로 조회 성공")
		void getAbandonedAnimalList_Success_WithLatestSort() {
			// given
			AbandonedAnimal animal1 = createAbandonedAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			AbandonedAnimal animal2 = createAbandonedAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			abandonedAnimalRepository.saveAll(List.of(animal1, animal2));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(
				request,
				SortDirection.LATEST,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
			);

			// then
			assertThat(response.abandonedAnimal().getData()).hasSize(2);
			List<Long> ids = response.abandonedAnimal().getData().stream()
				.map(AbandonedAnimalResponse::abandonedAnimalId)
				.toList();
			assertThat(ids).isSortedAccordingTo((a, b) -> b.compareTo(a)); // 내림차순
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - OLDEST 정렬로 조회 성공")
		void getAbandonedAnimalList_Success_WithOldestSort() {
			// given
			AbandonedAnimal animal1 = createAbandonedAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			AbandonedAnimal animal2 = createAbandonedAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			abandonedAnimalRepository.saveAll(List.of(animal1, animal2));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(
				request,
				SortDirection.OLDEST,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
			);

			// then
			assertThat(response.abandonedAnimal().getData()).hasSize(2);
			List<Long> ids = response.abandonedAnimal().getData().stream()
				.map(AbandonedAnimalResponse::abandonedAnimalId)
				.toList();
			assertThat(ids).isSortedAccordingTo(Long::compareTo); // 오름차순
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - 조건에 맞는 데이터가 없으면 빈 리스트 반환")
		void getAbandonedAnimalList_ReturnsEmptyList_WhenNoDataMatches() {
			// given
			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AbandonedAnimalListResponse response = abandonedAnimalService.getAbandonedAnimalList(
				request,
				SortDirection.LATEST,
				null,
				null,
				Species.DOG,
				null,
				null,
				null,
				null,
				null
			);

			// then
			assertThat(response.abandonedAnimalCount()).isZero();
			assertThat(response.abandonedAnimal().getData()).isEmpty();
			assertThat(response.abandonedAnimal().isHasNext()).isFalse();
		}
	}

	private AbandonedAnimal createAbandonedAnimal(
		String desertionNo,
		LocalDate happenDate,
		Species species,
		String breedTypeName,
		Sex sex,
		NeuteredState neuteredState,
		String city,
		String town
	) {
		BreedType breedType = null;
		if (breedTypeName != null && !breedTypeName.isBlank()) {
			breedType = BreedType.builder()
				.name(breedTypeName)
				.type(species.toString())
				.code(UUID.randomUUID().toString())
				.build();
			entityManager.persist(breedType);
			entityManager.flush();
		}

		return AbandonedAnimal.builder()
			.desertionNo(desertionNo)
			.happenDate(happenDate)
			.species(species)
			.breedType(breedType)
			.sex(sex)
			.neuteredState(neuteredState)
			.city(city)
			.town(town)
			.birth("2024년생")
			.processState("보호중")
			.build();
	}
}