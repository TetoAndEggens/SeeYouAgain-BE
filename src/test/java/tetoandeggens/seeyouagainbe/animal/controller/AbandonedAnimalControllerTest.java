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

import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalListResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.service.AbandonedAnimalService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.ControllerTest;

@WebMvcTest(controllers = AbandonedAnimalController.class)
@DisplayName("AbandonedAnimal 컨트롤러 테스트")
class AbandonedAnimalControllerTest extends ControllerTest {

	@MockitoBean
	private AbandonedAnimalService abandonedAnimalService;

	@Nested
	@DisplayName("유기 동물 리스트 조회 API 테스트")
	class GetAbandonedAnimalListTests {

		@Test
		@DisplayName("유기 동물 리스트 조회 - 필수 파라미터만으로 성공")
		void getAbandonedAnimalList_Success_WithRequiredParamsOnly() throws Exception {
			// given
			AbandonedAnimalResponse animalResponse = AbandonedAnimalResponse.builder()
				.abandonedAnimalId(1L)
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

			CursorPage<AbandonedAnimalResponse, Long> cursorPage = CursorPage.of(
				List.of(animalResponse),
				10,
				AbandonedAnimalResponse::abandonedAnimalId
			);

			AbandonedAnimalListResponse response = AbandonedAnimalListResponse.of(1, cursorPage);

			given(abandonedAnimalService.getAbandonedAnimalList(
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
			mockMvc.perform(get("/abandoned-animal")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.abandonedAnimalCount").value(1))
				.andExpect(jsonPath("$.data.abandonedAnimal.data[0].abandonedAnimalId").value(1))
				.andExpect(jsonPath("$.data.abandonedAnimal.data[0].species").value("DOG"))
				.andExpect(jsonPath("$.data.abandonedAnimal.data[0].breedType").value("치와와"));

			verify(abandonedAnimalService).getAbandonedAnimalList(
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
		void getAbandonedAnimalList_Success_WithAllFilters() throws Exception {
			// given
			AbandonedAnimalResponse animalResponse = AbandonedAnimalResponse.builder()
				.abandonedAnimalId(1L)
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

			CursorPage<AbandonedAnimalResponse, Long> cursorPage = CursorPage.of(
				List.of(animalResponse),
				10,
				AbandonedAnimalResponse::abandonedAnimalId
			);

			AbandonedAnimalListResponse response = AbandonedAnimalListResponse.of(1, cursorPage);

			given(abandonedAnimalService.getAbandonedAnimalList(
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
			mockMvc.perform(get("/abandoned-animal")
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
				.andExpect(jsonPath("$.data.abandonedAnimalCount").value(1));

			verify(abandonedAnimalService).getAbandonedAnimalList(
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
		void getAbandonedAnimalList_Success_WithCursorId() throws Exception {
			// given
			AbandonedAnimalResponse animalResponse = AbandonedAnimalResponse.builder()
				.abandonedAnimalId(5L)
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

			CursorPage<AbandonedAnimalResponse, Long> cursorPage = CursorPage.of(
				List.of(animalResponse),
				10,
				AbandonedAnimalResponse::abandonedAnimalId
			);

			AbandonedAnimalListResponse response = AbandonedAnimalListResponse.of(1, cursorPage);

			given(abandonedAnimalService.getAbandonedAnimalList(
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
			mockMvc.perform(get("/abandoned-animal")
					.param("cursorId", "10")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(abandonedAnimalService).getAbandonedAnimalList(
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
		void getAbandonedAnimalList_Success_WithOldestSort() throws Exception {
			// given
			AbandonedAnimalListResponse response = AbandonedAnimalListResponse.of(
				0,
				CursorPage.of(List.of(), 10, AbandonedAnimalResponse::abandonedAnimalId)
			);

			given(abandonedAnimalService.getAbandonedAnimalList(
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
			mockMvc.perform(get("/abandoned-animal")
					.param("size", "10")
					.param("sortDirection", "OLDEST"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(abandonedAnimalService).getAbandonedAnimalList(
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
		void getAbandonedAnimalList_Fail_WithoutSize() throws Exception {
			// when & then
			mockMvc.perform(get("/abandoned-animal"))
				.andExpect(status().isBadRequest());

			verify(abandonedAnimalService, never()).getAbandonedAnimalList(
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
}