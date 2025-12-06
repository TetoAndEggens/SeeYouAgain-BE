package tetoandeggens.seeyouagainbe.animal.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;
import tetoandeggens.seeyouagainbe.animal.entity.BreedType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@DisplayName("BookMarkRepository QueryDSL 커스텀 메서드 테스트")
class BookMarkRepositoryTest extends RepositoryTest {

    @Autowired
    private BookMarkRepository bookMarkRepository;

    @Autowired
    private EntityManager entityManager;

    private Member testMember1;
    private Member testMember2;
    private Animal testAnimal1;
    private Animal testAnimal2;
    private BreedType chihuahua;
    private BreedType poodle;
    private AnimalLocation location;

    @BeforeEach
    void setUp() {
        bookMarkRepository.deleteAll();

        // Member 생성
        testMember1 = Member.builder()
                .loginId("testuser1")
                .password("password")
                .nickName("테스트유저1")
                .phoneNumber("010-1234-5678")
                .build();
        entityManager.persist(testMember1);

        testMember2 = Member.builder()
                .loginId("testuser2")
                .password("password")
                .nickName("테스트유저2")
                .phoneNumber("010-8765-4321")
                .build();
        entityManager.persist(testMember2);

        // BreedType 생성
        chihuahua = BreedType.builder()
                .name("치와와")
                .type("DOG")
                .code(UUID.randomUUID().toString())
                .build();
        entityManager.persist(chihuahua);

        poodle = BreedType.builder()
                .name("푸들")
                .type("DOG")
                .code(UUID.randomUUID().toString())
                .build();
        entityManager.persist(poodle);

        // AnimalLocation 생성
        location = AnimalLocation.builder()
                .name("서울 유기견 보호소")
                .address("서울특별시 강남구 테헤란로 123")
                .centerNo("CENTER001")
                .latitude(37.4979)
                .longitude(127.0276)
                .build();
        entityManager.persist(location);

        // Animal 생성
        testAnimal1 = Animal.builder()
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
        entityManager.persist(testAnimal1);

        testAnimal2 = Animal.builder()
                .desertionNo("ABANDONED20241202001")
                .happenDate(LocalDate.of(2024, 12, 2))
                .happenPlace("서울특별시 강남구 선릉역")
                .animalType(AnimalType.ABANDONED)
                .city("서울특별시")
                .town("강남구")
                .species(Species.DOG)
                .breedType(poodle)
                .color("흰색")
                .birth("2023년생")
                .weight("3.8(Kg)")
                .noticeNo("서울-강남-2024-00002")
                .noticeStartDate("2024-12-02")
                .noticeEndDate("2024-12-16")
                .processState("보호중")
                .sex(Sex.F)
                .neuteredState(NeuteredState.N)
                .specialMark("꼬리가 짧음")
                .centerPhone("02-1234-5678")
                .animalLocation(location)
                .build();
        entityManager.persist(testAnimal2);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findAllByMemberIdAndNotDeleted 테스트")
    class FindAllByMemberIdAndNotDeletedTests {

        @Test
        @DisplayName("회원의 삭제되지 않은 북마크 목록 조회 - 성공")
        void findAllByMemberIdAndNotDeleted_Success() {
            // given
            BookMark bookmark1 = BookMark.builder()
                    .member(new Member(testMember1.getId()))
                    .animal(testAnimal1)
                    .build();
            bookMarkRepository.save(bookmark1);

            BookMark bookmark2 = BookMark.builder()
                    .member(new Member(testMember1.getId()))
                    .animal(testAnimal2)
                    .build();
            bookMarkRepository.save(bookmark2);

            entityManager.flush();
            entityManager.clear();

            // when
            List<BookMark> results = bookMarkRepository.findAllByMemberIdAndNotDeleted(testMember1.getId());

            // then
            assertThat(results).hasSize(2);
            assertThat(results)
                    .extracting(bookMark -> bookMark.getAnimal().getId())
                    .containsExactlyInAnyOrder(testAnimal1.getId(), testAnimal2.getId());

            // fetch join 검증: Animal과 BreedType이 이미 로드되어 있어야 함
            assertThat(results.get(0).getAnimal()).isNotNull();
            assertThat(results.get(0).getAnimal().getBreedType()).isNotNull();
        }

        @Test
        @DisplayName("삭제된 북마크는 조회되지 않음")
        void findAllByMemberIdAndNotDeleted_ExcludesDeletedBookmarks() {
            // given
            BookMark activeBookmark = BookMark.builder()
                    .member(new Member(testMember1.getId()))
                    .animal(testAnimal1)
                    .build();
            bookMarkRepository.save(activeBookmark);

            BookMark deletedBookmark = BookMark.builder()
                    .member(new Member(testMember1.getId()))
                    .animal(testAnimal2)
                    .build();
            bookMarkRepository.save(deletedBookmark);

            // 삭제 처리
            deletedBookmark.toggleDelete();
            bookMarkRepository.save(deletedBookmark);

            entityManager.flush();
            entityManager.clear();

            // when
            List<BookMark> results = bookMarkRepository.findAllByMemberIdAndNotDeleted(testMember1.getId());

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAnimal().getId()).isEqualTo(testAnimal1.getId());
        }

        @Test
        @DisplayName("북마크가 없으면 빈 리스트 반환")
        void findAllByMemberIdAndNotDeleted_ReturnsEmptyList_WhenNoBookmarks() {
            // when
            List<BookMark> results = bookMarkRepository.findAllByMemberIdAndNotDeleted(testMember1.getId());

            // then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("다른 회원의 북마크는 조회되지 않음")
        void findAllByMemberIdAndNotDeleted_OnlyReturnsOwnBookmarks() {
            // given
            BookMark member1Bookmark = BookMark.builder()
                    .member(new Member(testMember1.getId()))
                    .animal(testAnimal1)
                    .build();
            bookMarkRepository.save(member1Bookmark);

            BookMark member2Bookmark = BookMark.builder()
                    .member(new Member(testMember2.getId()))
                    .animal(testAnimal2)
                    .build();
            bookMarkRepository.save(member2Bookmark);

            entityManager.flush();
            entityManager.clear();

            // when
            List<BookMark> results = bookMarkRepository.findAllByMemberIdAndNotDeleted(testMember1.getId());

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAnimal().getId()).isEqualTo(testAnimal1.getId());
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndAnimalId 테스트")
    class FindByMemberIdAndAnimalIdTests {

        @Test
        @DisplayName("회원과 동물 ID로 북마크 조회 - 성공")
        void findByMemberIdAndAnimalId_Success() {
            // given
            BookMark bookmark = BookMark.builder()
                    .member(new Member(testMember1.getId()))
                    .animal(testAnimal1)
                    .build();
            bookMarkRepository.save(bookmark);

            entityManager.flush();
            entityManager.clear();

            // when
            Optional<BookMark> result = bookMarkRepository.findByMemberIdAndAnimalId(
                    testMember1.getId(),
                    testAnimal1.getId()
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getAnimal().getId()).isEqualTo(testAnimal1.getId());
        }

        @Test
        @DisplayName("삭제된 북마크도 조회됨 (토글 기능을 위해)")
        void findByMemberIdAndAnimalId_ReturnsDeletedBookmark() {
            // given
            BookMark bookmark = BookMark.builder()
                    .member(new Member(testMember1.getId()))
                    .animal(testAnimal1)
                    .build();
            bookMarkRepository.save(bookmark);

            // 삭제 처리
            bookmark.toggleDelete();
            bookMarkRepository.save(bookmark);

            entityManager.flush();
            entityManager.clear();

            // when
            Optional<BookMark> result = bookMarkRepository.findByMemberIdAndAnimalId(
                    testMember1.getId(),
                    testAnimal1.getId()
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("북마크가 없으면 Optional.empty 반환")
        void findByMemberIdAndAnimalId_ReturnsEmpty_WhenNotExists() {
            // when
            Optional<BookMark> result = bookMarkRepository.findByMemberIdAndAnimalId(
                    testMember1.getId(),
                    testAnimal1.getId()
            );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 회원의 북마크는 조회되지 않음")
        void findByMemberIdAndAnimalId_DoesNotReturnOtherMembersBookmark() {
            // given
            BookMark member2Bookmark = BookMark.builder()
                    .member(new Member(testMember2.getId()))
                    .animal(testAnimal1)
                    .build();
            bookMarkRepository.save(member2Bookmark);

            entityManager.flush();
            entityManager.clear();

            // when
            Optional<BookMark> result = bookMarkRepository.findByMemberIdAndAnimalId(
                    testMember1.getId(),
                    testAnimal1.getId()
            );

            // then
            assertThat(result).isEmpty();
        }
    }
}