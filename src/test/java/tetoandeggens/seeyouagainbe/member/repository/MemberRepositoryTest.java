package tetoandeggens.seeyouagainbe.member.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberRepository 단위 테스트")
class MemberRepositoryTest extends RepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private String testUuid;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        testUuid = UUID.randomUUID().toString();

        testMember = Member.builder()
                .loginId("testuser123")
                .password("encodedPassword")
                .nickName("테스트유저")
                .phoneNumber("01012345678")
                .build();

        ReflectionTestUtils.setField(testMember, "uuid", testUuid);
    }

    @Nested
    @DisplayName("loginId 조회 테스트")
    class FindByLoginIdTests {

        @Test
        @DisplayName("loginId로 활성 회원 조회 - 성공")
        void findByLoginIdAndIsDeletedFalse_Success() {
            // given
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByLoginIdAndIsDeletedFalse("testuser123");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getLoginId()).isEqualTo("testuser123");
            assertThat(result.get().getNickName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("loginId로 조회 - 삭제된 회원은 조회되지 않음")
        void findByLoginIdAndIsDeletedFalse_NotFound_WhenDeleted() {
            // given
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByLoginIdAndIsDeletedFalse("testuser123");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("loginId로 조회 - 존재하지 않는 loginId면 빈 Optional 반환")
        void findByLoginIdAndIsDeletedFalse_ReturnsEmpty_WhenNotExists() {
            // given
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByLoginIdAndIsDeletedFalse("nonexistent");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("ID 조회 테스트")
    class FindByIdTests {

        @Test
        @DisplayName("ID로 활성 회원 조회 - 성공")
        void findByIdAndIsDeletedFalse_Success() {
            // given
            Member saved = memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByIdAndIsDeletedFalse(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("ID로 조회 - 삭제된 회원은 조회되지 않음")
        void findByIdAndIsDeletedFalse_NotFound_WhenDeleted() {
            // given
            testMember.updateDeleteStatus();
            Member saved = memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByIdAndIsDeletedFalse(saved.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("UUID 조회 테스트")
    class FindByUuidTests {

        @Test
        @DisplayName("UUID로 활성 회원 조회 - 성공")
        void findByUuidAndIsDeletedFalse_Success() {
            // given
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByUuidAndIsDeletedFalse(testUuid);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUuid()).isEqualTo(testUuid);
        }

        @Test
        @DisplayName("UUID로 조회 - 삭제된 회원은 조회되지 않음")
        void findByUuidAndIsDeletedFalse_NotFound_WhenDeleted() {
            // given
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByUuidAndIsDeletedFalse(testUuid);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("UUID로 조회 - 존재하지 않는 UUID면 빈 Optional 반환")
        void findByUuidAndIsDeletedFalse_ReturnsEmpty_WhenNotExists() {
            // given
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByUuidAndIsDeletedFalse("nonexistent-uuid");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("loginId 존재 여부 확인 테스트")
    class ExistsByLoginIdTests {

        @Test
        @DisplayName("loginId 존재 여부 - 활성 회원이 존재하면 true 반환")
        void existsByLoginIdAndIsDeletedFalse_ReturnsTrue_WhenExists() {
            // given
            memberRepository.save(testMember);

            // when
            boolean result = memberRepository.existsByLoginIdAndIsDeletedFalse("testuser123");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("loginId 존재 여부 - 삭제된 회원은 존재하지 않음으로 처리")
        void existsByLoginIdAndIsDeletedFalse_ReturnsFalse_WhenDeleted() {
            // given
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            boolean result = memberRepository.existsByLoginIdAndIsDeletedFalse("testuser123");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("loginId 존재 여부 - 존재하지 않으면 false 반환")
        void existsByLoginIdAndIsDeletedFalse_ReturnsFalse_WhenNotExists() {
            // when
            boolean result = memberRepository.existsByLoginIdAndIsDeletedFalse("testuser123");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("휴대폰 번호 관련 테스트")
    class PhoneNumberTests {

        @Test
        @DisplayName("휴대폰 번호 존재 여부 - 활성 회원이 존재하면 true 반환")
        void existsByPhoneNumberAndIsDeletedFalse_ReturnsTrue_WhenExists() {
            // given
            memberRepository.save(testMember);

            // when
            boolean result = memberRepository.existsByPhoneNumberAndIsDeletedFalse("01012345678");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("휴대폰 번호 존재 여부 - 삭제된 회원은 존재하지 않음으로 처리")
        void existsByPhoneNumberAndIsDeletedFalse_ReturnsFalse_WhenDeleted() {
            // given
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            boolean result = memberRepository.existsByPhoneNumberAndIsDeletedFalse("01012345678");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("휴대폰 번호로 활성 회원 조회 - 성공")
        void findByPhoneNumberAndIsDeletedFalse_Success() {
            // given
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByPhoneNumberAndIsDeletedFalse("01012345678");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getPhoneNumber()).isEqualTo("01012345678");
        }

        @Test
        @DisplayName("휴대폰 번호로 조회 - 삭제된 회원은 조회되지 않음")
        void findByPhoneNumberAndIsDeletedFalse_NotFound_WhenDeleted() {
            // given
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByPhoneNumberAndIsDeletedFalse("01012345678");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("소셜 ID 조회 테스트")
    class SocialIdTests {

        @Test
        @DisplayName("카카오 소셜 ID로 활성 회원 조회 - 성공")
        void findBySocialIdKakaoAndIsDeletedFalse_Success() {
            // given
            ReflectionTestUtils.setField(testMember, "socialIdKakao", "kakao123456");
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findBySocialIdKakaoAndIsDeletedFalse("kakao123456");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSocialIdKakao()).isEqualTo("kakao123456");
        }

        @Test
        @DisplayName("카카오 소셜 ID로 조회 - 삭제된 회원은 조회되지 않음")
        void findBySocialIdKakaoAndIsDeletedFalse_NotFound_WhenDeleted() {
            // given
            ReflectionTestUtils.setField(testMember, "socialIdKakao", "kakao123456");
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findBySocialIdKakaoAndIsDeletedFalse("kakao123456");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("네이버 소셜 ID로 활성 회원 조회 - 성공")
        void findBySocialIdNaverAndIsDeletedFalse_Success() {
            // given
            ReflectionTestUtils.setField(testMember, "socialIdNaver", "naver123456");
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findBySocialIdNaverAndIsDeletedFalse("naver123456");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSocialIdNaver()).isEqualTo("naver123456");
        }

        @Test
        @DisplayName("네이버 소셜 ID로 조회 - 삭제된 회원은 조회되지 않음")
        void findBySocialIdNaverAndIsDeletedFalse_NotFound_WhenDeleted() {
            // given
            ReflectionTestUtils.setField(testMember, "socialIdNaver", "naver123456");
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findBySocialIdNaverAndIsDeletedFalse("naver123456");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("구글 소셜 ID로 활성 회원 조회 - 성공")
        void findBySocialIdGoogleAndIsDeletedFalse_Success() {
            // given
            ReflectionTestUtils.setField(testMember, "socialIdGoogle", "google123456");
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findBySocialIdGoogleAndIsDeletedFalse("google123456");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSocialIdGoogle()).isEqualTo("google123456");
        }

        @Test
        @DisplayName("구글 소셜 ID로 조회 - 삭제된 회원은 조회되지 않음")
        void findBySocialIdGoogleAndIsDeletedFalse_NotFound_WhenDeleted() {
            // given
            ReflectionTestUtils.setField(testMember, "socialIdGoogle", "google123456");
            testMember.updateDeleteStatus();
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findBySocialIdGoogleAndIsDeletedFalse("google123456");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("소셜 ID 조회 - 존재하지 않으면 빈 Optional 반환")
        void findBySocialId_ReturnsEmpty_WhenNotExists() {
            // given
            memberRepository.save(testMember);

            // when
            Optional<Member> kakaoResult = memberRepository.findBySocialIdKakaoAndIsDeletedFalse("nonexistent");
            Optional<Member> naverResult = memberRepository.findBySocialIdNaverAndIsDeletedFalse("nonexistent");
            Optional<Member> googleResult = memberRepository.findBySocialIdGoogleAndIsDeletedFalse("nonexistent");

            // then
            assertThat(kakaoResult).isEmpty();
            assertThat(naverResult).isEmpty();
            assertThat(googleResult).isEmpty();
        }
    }

    @Nested
    @DisplayName("복합 조건 테스트")
    class ComplexQueryTests {

        @Test
        @DisplayName("여러 소셜 계정이 연동된 회원 저장 및 조회")
        void saveAndFind_MemberWithMultipleSocialAccounts() {
            // given
            ReflectionTestUtils.setField(testMember, "socialIdKakao", "kakao123");
            ReflectionTestUtils.setField(testMember, "socialIdNaver", "naver123");
            ReflectionTestUtils.setField(testMember, "socialIdGoogle", "google123");
            memberRepository.save(testMember);

            // when
            Optional<Member> byKakao = memberRepository.findBySocialIdKakaoAndIsDeletedFalse("kakao123");
            Optional<Member> byNaver = memberRepository.findBySocialIdNaverAndIsDeletedFalse("naver123");
            Optional<Member> byGoogle = memberRepository.findBySocialIdGoogleAndIsDeletedFalse("google123");

            // then
            assertThat(byKakao).isPresent();
            assertThat(byNaver).isPresent();
            assertThat(byGoogle).isPresent();

            assertThat(byKakao.get().getId()).isEqualTo(byNaver.get().getId());
            assertThat(byNaver.get().getId()).isEqualTo(byGoogle.get().getId());
        }
    }
}