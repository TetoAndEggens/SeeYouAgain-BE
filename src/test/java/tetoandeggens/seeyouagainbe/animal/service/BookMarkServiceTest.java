package tetoandeggens.seeyouagainbe.animal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalRepository;
import tetoandeggens.seeyouagainbe.animal.repository.BookMarkRepository;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BookMarkErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@DisplayName("BookMarkService 단위 테스트")
class BookMarkServiceTest extends ServiceTest {

    @Autowired
    private BookMarkService bookMarkService;

    @MockitoBean
    private BookMarkRepository bookMarkRepository;

    @MockitoBean
    private AnimalRepository animalRepository;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_ANIMAL_ID = 100L;

    private Animal abandonedAnimal;
    private Animal missingAnimal;
    private BreedType chihuahua;
    private AnimalLocation location;

    @BeforeEach
    void setUp() {

        chihuahua = BreedType.builder()
                .name("치와와")
                .type("DOG")
                .code(UUID.randomUUID().toString())
                .build();

        location = AnimalLocation.builder()
                .name("서울 유기견 보호소")
                .address("서울특별시 강남구 테헤란로 123")
                .centerNo("CENTER001")
                .latitude(37.4979)
                .longitude(127.0276)
                .build();

        abandonedAnimal = Animal.builder()
                .desertionNo("ABANDONED20241201001")
                .happenDate(LocalDate.of(2024, 12, 1))
                .happenPlace("서울특별시 강남구 역삼동")
                .animalType(AnimalType.ABANDONED)
                .city("서울특별시")
                .town("강남구")
                .species(Species.DOG)
                .breedType(chihuahua)
                .color("갈색")
                .birth("2022년생")
                .weight("2.5(Kg)")
                .noticeNo("서울-강남-2024-00001")
                .noticeStartDate("2024-12-01")
                .noticeEndDate("2024-12-15")
                .processState("보호중")
                .sex(Sex.M)
                .neuteredState(NeuteredState.Y)
                .specialMark("오른쪽 귀에 작은 흰 반점")
                .centerPhone("02-1234-5678")
                .animalLocation(location)
                .build();

        missingAnimal = Animal.builder()
                .desertionNo("MISSING20241206001")
                .happenDate(LocalDate.of(2024, 12, 6))
                .happenPlace("서울특별시 강남구 논현동")
                .animalType(AnimalType.MISSING)
                .city("서울특별시")
                .town("강남구")
                .species(Species.DOG)
                .breedType(chihuahua)
                .color("검정색")
                .birth("2021년생")
                .processState("실종")
                .sex(Sex.F)
                .neuteredState(NeuteredState.Y)
                .specialMark("목에 빨간색 목걸이")
                .animalLocation(location)
                .build();
    }

    @Nested
    @DisplayName("getMyBookMarks 테스트")
    class GetMyBookMarksTests {

        @Test
        @DisplayName("북마크 목록 조회 - DTO 기반 성공")
        void getMyBookMarks_Success() {

            BookMarkAnimalResponse dto1 = new BookMarkAnimalResponse(
                    1L,
                    10L,
                    Species.DOG,
                    "치와와",
                    "보호중"
            );

            BookMarkAnimalResponse dto2 = new BookMarkAnimalResponse(
                    2L,
                    11L,
                    Species.DOG,
                    "치와와",
                    "보호중"
            );

            given(bookMarkRepository.findAllByMemberIdAndNotDeleted(TEST_MEMBER_ID))
                    .willReturn(List.of(dto1, dto2));

            List<BookMarkAnimalResponse> results = bookMarkService.getMyBookMarks(TEST_MEMBER_ID);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).species()).isEqualTo(Species.DOG);
            assertThat(results.get(0).breedType()).isEqualTo("치와와");
            assertThat(results.get(0).processState()).isEqualTo("보호중");

            verify(bookMarkRepository).findAllByMemberIdAndNotDeleted(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("북마크가 없으면 빈 리스트 반환")
        void getMyBookMarks_ReturnsEmptyList() {

            given(bookMarkRepository.findAllByMemberIdAndNotDeleted(TEST_MEMBER_ID))
                    .willReturn(List.of());

            List<BookMarkAnimalResponse> results = bookMarkService.getMyBookMarks(TEST_MEMBER_ID);

            assertThat(results).isEmpty();
            verify(bookMarkRepository).findAllByMemberIdAndNotDeleted(TEST_MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("toggleBookMark 테스트")
    class ToggleBookMarkTests {

        @Test
        @DisplayName("북마크 추가 - 성공 (북마크가 없을 때)")
        void toggleBookMark_AddBookmark_Success() {
            // given
            given(animalRepository.findById(TEST_ANIMAL_ID))
                    .willReturn(Optional.of(abandonedAnimal));
            given(bookMarkRepository.findByMemberIdAndAnimalId(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .willReturn(Optional.empty());

            BookMark newBookmark = BookMark.builder()
                    .member(new Member(TEST_MEMBER_ID))
                    .animal(abandonedAnimal)
                    .build();

            given(bookMarkRepository.save(any(BookMark.class)))
                    .willReturn(newBookmark);

            // when
            bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID);

            // then
            verify(animalRepository).findById(TEST_ANIMAL_ID);
            verify(bookMarkRepository).findByMemberIdAndAnimalId(TEST_MEMBER_ID, TEST_ANIMAL_ID);
            verify(bookMarkRepository).save(any(BookMark.class));
        }

        @Test
        @DisplayName("북마크 토글 - 성공 (기존 북마크가 있을 때)")
        void toggleBookMark_ToggleExistingBookmark_Success() {
            // given
            BookMark existingBookmark = BookMark.builder()
                    .member(new Member(TEST_MEMBER_ID))
                    .animal(abandonedAnimal)
                    .build();

            given(animalRepository.findById(TEST_ANIMAL_ID))
                    .willReturn(Optional.of(abandonedAnimal));
            given(bookMarkRepository.findByMemberIdAndAnimalId(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .willReturn(Optional.of(existingBookmark));

            // when
            bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID);

            // then
            verify(animalRepository).findById(TEST_ANIMAL_ID);
            verify(bookMarkRepository).findByMemberIdAndAnimalId(TEST_MEMBER_ID, TEST_ANIMAL_ID);
        }

        @Test
        @DisplayName("동물이 존재하지 않으면 예외 발생")
        void toggleBookMark_Fail_AnimalNotFound() {

            given(animalRepository.findById(TEST_ANIMAL_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnimalErrorCode.ANIMAL_NOT_FOUND);

            verify(bookMarkRepository, never()).findByMemberIdAndAnimalId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("실종 동물(MISSING)은 북마크할 수 없음")
        void toggleBookMark_Fail_MissingAnimalCannotBeBookmarked() {

            given(animalRepository.findById(TEST_ANIMAL_ID))
                    .willReturn(Optional.of(missingAnimal));

            assertThatThrownBy(() -> bookMarkService.toggleBookMark(TEST_MEMBER_ID, TEST_ANIMAL_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BookMarkErrorCode.ONLY_ABANDONED_ANIMAL_CAN_BE_BOOKMARKED);

            verify(bookMarkRepository, never()).findByMemberIdAndAnimalId(anyLong(), anyLong());
        }
    }
}
