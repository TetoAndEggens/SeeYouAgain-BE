package tetoandeggens.seeyouagainbe.notification.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;

@DisplayName("NotificationKeywordRepository 단위 테스트")
class NotificationKeywordRepositoryTest extends RepositoryTest {

    private static final String TEST_KEYWORD_BREED = "골든리트리버";
    private static final String TEST_KEYWORD_REGION = "서울";

    @Autowired
    private NotificationKeywordRepository notificationKeywordRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .loginId("testuser")
                .nickName("테스트유저")
                .phoneNumber("01012345678")
                .password("encodedPassword")

                .build());
    }

    @Nested
    @DisplayName("키워드 저장 테스트")
    class SaveKeywordTests {

        @Test
        @DisplayName("키워드 저장 - 성공 (품종 타입)")
        void saveKeyword_Success_Breed() {
            // given
            NotificationKeyword keyword = NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build();

            // when
            NotificationKeyword savedKeyword = notificationKeywordRepository.save(keyword);

            // then
            assertThat(savedKeyword.getId()).isNotNull();
            assertThat(savedKeyword.getKeyword()).isEqualTo(TEST_KEYWORD_BREED);
            assertThat(savedKeyword.getKeywordType()).isEqualTo(KeywordType.ABANDONED);
            assertThat(savedKeyword.getKeywordCategoryType()).isEqualTo(KeywordCategoryType.BREED);
            assertThat(savedKeyword.getMember().getId()).isEqualTo(testMember.getId());
        }

        @Test
        @DisplayName("키워드 저장 - 성공 (지역 타입)")
        void saveKeyword_Success_Region() {
            // given
            NotificationKeyword keyword = NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_REGION)
                    .keywordType(KeywordType.WITNESS)
                    .keywordCategoryType(KeywordCategoryType.LOCATION)
                    .member(testMember)
                    .build();

            // when
            NotificationKeyword savedKeyword = notificationKeywordRepository.save(keyword);

            // then
            assertThat(savedKeyword.getKeyword()).isEqualTo(TEST_KEYWORD_REGION);
            assertThat(savedKeyword.getKeywordType()).isEqualTo(KeywordType.WITNESS);
            assertThat(savedKeyword.getKeywordCategoryType()).isEqualTo(KeywordCategoryType.LOCATION);
        }

        @Test
        @DisplayName("키워드 저장 - 같은 회원이 여러 키워드 등록 가능")
        void saveMultipleKeywords_Success() {
            // given
            NotificationKeyword keyword1 = NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build();

            NotificationKeyword keyword2 = NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_REGION)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.LOCATION)
                    .member(testMember)
                    .build();

            // when
            notificationKeywordRepository.save(keyword1);
            notificationKeywordRepository.save(keyword2);

            // then
            List<NotificationKeyword> keywords = notificationKeywordRepository
                    .findAllByMemberId(testMember.getId());
            assertThat(keywords).hasSize(2);
        }
    }

    @Nested
    @DisplayName("키워드 조회 테스트")
    class FindKeywordTests {

        @Test
        @DisplayName("회원 ID로 모든 키워드 조회 - 성공")
        void findAllByMemberId_Success() {
            // given
            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build());

            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_REGION)
                    .keywordType(KeywordType.WITNESS)
                    .keywordCategoryType(KeywordCategoryType.LOCATION)
                    .member(testMember)
                    .build());

            // when
            List<NotificationKeyword> keywords = notificationKeywordRepository
                    .findAllByMemberId(testMember.getId());

            // then
            assertThat(keywords).hasSize(2);
            assertThat(keywords).extracting(NotificationKeyword::getKeyword)
                    .containsExactlyInAnyOrder(TEST_KEYWORD_BREED, TEST_KEYWORD_REGION);
        }

        @Test
        @DisplayName("회원 ID로 키워드 조회 - 키워드가 없으면 빈 리스트 반환")
        void findAllByMemberId_ReturnsEmptyList_WhenNoKeywords() {
            // when
            List<NotificationKeyword> keywords = notificationKeywordRepository
                    .findAllByMemberId(testMember.getId());

            // then
            assertThat(keywords).isEmpty();
        }

        @Test
        @DisplayName("키워드 ID와 회원 ID로 조회 - 성공")
        void findByIdAndMemberId_Success() {
            // given
            NotificationKeyword keyword = notificationKeywordRepository.save(
                    NotificationKeyword.builder()
                            .keyword(TEST_KEYWORD_BREED)
                            .keywordType(KeywordType.ABANDONED)
                            .keywordCategoryType(KeywordCategoryType.BREED)
                            .member(testMember)
                            .build());

            // when
            Optional<NotificationKeyword> foundKeyword = notificationKeywordRepository
                    .findByIdAndMemberId(keyword.getId(), testMember.getId());

            // then
            assertThat(foundKeyword).isPresent();
            assertThat(foundKeyword.get().getKeyword()).isEqualTo(TEST_KEYWORD_BREED);
        }

        @Test
        @DisplayName("키워드 ID와 회원 ID로 조회 - 다른 회원의 키워드면 조회 불가")
        void findByIdAndMemberId_ReturnsEmpty_WhenDifferentMember() {
            // given
            Member anotherMember = memberRepository.save(Member.builder()
                    .loginId("anotheruser")
                    .nickName("다른유저")
                    .phoneNumber("01087654321")
                    .password("encodedPassword")

                    .build());

            NotificationKeyword keyword = notificationKeywordRepository.save(
                    NotificationKeyword.builder()
                            .keyword(TEST_KEYWORD_BREED)
                            .keywordType(KeywordType.ABANDONED)
                            .keywordCategoryType(KeywordCategoryType.BREED)
                            .member(testMember)
                            .build());

            // when
            Optional<NotificationKeyword> foundKeyword = notificationKeywordRepository
                    .findByIdAndMemberId(keyword.getId(), anotherMember.getId());

            // then
            assertThat(foundKeyword).isEmpty();
        }
    }

    @Nested
    @DisplayName("키워드 중복 확인 테스트")
    class ExistsKeywordTests {

        @Test
        @DisplayName("키워드 중복 확인 - 이미 존재하면 true 반환")
        void existsByKeywordConditions_ReturnsTrue_WhenExists() {
            // given
            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build());

            // when
            boolean exists = notificationKeywordRepository
                    .existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                            testMember.getId(),
                            TEST_KEYWORD_BREED,
                            KeywordType.ABANDONED,
                            KeywordCategoryType.BREED
                    );

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("키워드 중복 확인 - 존재하지 않으면 false 반환")
        void existsByKeywordConditions_ReturnsFalse_WhenNotExists() {
            // when
            boolean exists = notificationKeywordRepository
                    .existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                            testMember.getId(),
                            TEST_KEYWORD_BREED,
                            KeywordType.ABANDONED,
                            KeywordCategoryType.BREED
                    );

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("키워드 중복 확인 - 같은 키워드라도 타입이 다르면 false 반환")
        void existsByKeywordConditions_ReturnsFalse_WhenDifferentType() {
            // given
            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build());

            // when
            boolean exists = notificationKeywordRepository
                    .existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                            testMember.getId(),
                            TEST_KEYWORD_BREED,
                            KeywordType.WITNESS, // 다른 타입
                            KeywordCategoryType.BREED
                    );

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("키워드 중복 확인 - 같은 키워드라도 카테고리가 다르면 false 반환")
        void existsByKeywordConditions_ReturnsFalse_WhenDifferentCategory() {
            // given
            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build());

            // when
            boolean exists = notificationKeywordRepository
                    .existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                            testMember.getId(),
                            TEST_KEYWORD_BREED,
                            KeywordType.ABANDONED,
                            KeywordCategoryType.LOCATION // 다른 카테고리
                    );

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("키워드 중복 확인 - 다른 회원이 같은 키워드를 가지고 있어도 false 반환")
        void existsByKeywordConditions_ReturnsFalse_WhenDifferentMember() {
            // given
            Member anotherMember = memberRepository.save(Member.builder()
                    .loginId("anotheruser")
                    .nickName("다른유저")
                    .phoneNumber("01087654321")
                    .password("encodedPassword")

                    .build());

            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(anotherMember)
                    .build());

            // when
            boolean exists = notificationKeywordRepository
                    .existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                            testMember.getId(), // 다른 회원
                            TEST_KEYWORD_BREED,
                            KeywordType.ABANDONED,
                            KeywordCategoryType.BREED
                    );

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("키워드 삭제 테스트")
    class DeleteKeywordTests {

        @Test
        @DisplayName("키워드 삭제 - 성공")
        void deleteKeyword_Success() {
            // given
            NotificationKeyword keyword = notificationKeywordRepository.save(
                    NotificationKeyword.builder()
                            .keyword(TEST_KEYWORD_BREED)
                            .keywordType(KeywordType.ABANDONED)
                            .keywordCategoryType(KeywordCategoryType.BREED)
                            .member(testMember)
                            .build());

            Long keywordId = keyword.getId();

            // when
            notificationKeywordRepository.delete(keyword);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<NotificationKeyword> deletedKeyword = notificationKeywordRepository
                    .findById(keywordId);
            assertThat(deletedKeyword).isEmpty();
        }

        @Test
        @DisplayName("키워드 삭제 - 여러 키워드 중 하나만 삭제")
        void deleteOneKeyword_Success() {
            // given
            NotificationKeyword keyword1 = notificationKeywordRepository.save(
                    NotificationKeyword.builder()
                            .keyword(TEST_KEYWORD_BREED)
                            .keywordType(KeywordType.ABANDONED)
                            .keywordCategoryType(KeywordCategoryType.BREED)
                            .member(testMember)
                            .build());

            NotificationKeyword keyword2 = notificationKeywordRepository.save(
                    NotificationKeyword.builder()
                            .keyword(TEST_KEYWORD_REGION)
                            .keywordType(KeywordType.WITNESS)
                            .keywordCategoryType(KeywordCategoryType.LOCATION)
                            .member(testMember)
                            .build());

            // when
            notificationKeywordRepository.delete(keyword1);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<NotificationKeyword> deletedKeyword = notificationKeywordRepository
                    .findById(keyword1.getId());
            Optional<NotificationKeyword> remainingKeyword = notificationKeywordRepository
                    .findById(keyword2.getId());

            assertThat(deletedKeyword).isEmpty();
            assertThat(remainingKeyword).isPresent();
        }
    }

    @Nested
    @DisplayName("키워드 연관관계 테스트")
    class KeywordRelationshipTests {

        @Test
        @DisplayName("회원과 키워드의 연관관계 - Member 삭제시 외래키 제약조건 확인")
        void deleteMember_FailsWithForeignKeyConstraint() {
            // given
            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build());

            entityManager.flush();
            entityManager.clear();

            Long memberId = testMember.getId();

            // when & then - 키워드가 존재하는 상태에서 Member 삭제시 제약조건 위반 예외 발생
            assertThatThrownBy(() -> {
                Member managedMember = memberRepository.findById(memberId).orElseThrow();
                memberRepository.delete(managedMember);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("회원과 키워드의 연관관계 - Member 함께 조회")
        void findKeywordWithMember_Success() {
            // given
            NotificationKeyword keyword = notificationKeywordRepository.save(
                    NotificationKeyword.builder()
                            .keyword(TEST_KEYWORD_BREED)
                            .keywordType(KeywordType.ABANDONED)
                            .keywordCategoryType(KeywordCategoryType.BREED)
                            .member(testMember)
                            .build());

            entityManager.flush();
            entityManager.clear();

            // when
            NotificationKeyword foundKeyword = notificationKeywordRepository
                    .findById(keyword.getId()).orElseThrow();

            // then
            assertThat(foundKeyword.getMember()).isNotNull();
            assertThat(foundKeyword.getMember().getId()).isEqualTo(testMember.getId());
            assertThat(foundKeyword.getMember().getNickName()).isEqualTo("테스트유저");
        }
    }

    @Nested
    @DisplayName("KeywordType별 조회 테스트")
    class FindByKeywordTypeTests {

        @Test
        @DisplayName("ABANDONED 타입 키워드만 조회")
        void findByKeywordType_Abandoned() {
            // given
            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build());

            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_REGION)
                    .keywordType(KeywordType.WITNESS)
                    .keywordCategoryType(KeywordCategoryType.LOCATION)
                    .member(testMember)
                    .build());

            // when
            List<NotificationKeyword> allKeywords = notificationKeywordRepository
                    .findAllByMemberId(testMember.getId());

            long abandonedAnimalCount = allKeywords.stream()
                    .filter(k -> k.getKeywordType() == KeywordType.ABANDONED)
                    .count();

            // then
            assertThat(abandonedAnimalCount).isEqualTo(1);
        }

        @Test
        @DisplayName("WITNESS 타입 키워드만 조회")
        void findByKeywordType_Witness() {
            // given
            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_BREED)
                    .keywordType(KeywordType.ABANDONED)
                    .keywordCategoryType(KeywordCategoryType.BREED)
                    .member(testMember)
                    .build());

            notificationKeywordRepository.save(NotificationKeyword.builder()
                    .keyword(TEST_KEYWORD_REGION)
                    .keywordType(KeywordType.WITNESS)
                    .keywordCategoryType(KeywordCategoryType.LOCATION)
                    .member(testMember)
                    .build());

            // when
            List<NotificationKeyword> allKeywords = notificationKeywordRepository
                    .findAllByMemberId(testMember.getId());

            long missingAnimalCount = allKeywords.stream()
                    .filter(k -> k.getKeywordType() == KeywordType.WITNESS)
                    .count();

            // then
            assertThat(missingAnimalCount).isEqualTo(1);
        }
    }
}