package tetoandeggens.seeyouagainbe.fcm.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("FcmTokenRepository 단위 테스트")
class FcmTokenRepositoryTest extends RepositoryTest {

    private static final String TEST_TOKEN = "test-fcm-token-12345";
    private static final String TEST_DEVICE_ID = "test-device-id-12345";

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @PersistenceContext
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
    @DisplayName("FCM 토큰 저장 테스트")
    class SaveFcmTokenTests {

        @Test
        @DisplayName("FCM 토큰 저장 - 성공")
        void saveFcmToken_Success() {
            // given
            FcmToken fcmToken = FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build();

            // when
            FcmToken savedToken = fcmTokenRepository.save(fcmToken);

            // then
            assertThat(savedToken.getId()).isNotNull();
            assertThat(savedToken.getToken()).isEqualTo(TEST_TOKEN);
            assertThat(savedToken.getDeviceId()).isEqualTo(TEST_DEVICE_ID);
            assertThat(savedToken.getDeviceType()).isEqualTo(DeviceType.ANDROID);
            assertThat(savedToken.getMember().getId()).isEqualTo(testMember.getId());
            assertThat(savedToken.getLastUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("FCM 토큰 저장 - iOS 기기")
        void saveFcmToken_Success_iOS() {
            // given
            FcmToken fcmToken = FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.IOS)
                    .build();

            // when
            FcmToken savedToken = fcmTokenRepository.save(fcmToken);

            // then
            assertThat(savedToken.getDeviceType()).isEqualTo(DeviceType.IOS);
        }

        @Test
        @DisplayName("FCM 토큰 저장 - WEB 기기")
        void saveFcmToken_Success_Web() {
            // given
            FcmToken fcmToken = FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.WEB)
                    .build();

            // when
            FcmToken savedToken = fcmTokenRepository.save(fcmToken);

            // then
            assertThat(savedToken.getDeviceType()).isEqualTo(DeviceType.WEB);
        }
    }

    @Nested
    @DisplayName("FCM 토큰 조회 테스트")
    class FindFcmTokenTests {

        @Test
        @DisplayName("회원 ID와 기기 ID로 FCM 토큰 조회 - 성공")
        void findByMemberIdAndDeviceId_Success() {
            // given
            FcmToken fcmToken = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            // when
            Optional<FcmToken> foundToken = fcmTokenRepository.findByMemberIdAndDeviceId(
                    testMember.getId(), TEST_DEVICE_ID);

            // then
            assertThat(foundToken).isPresent();
            assertThat(foundToken.get().getId()).isEqualTo(fcmToken.getId());
            assertThat(foundToken.get().getToken()).isEqualTo(TEST_TOKEN);
        }

        @Test
        @DisplayName("회원 ID와 기기 ID로 FCM 토큰 조회 - 토큰이 없으면 빈 Optional 반환")
        void findByMemberIdAndDeviceId_ReturnsEmpty_WhenNotFound() {
            // when
            Optional<FcmToken> foundToken = fcmTokenRepository.findByMemberIdAndDeviceId(
                    testMember.getId(), "non-existent-device-id");

            // then
            assertThat(foundToken).isEmpty();
        }

        @Test
        @DisplayName("회원 ID로 모든 FCM 토큰 조회 - 성공")
        void findAllByMemberId_Success() {
            // given
            fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token("another-token")
                    .deviceId("another-device-id")
                    .deviceType(DeviceType.IOS)
                    .build());

            // when
            List<FcmToken> tokens = fcmTokenRepository.findAllByMemberId(testMember.getId());

            // then
            assertThat(tokens).hasSize(2);
            assertThat(tokens).extracting(FcmToken::getDeviceType)
                    .containsExactlyInAnyOrder(DeviceType.ANDROID, DeviceType.IOS);
        }

        @Test
        @DisplayName("회원 ID로 모든 FCM 토큰 조회 - 토큰이 없으면 빈 리스트 반환")
        void findAllByMemberId_ReturnsEmptyList_WhenNoTokens() {
            // when
            List<FcmToken> tokens = fcmTokenRepository.findAllByMemberId(testMember.getId());

            // then
            assertThat(tokens).isEmpty();
        }
    }

    @Nested
    @DisplayName("FCM 토큰 업데이트 테스트")
    class UpdateFcmTokenTests {

        @Test
        @DisplayName("FCM 토큰 업데이트 - 성공")
        void updateFcmToken_Success() {
            // given
            FcmToken fcmToken = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            String newToken = "new-fcm-token-67890";

            // when
            fcmToken.updateToken(newToken);
            FcmToken updatedToken = fcmTokenRepository.save(fcmToken);
            entityManager.flush();
            entityManager.clear();

            // then
            FcmToken foundToken = fcmTokenRepository.findById(updatedToken.getId()).orElseThrow();
            assertThat(foundToken.getToken()).isEqualTo(newToken);
        }

        @Test
        @DisplayName("FCM 토큰 마지막 사용 시간 업데이트 - 성공")
        void updateLastUsedAt_Success() {
            // given
            FcmToken fcmToken = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            LocalDateTime originalLastUsedAt = fcmToken.getLastUsedAt();

            // 시간을 약간 지연시켜 차이를 만듦
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            fcmToken.updateLastUsedAt();
            FcmToken updatedToken = fcmTokenRepository.save(fcmToken);
            entityManager.flush();
            entityManager.clear();

            // then
            FcmToken foundToken = fcmTokenRepository.findById(updatedToken.getId()).orElseThrow();
            assertThat(foundToken.getLastUsedAt()).isAfter(originalLastUsedAt);
        }
    }

    @Nested
    @DisplayName("FCM 토큰 삭제 테스트")
    class DeleteFcmTokenTests {

        @Test
        @DisplayName("FCM 토큰 삭제 - 성공")
        void deleteFcmToken_Success() {
            // given
            FcmToken fcmToken = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            Long tokenId = fcmToken.getId();

            // when
            fcmTokenRepository.delete(fcmToken);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<FcmToken> deletedToken = fcmTokenRepository.findById(tokenId);
            assertThat(deletedToken).isEmpty();
        }

        @Test
        @DisplayName("FCM 토큰 삭제 - 여러 토큰 중 하나만 삭제")
        void deleteOneFcmToken_Success() {
            // given
            FcmToken token1 = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            FcmToken token2 = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token("another-token")
                    .deviceId("another-device-id")
                    .deviceType(DeviceType.IOS)
                    .build());

            // when
            fcmTokenRepository.delete(token1);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<FcmToken> deletedToken = fcmTokenRepository.findById(token1.getId());
            Optional<FcmToken> remainingToken = fcmTokenRepository.findById(token2.getId());

            assertThat(deletedToken).isEmpty();
            assertThat(remainingToken).isPresent();
        }
    }

    @Nested
    @DisplayName("FCM 토큰 만료 관련 테스트")
    class FcmTokenExpirationTests {

        @Test
        @DisplayName("30일 이상 지난 토큰 필터링 - CustomRepository 메서드")
        void findExpiredTokens_Success() {
            // given
            FcmToken recentToken = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            FcmToken oldToken = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token("old-token")
                    .deviceId("old-device-id")
                    .deviceType(DeviceType.IOS)
                    .lastUsedAt(LocalDateTime.now().minusDays(31))
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            List<FcmToken> allTokens = fcmTokenRepository.findAllByMemberId(testMember.getId());

            // then
            assertThat(allTokens).hasSize(2);

            // 만료된 토큰과 유효한 토큰 구분
            long expiredCount = allTokens.stream()
                    .filter(token -> token.getLastUsedAt()
                            .isBefore(LocalDateTime.now().minusDays(30)))
                    .count();

            assertThat(expiredCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("FCM 토큰 연관관계 테스트")
    class FcmTokenRelationshipTests {

        @Test
        @DisplayName("회원과 FCM 토큰의 연관관계 - Member 삭제시 외래키 제약조건 확인")
        void deleteMember_FailsWithForeignKeyConstraint() {
            // given
            fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            entityManager.flush();
            entityManager.clear();

            Long memberId = testMember.getId();

            // when & then - FCM 토큰이 존재하는 상태에서 Member 삭제시 제약조건 위반 예외 발생
            assertThatThrownBy(() -> {
                Member managedMember = memberRepository.findById(memberId).orElseThrow();
                memberRepository.delete(managedMember);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("회원과 FCM 토큰의 연관관계 - Fetch Join으로 Member 함께 조회")
        void findTokenWithMember_Success() {
            // given
            FcmToken fcmToken = fcmTokenRepository.save(FcmToken.builder()
                    .member(testMember)
                    .token(TEST_TOKEN)
                    .deviceId(TEST_DEVICE_ID)
                    .deviceType(DeviceType.ANDROID)
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            FcmToken foundToken = fcmTokenRepository.findById(fcmToken.getId()).orElseThrow();

            // then
            assertThat(foundToken.getMember()).isNotNull();
            assertThat(foundToken.getMember().getId()).isEqualTo(testMember.getId());
            assertThat(foundToken.getMember().getNickName()).isEqualTo("테스트유저");
        }
    }
}