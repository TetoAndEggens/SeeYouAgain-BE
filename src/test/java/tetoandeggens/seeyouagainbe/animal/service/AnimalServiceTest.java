package tetoandeggens.seeyouagainbe.animal.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;

@DisplayName("AnimalService 통합 테스트")
class AnimalServiceTest extends ServiceTest {

	@Autowired
	private AnimalService animalService;

	@Autowired
	private AnimalRepository animalRepository;

	@Autowired
	private EntityManager entityManager;

	private final GeometryFactory geometryFactory = new GeometryFactory();

	@BeforeEach
	void setUp() {
		animalRepository.deleteAll();
	}

	@Nested
	@DisplayName("유기 동물 리스트 조회 테스트")
	class GetAnimalListTests {

		@Test
		@DisplayName("유기 동물 리스트 조회 - 필터 없이 전체 조회 성공")
		void getAnimalList_Success_WithoutFilters() {
			// given
			Animal animal1 = createAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			Animal animal2 = createAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			animalRepository.saveAll(List.of(animal1, animal2));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AnimalListResponse response = animalService.getAnimalList(
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
			assertThat(response.animalCount()).isEqualTo(2);
			assertThat(response.animal().getData()).hasSize(2);
			assertThat(response.animal().isHasNext()).isFalse();
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - breedType 필터로 조회 성공")
		void getAnimalList_Success_WithBreedTypeFilter() {
			// given
			Animal chihuahuaAnimal = createAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			Animal catAnimal = createAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			animalRepository.saveAll(List.of(chihuahuaAnimal, catAnimal));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AnimalListResponse response = animalService.getAnimalList(
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
			assertThat(response.animalCount()).isEqualTo(1);
			assertThat(response.animal().getData()).hasSize(1);
			assertThat(response.animal().getData().get(0).breedType()).isEqualTo("치와와");
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - sex 필터로 조회 성공")
		void getAnimalList_Success_WithSexFilter() {
			// given
			Animal male = createAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			Animal female = createAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			animalRepository.saveAll(List.of(male, female));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AnimalListResponse response = animalService.getAnimalList(
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
			assertThat(response.animalCount()).isEqualTo(1);
			assertThat(response.animal().getData()).hasSize(1);
			assertThat(response.animal().getData().get(0).sex()).isEqualTo(Sex.M);
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - 여러 필터 조합으로 조회 성공")
		void getAnimalList_Success_WithMultipleFilters() {
			// given
			Animal matchingAnimal = createAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			Animal nonMatchingAnimal = createAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"부산광역시",
				"해운대구"
			);
			animalRepository.saveAll(List.of(matchingAnimal, nonMatchingAnimal));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AnimalListResponse response = animalService.getAnimalList(
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
			assertThat(response.animalCount()).isEqualTo(1);
			assertThat(response.animal().getData()).hasSize(1);
			assertThat(response.animal().getData().get(0).species()).isEqualTo(Species.DOG);
			assertThat(response.animal().getData().get(0).sex()).isEqualTo(Sex.M);
			assertThat(response.animal().getData().get(0).city()).isEqualTo("서울특별시");
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - LATEST 정렬로 조회 성공")
		void getAnimalList_Success_WithLatestSort() {
			// given
			Animal animal1 = createAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			Animal animal2 = createAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			animalRepository.saveAll(List.of(animal1, animal2));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AnimalListResponse response = animalService.getAnimalList(
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
			assertThat(response.animal().getData()).hasSize(2);
			List<Long> ids = response.animal().getData().stream()
				.map(AnimalResponse::animalId)
				.toList();
			assertThat(ids).isSortedAccordingTo((a, b) -> b.compareTo(a)); // 내림차순
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - OLDEST 정렬로 조회 성공")
		void getAnimalList_Success_WithOldestSort() {
			// given
			Animal animal1 = createAnimal(
				"12345",
				LocalDate.of(2025, 1, 1),
				Species.DOG,
				"치와와",
				Sex.M,
				NeuteredState.Y,
				"서울특별시",
				"강남구"
			);
			Animal animal2 = createAnimal(
				"12346",
				LocalDate.of(2025, 1, 2),
				Species.CAT,
				"코리안 숏헤어",
				Sex.F,
				NeuteredState.N,
				"서울특별시",
				"서초구"
			);
			animalRepository.saveAll(List.of(animal1, animal2));

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AnimalListResponse response = animalService.getAnimalList(
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
			assertThat(response.animal().getData()).hasSize(2);
			List<Long> ids = response.animal().getData().stream()
				.map(AnimalResponse::animalId)
				.toList();
			assertThat(ids).isSortedAccordingTo(Long::compareTo); // 오름차순
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - 조건에 맞는 데이터가 없으면 빈 리스트 반환")
		void getAnimalList_ReturnsEmptyList_WhenNoDataMatches() {
			// given
			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			AnimalListResponse response = animalService.getAnimalList(
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
			assertThat(response.animalCount()).isZero();
			assertThat(response.animal().getData()).isEmpty();
			assertThat(response.animal().isHasNext()).isFalse();
		}
	}

	private Animal createAnimal(
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

		return Animal.builder()
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

	@Nested
	@DisplayName("유기 동물 상세 조회 테스트")
	class GetAnimalTests {

		@Test
		@DisplayName("유기 동물 상세 조회 - 성공")
		void getAnimal_Success() {
			// given
			Point point = geometryFactory.createPoint(new Coordinate(127.0276, 37.4979));
			point.setSRID(4326);

			AnimalLocation animalLocation = AnimalLocation.builder()
				.name("서울 유기견 보호소")
				.address("서울특별시 강남구")
				.centerNo("CENTER001")
				.coordinates(point)
				.build();
			entityManager.persist(animalLocation);

			Animal animal = Animal.builder()
				.desertionNo("12345")
				.happenDate(LocalDate.of(2025, 1, 1))
				.happenPlace("서울특별시 강남구 테헤란로")
				.species(Species.DOG)
				.breedType(createBreedType("푸들", "DOG"))
				.sex(Sex.M)
				.neuteredState(NeuteredState.Y)
				.city("서울특별시")
				.town("강남구")
				.birth("2024년생")
				.processState("보호중")
				.color("흰색")
				.noticeNo("경북-경주-2025-01056")
				.noticeStartDate("2025-01-01")
				.noticeEndDate("2025-12-31")
				.specialMark("귀엽다")
				.weight("3.3(Kg)")
				.centerPhone("02-1234-5678")
				.animalLocation(animalLocation)
				.build();
			animalRepository.save(animal);

			entityManager.flush();
			entityManager.clear();

			// when
			AnimalDetailResponse response = animalService.getAnimal(
				animal.getId());

			// then
			assertThat(response).isNotNull();
			assertThat(response.animalId()).isEqualTo(animal.getId());
			assertThat(response.happenDate()).isEqualTo(LocalDate.of(2025, 1, 1));
			assertThat(response.happenPlace()).isEqualTo("서울특별시 강남구 테헤란로");
			assertThat(response.species()).isEqualTo(Species.DOG);
			assertThat(response.breedType()).isEqualTo("푸들");
			assertThat(response.sex()).isEqualTo(Sex.M);
			assertThat(response.processState()).isEqualTo("보호중");
			assertThat(response.color()).isEqualTo("흰색");
			assertThat(response.noticeNo()).isEqualTo("경북-경주-2025-01056");
			assertThat(response.noticeStartDate()).isEqualTo("2025-01-01");
			assertThat(response.noticeEndDate()).isEqualTo("2025-12-31");
			assertThat(response.specialMark()).isEqualTo("귀엽다");
			assertThat(response.weight()).isEqualTo("3.3(Kg)");
			assertThat(response.neuteredState()).isEqualTo(NeuteredState.Y);
			assertThat(response.centerName()).isEqualTo("서울 유기견 보호소");
			assertThat(response.centerAddress()).isEqualTo("서울특별시 강남구");
			assertThat(response.centerPhone()).isEqualTo("02-1234-5678");
		}

		@Test
		@DisplayName("유기 동물 상세 조회 - 존재하지 않는 ID로 조회시 예외 발생")
		void getAnimal_ThrowsException_WhenNotExists() {
			// when & then
			assertThatThrownBy(() -> animalService.getAnimal(999L))
				.isInstanceOf(tetoandeggens.seeyouagainbe.global.exception.CustomException.class)
				.hasMessageContaining(AnimalErrorCode.ANIMAL_NOT_FOUND.getMessage());
		}
	}

	private BreedType createBreedType(String name, String type) {
		BreedType breedType = BreedType.builder()
			.name(name)
			.type(type)
			.code(UUID.randomUUID().toString())
			.build();
		entityManager.persist(breedType);
		entityManager.flush();
		return breedType;
	}
}