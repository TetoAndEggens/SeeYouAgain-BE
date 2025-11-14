package tetoandeggens.seeyouagainbe.animal.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.service.AnimalService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.ControllerTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;

@WebMvcTest(controllers = AnimalController.class)
@DisplayName("Animal 컨트롤러 테스트")
class AnimalControllerTest extends ControllerTest {

	@MockitoBean
	private AnimalService animalService;

	@Nested
	@DisplayName("유기 동물 리스트 조회 API 테스트")
	class GetAnimalListTests {

		@Test
		@DisplayName("유기 동물 리스트 조회 - 필수 파라미터만으로 성공")
		void getAnimalList_Success_WithRequiredParamsOnly() throws Exception {
			// given
			AnimalResponse animalResponse = AnimalResponse.builder()
				.animalId(1L)
				.happenDate(LocalDate.of(2025, 1, 1))
				.species(Species.DOG)
				.breedType("치와와")
				.birth("2024년생")
				.city("서울특별시")
				.town("강남구")
				.sex(Sex.M)
				.processState("보호중")
				.profile("https://profile.com")
				.build();

			CursorPage<AnimalResponse, Long> cursorPage = CursorPage.of(
				List.of(animalResponse),
				10,
				AnimalResponse::animalId
			);

			AnimalListResponse response = AnimalListResponse.of(1, cursorPage);

			given(animalService.getAnimalList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/animal")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.animalCount").value(1))
				.andExpect(jsonPath("$.data.animal.data[0].animalId").value(1))
				.andExpect(jsonPath("$.data.animal.data[0].species").value("DOG"))
				.andExpect(jsonPath("$.data.animal.data[0].breedType").value("치와와"));

			verify(animalService).getAnimalList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull()
			);
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - 모든 필터 파라미터 포함 성공")
		void getAnimalList_Success_WithAllFilters() throws Exception {
			// given
			AnimalResponse animalResponse = AnimalResponse.builder()
				.animalId(1L)
				.happenDate(LocalDate.of(2025, 1, 1))
				.species(Species.DOG)
				.breedType("치와와")
				.birth("2024년생")
				.city("서울특별시")
				.town("강남구")
				.sex(Sex.M)
				.processState("보호중")
				.profile("https://profile.com")
				.build();

			CursorPage<AnimalResponse, Long> cursorPage = CursorPage.of(
				List.of(animalResponse),
				10,
				AnimalResponse::animalId
			);

			AnimalListResponse response = AnimalListResponse.of(1, cursorPage);

			given(animalService.getAnimalList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				anyString(),
				anyString(),
				any(Species.class),
				anyString(),
				any(NeuteredState.class),
				any(Sex.class),
				anyString(),
				anyString()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/animal")
					.param("size", "10")
					.param("sortDirection", "LATEST")
					.param("startDate", "20250101")
					.param("endDate", "20250131")
					.param("species", "DOG")
					.param("breedType", "치와와")
					.param("neuteredState", "Y")
					.param("sex", "M")
					.param("city", "서울특별시")
					.param("town", "강남구"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.animalCount").value(1));

			verify(animalService).getAnimalList(
				any(CursorPageRequest.class),
				eq(SortDirection.LATEST),
				eq("20250101"),
				eq("20250131"),
				eq(Species.DOG),
				eq("치와와"),
				eq(NeuteredState.Y),
				eq(Sex.M),
				eq("서울특별시"),
				eq("강남구")
			);
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - 커서 ID와 함께 성공")
		void getAnimalList_Success_WithCursorId() throws Exception {
			// given
			AnimalResponse animalResponse = AnimalResponse.builder()
				.animalId(5L)
				.happenDate(LocalDate.of(2025, 1, 1))
				.species(Species.CAT)
				.breedType("코리안 숏헤어")
				.birth("2024년생")
				.city("서울특별시")
				.town("강남구")
				.sex(Sex.F)
				.processState("보호중")
				.profile("https://profile.com")
				.build();

			CursorPage<AnimalResponse, Long> cursorPage = CursorPage.of(
				List.of(animalResponse),
				10,
				AnimalResponse::animalId
			);

			AnimalListResponse response = AnimalListResponse.of(1, cursorPage);

			given(animalService.getAnimalList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/animal")
					.param("cursorId", "10")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(animalService).getAnimalList(
				any(CursorPageRequest.class),
				any(SortDirection.class),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull()
			);
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - OLDEST 정렬로 성공")
		void getAnimalList_Success_WithOldestSort() throws Exception {
			// given
			AnimalListResponse response = AnimalListResponse.of(
				0,
				CursorPage.of(List.of(), 10, AnimalResponse::animalId)
			);

			given(animalService.getAnimalList(
				any(CursorPageRequest.class),
				eq(SortDirection.OLDEST),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/animal")
					.param("size", "10")
					.param("sortDirection", "OLDEST"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(animalService).getAnimalList(
				any(CursorPageRequest.class),
				eq(SortDirection.OLDEST),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull()
			);
		}

		@Test
		@DisplayName("유기 동물 리스트 조회 - size 파라미터 없으면 실패")
		void getAnimalList_Fail_WithoutSize() throws Exception {
			// when & then
			mockMvc.perform(get("/animal"))
				.andExpect(status().isBadRequest());

			verify(animalService, never()).getAnimalList(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any()
			);
		}
	}

	@Nested
	@DisplayName("유기 동물 상세 조회 API 테스트")
	class GetAnimalTests {

		@Test
		@DisplayName("유기 동물 상세 조회 - 성공")
		void getAnimal_Success() throws Exception {
			// given
			Long animalId = 1L;
			AnimalDetailResponse response = AnimalDetailResponse.builder()
				.animalId(animalId)
				.happenDate(LocalDate.of(2025, 1, 1))
				.species(Species.DOG)
				.breedType("푸들")
				.birth("2024년생")
				.happenPlace("서울특별시 강남구 테헤란로")
				.sex(Sex.M)
				.processState("보호중")
				.profiles(List.of("profile1.jpg", "profile2.jpg", "profile3.jpg"))
				.color("흰색")
				.noticeNo("경북-경주-2025-01056")
				.noticeStartDate("2025-01-01")
				.noticeEndDate("2025-12-31")
				.specialMark("귀엽다")
				.weight("3.3(Kg)")
				.neuteredState(NeuteredState.Y)
				.centerName("서울 유기견 보호소")
				.centerAddress("서울특별시 강남구")
				.centerPhone("02-1234-5678")
				.build();

			given(animalService.getAnimal(animalId))
				.willReturn(response);

			// when & then
			mockMvc.perform(get("/animal/{animalId}", animalId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.animalId").value(animalId))
				.andExpect(jsonPath("$.data.happenDate").value("2025-01-01"))
				.andExpect(jsonPath("$.data.species").value("DOG"))
				.andExpect(jsonPath("$.data.breedType").value("푸들"))
				.andExpect(jsonPath("$.data.birth").value("2024년생"))
				.andExpect(jsonPath("$.data.happenPlace").value("서울특별시 강남구 테헤란로"))
				.andExpect(jsonPath("$.data.sex").value("M"))
				.andExpect(jsonPath("$.data.processState").value("보호중"))
				.andExpect(jsonPath("$.data.profiles").isArray())
				.andExpect(jsonPath("$.data.profiles.length()").value(3))
				.andExpect(jsonPath("$.data.profiles[0]").value("profile1.jpg"))
				.andExpect(jsonPath("$.data.profiles[1]").value("profile2.jpg"))
				.andExpect(jsonPath("$.data.profiles[2]").value("profile3.jpg"))
				.andExpect(jsonPath("$.data.color").value("흰색"))
				.andExpect(jsonPath("$.data.noticeNo").value("경북-경주-2025-01056"))
				.andExpect(jsonPath("$.data.noticeStartDate").value("2025-01-01"))
				.andExpect(jsonPath("$.data.noticeEndDate").value("2025-12-31"))
				.andExpect(jsonPath("$.data.specialMark").value("귀엽다"))
				.andExpect(jsonPath("$.data.weight").value("3.3(Kg)"))
				.andExpect(jsonPath("$.data.neuteredState").value("Y"))
				.andExpect(jsonPath("$.data.centerName").value("서울 유기견 보호소"))
				.andExpect(jsonPath("$.data.centerAddress").value("서울특별시 강남구"))
				.andExpect(jsonPath("$.data.centerPhone").value("02-1234-5678"));

			verify(animalService).getAnimal(animalId);
		}

		@Test
		@DisplayName("유기 동물 상세 조회 - 존재하지 않는 ID로 조회시 예외 발생")
		void getAnimal_Fail_WhenNotExists() throws Exception {
			// given
			Long animalId = 999L;

			given(animalService.getAnimal(animalId))
				.willThrow(new CustomException(AnimalErrorCode.ANIMAL_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/animal/{animalId}", animalId))
				.andExpect(status().isNotFound());

			verify(animalService).getAnimal(animalId);
		}
	}

	@Nested
	@DisplayName("좌표 기반 유기 동물 리스트 조회 API 테스트")
	class GetAnimalListWithCoordinatesTests {

		@Test
		@DisplayName("좌표 기반 조회 - 필수 파라미터만으로 성공")
		void getAnimalListWithCoordinates_Success_WithRequiredParamsOnly() throws Exception {
			// given
			AnimalResponse animalResponse = AnimalResponse.builder()
				.animalId(1L)
				.happenDate(LocalDate.of(2025, 1, 1))
				.species(Species.DOG)
				.breedType("치와와")
				.birth("2024년생")
				.city("서울특별시")
				.town("중구")
				.sex(Sex.M)
				.processState("보호중")
				.profile("https://profile.com")
				.build();

			CursorPage<AnimalResponse, Long> cursorPage = CursorPage.of(
				List.of(animalResponse),
				10,
				AnimalResponse::animalId
			);

			AnimalListResponse response = AnimalListResponse.of(1, cursorPage);

			given(animalService.getAnimalListWithCoordinates(
				any(CursorPageRequest.class), any(SortDirection.class),
				anyDouble(), anyDouble(), anyDouble(), anyDouble(),
				isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/animal/map")
					.param("minLongitude", "126.8")
					.param("minLatitude", "37.4")
					.param("maxLongitude", "127.1")
					.param("maxLatitude", "37.7")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.animalCount").value(1))
				.andExpect(jsonPath("$.data.animal.data[0].animalId").value(1))
				.andExpect(jsonPath("$.data.animal.data[0].city").value("서울특별시"));

			verify(animalService).getAnimalListWithCoordinates(
				any(CursorPageRequest.class), any(SortDirection.class),
				eq(126.8), eq(37.4), eq(127.1), eq(37.7),
				isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
			);
		}

		@Test
		@DisplayName("좌표 기반 조회 - 모든 필터 파라미터 포함 성공")
		void getAnimalListWithCoordinates_Success_WithAllFilters() throws Exception {
			// given
			AnimalListResponse response = AnimalListResponse.of(
				1,
				CursorPage.of(List.of(), 10, AnimalResponse::animalId)
			);

			given(animalService.getAnimalListWithCoordinates(
				any(CursorPageRequest.class), any(SortDirection.class),
				anyDouble(), anyDouble(), anyDouble(), anyDouble(),
				anyString(), anyString(), any(Species.class), anyString(),
				any(NeuteredState.class), any(Sex.class), anyString(), anyString()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/animal/map")
					.param("minLongitude", "126.8")
					.param("minLatitude", "37.4")
					.param("maxLongitude", "127.1")
					.param("maxLatitude", "37.7")
					.param("size", "10")
					.param("keyword", "검색어")
					.param("startDate", "20250101")
					.param("endDate", "20250131")
					.param("species", "DOG")
					.param("breedType", "치와와")
					.param("neuteredState", "Y")
					.param("sex", "M")
					.param("city", "서울특별시")
					.param("town", "중구"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(animalService).getAnimalListWithCoordinates(
				any(CursorPageRequest.class), eq(SortDirection.LATEST),
				eq(126.8), eq(37.4), eq(127.1), eq(37.7),
				eq("20250101"), eq("20250131"), eq(Species.DOG), eq("치와와"),
				eq(NeuteredState.Y), eq(Sex.M), eq("서울특별시"), eq("중구")
			);
		}

		@Test
		@DisplayName("좌표 기반 조회 - size 파라미터 누락 시 실패")
		void getAnimalListWithCoordinates_Fail_WithoutSize() throws Exception {
			// when & then
			mockMvc.perform(get("/animal/map")
					.param("minLongitude", "126.8")
					.param("minLatitude", "37.4")
					.param("maxLongitude", "127.1")
					.param("maxLatitude", "37.7"))
				.andExpect(status().isBadRequest());

			verify(animalService, never()).getAnimalListWithCoordinates(
				any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
			);
		}

		@Test
		@DisplayName("좌표 기반 조회 - 커서 ID와 함께 성공")
		void getAnimalListWithCoordinates_Success_WithCursorId() throws Exception {
			// given
			AnimalListResponse response = AnimalListResponse.of(
				0,
				CursorPage.of(List.of(), 10, AnimalResponse::animalId)
			);

			given(animalService.getAnimalListWithCoordinates(
				any(CursorPageRequest.class), any(SortDirection.class),
				anyDouble(), anyDouble(), anyDouble(), anyDouble(),
				isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/animal/map")
					.param("cursorId", "10")
					.param("size", "10")
					.param("minLongitude", "126.8")
					.param("minLatitude", "37.4")
					.param("maxLongitude", "127.1")
					.param("maxLatitude", "37.7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(animalService).getAnimalListWithCoordinates(
				any(CursorPageRequest.class), any(SortDirection.class),
				anyDouble(), anyDouble(), anyDouble(), anyDouble(),
				isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
			);
		}
	}
}