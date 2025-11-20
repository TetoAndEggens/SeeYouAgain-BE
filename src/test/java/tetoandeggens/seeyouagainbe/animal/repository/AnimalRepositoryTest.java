package tetoandeggens.seeyouagainbe.animal.repository;

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
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalS3Profile;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;

@DisplayName("AnimalRepository QueryDSL 복잡한 쿼리 테스트")
class AnimalRepositoryTest extends RepositoryTest {

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
	@DisplayName("복잡한 leftJoin 및 서브쿼리 테스트")
	class ComplexQueryTests {

		@Test
		@DisplayName("breedType leftJoin - breedType이 null인 동물 조회 성공")
		void getAnimals_Success_WithBreedTypeLeftJoin() {
			// given
			createAndSaveAnimalWithBreed("12345", LocalDate.of(2025, 1, 1), Species.DOG, "푸들", Sex.M,
				NeuteredState.Y, "서울특별시", "강남구");
			createAndSaveAnimal("12346", LocalDate.of(2025, 1, 2), Species.DOG, Sex.M, NeuteredState.Y,
				"서울특별시", "강남구");

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			List<AnimalResponse> results = animalRepository.getAbandonedAnimals(
				request, SortDirection.LATEST, null, null, null, null, null, null, null, null, null
			);

			// then
			assertThat(results).hasSize(2);
			assertThat(results.stream().filter(r -> r.breedType() != null).count()).isEqualTo(1);
			assertThat(results.stream().filter(r -> r.breedType() == null).count()).isEqualTo(1);
		}

		@Test
		@DisplayName("profile 서브쿼리 leftJoin - 가장 작은 ID의 프로필 조회 성공")
		void getAnimals_Success_WithProfileSubqueryLeftJoin() {
			// given
			Animal animal = createAndSaveAnimal("12345", LocalDate.of(2025, 1, 1), Species.DOG, Sex.M,
				NeuteredState.Y, "서울특별시", "강남구");

			createAndSaveProfile(animal, "profile3.jpg");
			createAndSaveProfile(animal, "profile1.jpg");
			createAndSaveProfile(animal, "profile2.jpg");

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			List<AnimalResponse> results = animalRepository.getAbandonedAnimals(
				request, SortDirection.LATEST, null, null, null, null, null, null, null, null, null
			);

			// then
			assertThat(results).hasSize(1);
			assertThat(results.get(0).profile()).isEqualTo("profile3.jpg");
		}

		@Test
		@DisplayName("breedType과 profile 동시 leftJoin - 모두 null인 경우 조회 성공")
		void getAnimals_Success_WithBothLeftJoins() {
			// given
			Animal animalWithAll = createAndSaveAnimalWithBreed("12345", LocalDate.of(2025, 1, 1),
				Species.DOG, "치와와", Sex.M, NeuteredState.Y, "서울특별시", "강남구");
			createAndSaveProfile(animalWithAll, "profile1.jpg");

			Animal animalWithoutBreed = createAndSaveAnimal("12346", LocalDate.of(2025, 1, 2), Species.CAT,
				Sex.F, NeuteredState.N, "서울특별시", "서초구");
			createAndSaveProfile(animalWithoutBreed, "profile2.jpg");

			Animal animalWithoutProfile = createAndSaveAnimalWithBreed("12347", LocalDate.of(2025, 1, 3),
				Species.DOG, "푸들", Sex.M, NeuteredState.Y, "부산광역시", "해운대구");

			Animal animalWithNothing = createAndSaveAnimal("12348", LocalDate.of(2025, 1, 4), Species.CAT,
				Sex.F, NeuteredState.N, "인천광역시", "남동구");

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			List<AnimalResponse> results = animalRepository.getAbandonedAnimals(
				request, SortDirection.LATEST, null, null, null, null, null, null, null, null, null
			);

			// then
			assertThat(results).hasSize(4);

			AnimalResponse withAll = results.stream()
				.filter(r -> r.animalId().equals(animalWithAll.getId()))
				.findFirst().orElseThrow();
			assertThat(withAll.breedType()).isEqualTo("치와와");
			assertThat(withAll.profile()).isEqualTo("profile1.jpg");

			AnimalResponse withoutBreed = results.stream()
				.filter(r -> r.animalId().equals(animalWithoutBreed.getId()))
				.findFirst().orElseThrow();
			assertThat(withoutBreed.breedType()).isNull();
			assertThat(withoutBreed.profile()).isEqualTo("profile2.jpg");

			AnimalResponse withoutProfile = results.stream()
				.filter(r -> r.animalId().equals(animalWithoutProfile.getId()))
				.findFirst().orElseThrow();
			assertThat(withoutProfile.breedType()).isEqualTo("푸들");
			assertThat(withoutProfile.profile()).isNull();

			AnimalResponse withNothing = results.stream()
				.filter(r -> r.animalId().equals(animalWithNothing.getId()))
				.findFirst().orElseThrow();
			assertThat(withNothing.breedType()).isNull();
			assertThat(withNothing.profile()).isNull();
		}

		@Test
		@DisplayName("profile 서브쿼리 - 여러 동물의 각 최소 ID 프로필 조회 성공")
		void getAnimals_Success_WithMultipleAnimalsProfileSubquery() {
			// given
			Animal animal1 = createAndSaveAnimal("12345", LocalDate.of(2025, 1, 1), Species.DOG, Sex.M,
				NeuteredState.Y, "서울특별시", "강남구");
			createAndSaveProfile(animal1, "animal1_profile3.jpg");
			createAndSaveProfile(animal1, "animal1_profile1.jpg");
			createAndSaveProfile(animal1, "animal1_profile2.jpg");

			Animal animal2 = createAndSaveAnimal("12346", LocalDate.of(2025, 1, 2), Species.CAT, Sex.F,
				NeuteredState.N, "서울특별시", "서초구");
			createAndSaveProfile(animal2, "animal2_profile2.jpg");
			createAndSaveProfile(animal2, "animal2_profile1.jpg");

			entityManager.flush();
			entityManager.clear();

			CursorPageRequest request = new CursorPageRequest(null, 10);

			// when
			List<AnimalResponse> results = animalRepository.getAbandonedAnimals(
				request, SortDirection.LATEST, null, null, null, null, null, null, null, null, null
			);

			// then
			assertThat(results).hasSize(2);

			AnimalResponse result1 = results.stream()
				.filter(r -> r.animalId().equals(animal1.getId()))
				.findFirst().orElseThrow();
			assertThat(result1.profile()).isEqualTo("animal1_profile3.jpg");

			AnimalResponse result2 = results.stream()
				.filter(r -> r.animalId().equals(animal2.getId()))
				.findFirst().orElseThrow();
			assertThat(result2.profile()).isEqualTo("animal2_profile2.jpg");
		}
	}

	private Animal createAndSaveAnimal(String desertionNo, LocalDate happenDate, Species species, Sex sex,
		NeuteredState neuteredState, String city, String town) {
		Animal animal = Animal.builder()
			.desertionNo(desertionNo)
			.happenDate(happenDate)
			.species(species)
			.sex(sex)
			.neuteredState(neuteredState)
			.city(city)
			.town(town)
			.birth("2024년생")
			.processState("보호중")
			.build();

		return animalRepository.save(animal);
	}

	private Animal createAndSaveAnimalWithBreed(String desertionNo, LocalDate happenDate, Species species,
		String breedTypeName, Sex sex, NeuteredState neuteredState, String city, String town) {
		BreedType breedType = BreedType.builder()
			.name(breedTypeName)
			.type(species.toString())
			.code(UUID.randomUUID().toString())
			.build();
		entityManager.persist(breedType);
		entityManager.flush();

		Animal animal = Animal.builder()
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

		return animalRepository.save(animal);
	}

	private void createAndSaveProfile(Animal animal, String profileUrl) {
		AnimalS3Profile profile = AnimalS3Profile.builder()
			.animal(animal)
			.profile(profileUrl)
			.build();
		entityManager.persist(profile);
	}

	@Nested
	@DisplayName("유기 동물 상세 조회 테스트")
	class GetAnimalTests {

		@Test
		@DisplayName("유기 동물 상세 조회 - 모든 정보 포함 성공")
		void getAnimal_Success_WithAllInformation() {
			// given
			Point point = geometryFactory.createPoint(new Coordinate(127.0276, 37.4979));
			point.setSRID(4326);

			AnimalLocation animalLocation = AnimalLocation.builder()
				.name("서울 유기견 보호소")
				.address("서울특별시 강남구")
				.centerNo("CENTER001")
				.latitude(37.4979)
				.longitude(127.0276)
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

			createAndSaveProfile(animal, "profile1.jpg");
			createAndSaveProfile(animal, "profile2.jpg");
			createAndSaveProfile(animal, "profile3.jpg");

			entityManager.flush();
			entityManager.clear();

			// when
			AnimalDetailResponse result = animalRepository.getAnimal(
				animal.getId());

			// then
			assertThat(result).isNotNull();
			assertThat(result.animalId()).isEqualTo(animal.getId());
			assertThat(result.happenDate()).isEqualTo(LocalDate.of(2025, 1, 1));
			assertThat(result.happenPlace()).isEqualTo("서울특별시 강남구 테헤란로");
			assertThat(result.species()).isEqualTo(Species.DOG);
			assertThat(result.breedType()).isEqualTo("푸들");
			assertThat(result.sex()).isEqualTo(Sex.M);
			assertThat(result.processState()).isEqualTo("보호중");
			assertThat(result.profiles()).hasSize(3);
			assertThat(result.profiles()).containsExactly("profile1.jpg", "profile2.jpg", "profile3.jpg");
			assertThat(result.color()).isEqualTo("흰색");
			assertThat(result.noticeNo()).isEqualTo("경북-경주-2025-01056");
			assertThat(result.noticeStartDate()).isEqualTo("2025-01-01");
			assertThat(result.noticeEndDate()).isEqualTo("2025-12-31");
			assertThat(result.specialMark()).isEqualTo("귀엽다");
			assertThat(result.weight()).isEqualTo("3.3(Kg)");
			assertThat(result.neuteredState()).isEqualTo(NeuteredState.Y);
			assertThat(result.centerName()).isEqualTo("서울 유기견 보호소");
			assertThat(result.centerAddress()).isEqualTo("서울특별시 강남구");
			assertThat(result.centerPhone()).isEqualTo("02-1234-5678");
		}

		@Test
		@DisplayName("유기 동물 상세 조회 - 존재하지 않는 ID로 조회시 null 반환")
		void getAnimal_ReturnsNull_WhenNotExists() {
			// when
			AnimalDetailResponse result = animalRepository.getAnimal(
				999L);

			// then
			assertThat(result).isNull();
		}
	}

	private BreedType createBreedType(String name, String type) {
		BreedType breedType = BreedType.builder()
			.name(name)
			.type(type)
			.code(java.util.UUID.randomUUID().toString())
			.build();
		entityManager.persist(breedType);
		entityManager.flush();
		return breedType;
	}
}