package tetoandeggens.seeyouagainbe.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static tetoandeggens.seeyouagainbe.global.constants.AuthVerificationConstants.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisAuthService 단위 테스트")
class RedisAuthServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisAuthService redisAuthService;

    private static final String TEST_PHONE = "01012345678";
    private static final String TEST_CODE = "123456";
    private static final String TEST_TIME = "2025-11-10T10:00:00";
    private static final String TEST_UUID = "test-uuid-123";
    private static final String TEST_PROVIDER = "kakao";
    private static final String TEST_SOCIAL_ID = "kakao123456";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("일반 휴대폰 인증 테스트")
    class PhoneVerificationTests {

        @Test
        @DisplayName("인증 코드 저장 - 성공")
        void saveVerificationCode_Success() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveVerificationCode(TEST_PHONE, TEST_CODE);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_VERIFICATION_CODE + TEST_PHONE),
                    eq(TEST_CODE),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("인증 시간 저장 - 성공")
        void saveVerificationTime_Success() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveVerificationTime(TEST_PHONE, TEST_TIME);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_VERIFICATION_TIME + TEST_PHONE),
                    eq(TEST_TIME),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("인증 코드 조회 - 성공")
        void getVerificationCode_Success() {
            // given
            when(valueOperations.get(PREFIX_VERIFICATION_CODE + TEST_PHONE)).thenReturn(TEST_CODE);

            // when
            Optional<String> result = redisAuthService.getVerificationCode(TEST_PHONE);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_CODE);
            verify(valueOperations).get(PREFIX_VERIFICATION_CODE + TEST_PHONE);
        }

        @Test
        @DisplayName("인증 코드 조회 - 존재하지 않으면 빈 Optional 반환")
        void getVerificationCode_ReturnsEmpty_WhenNotExists() {
            // given
            when(valueOperations.get(PREFIX_VERIFICATION_CODE + TEST_PHONE)).thenReturn(null);

            // when
            Optional<String> result = redisAuthService.getVerificationCode(TEST_PHONE);

            // then
            assertThat(result).isEmpty();
            verify(valueOperations).get(PREFIX_VERIFICATION_CODE + TEST_PHONE);
        }

        @Test
        @DisplayName("인증 시간 조회 - 성공")
        void getVerificationTime_Success() {
            // given
            when(valueOperations.get(PREFIX_VERIFICATION_TIME + TEST_PHONE)).thenReturn(TEST_TIME);

            // when
            Optional<String> result = redisAuthService.getVerificationTime(TEST_PHONE);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_TIME);
            verify(valueOperations).get(PREFIX_VERIFICATION_TIME + TEST_PHONE);
        }

        @Test
        @DisplayName("휴대폰 인증 완료 표시 - 성공")
        void markPhoneAsVerified_Success() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.markPhoneAsVerified(TEST_PHONE);

            // then
            verify(valueOperations).set(
                    eq(TEST_PHONE),
                    eq(VERIFIED),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("휴대폰 인증 여부 확인 - 인증됨")
        void isPhoneVerified_ReturnsTrue_WhenVerified() {
            // given
            when(valueOperations.get(TEST_PHONE)).thenReturn(VERIFIED);

            // when
            boolean result = redisAuthService.isPhoneVerified(TEST_PHONE);

            // then
            assertThat(result).isTrue();
            verify(valueOperations).get(TEST_PHONE);
        }

        @Test
        @DisplayName("휴대폰 인증 여부 확인 - 인증 안됨")
        void isPhoneVerified_ReturnsFalse_WhenNotVerified() {
            // given
            when(valueOperations.get(TEST_PHONE)).thenReturn(null);

            // when
            boolean result = redisAuthService.isPhoneVerified(TEST_PHONE);

            // then
            assertThat(result).isFalse();
            verify(valueOperations).get(TEST_PHONE);
        }

        @Test
        @DisplayName("인증 데이터 삭제 - 성공")
        void deleteVerificationData_Success() {
            // given
            when(redisTemplate.delete(PREFIX_VERIFICATION_CODE + TEST_PHONE)).thenReturn(true);
            when(redisTemplate.delete(PREFIX_VERIFICATION_TIME + TEST_PHONE)).thenReturn(true);

            // when
            redisAuthService.deleteVerificationData(TEST_PHONE);

            // then
            verify(redisTemplate).delete(PREFIX_VERIFICATION_CODE + TEST_PHONE);
            verify(redisTemplate).delete(PREFIX_VERIFICATION_TIME + TEST_PHONE);
        }

        @Test
        @DisplayName("휴대폰 인증 정보 삭제 - 성공")
        void deletePhoneVerification_Success() {
            // given
            when(redisTemplate.delete(TEST_PHONE)).thenReturn(true);

            // when
            redisAuthService.deletePhoneVerification(TEST_PHONE);

            // then
            verify(redisTemplate).delete(TEST_PHONE);
        }
    }

    @Nested
    @DisplayName("소셜 로그인 휴대폰 인증 테스트")
    class SocialPhoneVerificationTests {

        @Test
        @DisplayName("소셜 인증 코드 저장 - 성공")
        void saveSocialVerificationCode_Success() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveSocialVerificationCode(TEST_PHONE, TEST_CODE);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE),
                    eq(TEST_CODE),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("소셜 provider 저장 - 성공")
        void saveSocialProvider_Success() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveSocialProvider(TEST_PHONE, TEST_PROVIDER);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_SOCIAL_PROVIDER + TEST_PHONE),
                    eq(TEST_PROVIDER),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("소셜 ID 저장 - 성공")
        void saveSocialId_Success() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveSocialId(TEST_PHONE, TEST_SOCIAL_ID);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_SOCIAL_ID + TEST_PHONE),
                    eq(TEST_SOCIAL_ID),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("소셜 provider 조회 - 성공")
        void getSocialProvider_Success() {
            // given
            when(valueOperations.get(PREFIX_SOCIAL_PROVIDER + TEST_PHONE)).thenReturn(TEST_PROVIDER);

            // when
            Optional<String> result = redisAuthService.getSocialProvider(TEST_PHONE);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_PROVIDER);
        }

        @Test
        @DisplayName("소셜 ID 조회 - 성공")
        void getSocialId_Success() {
            // given
            when(valueOperations.get(PREFIX_SOCIAL_ID + TEST_PHONE)).thenReturn(TEST_SOCIAL_ID);

            // when
            Optional<String> result = redisAuthService.getSocialId(TEST_PHONE);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_SOCIAL_ID);
        }

        @Test
        @DisplayName("소셜 휴대폰 인증 완료 표시 - 성공")
        void markSocialPhoneAsVerified_Success() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.markSocialPhoneAsVerified(TEST_PHONE);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_SOCIAL_VERIFIED + TEST_PHONE),
                    eq(VERIFIED),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("소셜 휴대폰 인증 여부 확인 - 인증됨")
        void isSocialPhoneVerified_ReturnsTrue_WhenVerified() {
            // given
            when(valueOperations.get(PREFIX_SOCIAL_VERIFIED + TEST_PHONE)).thenReturn(VERIFIED);

            // when
            boolean result = redisAuthService.isSocialPhoneVerified(TEST_PHONE);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("소셜 인증 데이터 삭제 - 성공")
        void deleteSocialVerificationData_Success() {
            // given
            when(redisTemplate.delete(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE)).thenReturn(true);
            when(redisTemplate.delete(PREFIX_SOCIAL_VERIFICATION_TIME + TEST_PHONE)).thenReturn(true);

            // when
            redisAuthService.deleteSocialVerificationData(TEST_PHONE);

            // then
            verify(redisTemplate).delete(PREFIX_SOCIAL_VERIFICATION_CODE + TEST_PHONE);
            verify(redisTemplate).delete(PREFIX_SOCIAL_VERIFICATION_TIME + TEST_PHONE);
        }

        @Test
        @DisplayName("소셜 휴대폰 데이터 전체 삭제 - 성공")
        void clearSocialPhoneData_Success() {
            // given
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // when
            redisAuthService.clearSocialPhoneData(TEST_PHONE);

            // then
            verify(redisTemplate).delete(PREFIX_SOCIAL_VERIFIED + TEST_PHONE);
            verify(redisTemplate).delete(PREFIX_SOCIAL_PROVIDER + TEST_PHONE);
            verify(redisTemplate).delete(PREFIX_SOCIAL_ID + TEST_PHONE);
            verify(redisTemplate).delete(PREFIX_SOCIAL_TEMP_UUID + TEST_PHONE);
        }
    }

    @Nested
    @DisplayName("소셜 임시 정보 관리 테스트")
    class TempSocialInfoTests {

        @Test
        @DisplayName("임시 소셜 정보 저장 - RefreshToken 포함 성공")
        void saveTempSocialInfo_Success_WithRefreshToken() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveTempSocialInfo(TEST_UUID, TEST_PROVIDER, TEST_SOCIAL_ID, TEST_REFRESH_TOKEN);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_TEMP_SOCIAL_PROVIDER + TEST_UUID),
                    eq(TEST_PROVIDER),
                    any(Duration.class)
            );
            verify(valueOperations).set(
                    eq(PREFIX_TEMP_SOCIAL_ID + TEST_UUID),
                    eq(TEST_SOCIAL_ID),
                    any(Duration.class)
            );
            verify(valueOperations).set(
                    eq(PREFIX_TEMP_SOCIAL_REFRESH + TEST_UUID),
                    eq(TEST_REFRESH_TOKEN),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("임시 소셜 정보 저장 - RefreshToken 없이 성공")
        void saveTempSocialInfo_Success_WithoutRefreshToken() {
            // given
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveTempSocialInfo(TEST_UUID, TEST_PROVIDER, TEST_SOCIAL_ID, null);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_TEMP_SOCIAL_PROVIDER + TEST_UUID),
                    eq(TEST_PROVIDER),
                    any(Duration.class)
            );
            verify(valueOperations).set(
                    eq(PREFIX_TEMP_SOCIAL_ID + TEST_UUID),
                    eq(TEST_SOCIAL_ID),
                    any(Duration.class)
            );
            verify(valueOperations, never()).set(
                    contains(PREFIX_TEMP_SOCIAL_REFRESH),
                    anyString(),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("임시 provider 조회 - 성공")
        void getTempSocialProvider_Success() {
            // given
            when(valueOperations.get(PREFIX_TEMP_SOCIAL_PROVIDER + TEST_UUID)).thenReturn(TEST_PROVIDER);

            // when
            Optional<String> result = redisAuthService.getTempSocialProvider(TEST_UUID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_PROVIDER);
        }

        @Test
        @DisplayName("임시 소셜 ID 조회 - 성공")
        void getTempSocialId_Success() {
            // given
            when(valueOperations.get(PREFIX_TEMP_SOCIAL_ID + TEST_UUID)).thenReturn(TEST_SOCIAL_ID);

            // when
            Optional<String> result = redisAuthService.getTempSocialId(TEST_UUID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_SOCIAL_ID);
        }

        @Test
        @DisplayName("임시 RefreshToken 조회 - 성공")
        void getTempSocialRefreshToken_Success() {
            // given
            when(valueOperations.get(PREFIX_TEMP_SOCIAL_REFRESH + TEST_UUID)).thenReturn(TEST_REFRESH_TOKEN);

            // when
            Optional<String> result = redisAuthService.getTempSocialRefreshToken(TEST_UUID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("임시 소셜 정보 삭제 - 성공")
        void deleteTempSocialInfo_Success() {
            // given
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // when
            redisAuthService.deleteTempSocialInfo(TEST_UUID);

            // then
            verify(redisTemplate).delete(PREFIX_TEMP_SOCIAL_PROVIDER + TEST_UUID);
            verify(redisTemplate).delete(PREFIX_TEMP_SOCIAL_ID + TEST_UUID);
            verify(redisTemplate).delete(PREFIX_TEMP_SOCIAL_REFRESH + TEST_UUID);
        }

        @Test
        @DisplayName("임시 소셜 정보 TTL 연장 - 성공")
        void extendTempSocialInfoTTL_Success() {
            // given
            when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

            // when
            redisAuthService.extendTempSocialInfoTTL(TEST_UUID);

            // then
            verify(redisTemplate).expire(eq(PREFIX_TEMP_SOCIAL_PROVIDER + TEST_UUID), any(Duration.class));
            verify(redisTemplate).expire(eq(PREFIX_TEMP_SOCIAL_ID + TEST_UUID), any(Duration.class));
            verify(redisTemplate).expire(eq(PREFIX_TEMP_SOCIAL_REFRESH + TEST_UUID), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("RefreshToken 및 MemberId 관리 테스트")
    class RefreshTokenAndMemberIdTests {

        private static final String PREFIX_REFRESH_TOKEN = "refresh:";
        private static final String PREFIX_MEMBER_ID = "member:";
        private static final Long TEST_MEMBER_ID = 1L;

        @Test
        @DisplayName("RefreshToken 저장 - 성공")
        void saveRefreshToken_Success() {
            // given
            long TEST_EXPIRATION_MS = 86400000L; // 24시간
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveRefreshToken(TEST_UUID, TEST_REFRESH_TOKEN, TEST_EXPIRATION_MS);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_REFRESH_TOKEN + TEST_UUID),
                    eq(TEST_REFRESH_TOKEN),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("RefreshToken 조회 - 성공")
        void getRefreshToken_Success() {
            // given
            when(valueOperations.get(PREFIX_REFRESH_TOKEN + TEST_UUID)).thenReturn(TEST_REFRESH_TOKEN);

            // when
            Optional<String> result = redisAuthService.getRefreshToken(TEST_UUID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_REFRESH_TOKEN);
            verify(valueOperations).get(PREFIX_REFRESH_TOKEN + TEST_UUID);
        }

        @Test
        @DisplayName("RefreshToken 조회 - 존재하지 않으면 빈 Optional 반환")
        void getRefreshToken_ReturnsEmpty_WhenNotExists() {
            // given
            when(valueOperations.get(PREFIX_REFRESH_TOKEN + TEST_UUID)).thenReturn(null);

            // when
            Optional<String> result = redisAuthService.getRefreshToken(TEST_UUID);

            // then
            assertThat(result).isEmpty();
            verify(valueOperations).get(PREFIX_REFRESH_TOKEN + TEST_UUID);
        }

        @Test
        @DisplayName("RefreshToken 삭제 - 성공")
        void deleteRefreshToken_Success() {
            // given
            when(redisTemplate.delete(PREFIX_REFRESH_TOKEN + TEST_UUID)).thenReturn(true);

            // when
            redisAuthService.deleteRefreshToken(TEST_UUID);

            // then
            verify(redisTemplate).delete(PREFIX_REFRESH_TOKEN + TEST_UUID);
        }

        @Test
        @DisplayName("MemberId 저장 - 성공")
        void saveMemberId_Success() {
            // given
            long TEST_EXPIRATION_MS = 86400000L; // 24시간
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // when
            redisAuthService.saveMemberId(TEST_UUID, TEST_MEMBER_ID, TEST_EXPIRATION_MS);

            // then
            verify(valueOperations).set(
                    eq(PREFIX_MEMBER_ID + TEST_UUID),
                    eq(String.valueOf(TEST_MEMBER_ID)),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("MemberId 조회 - 성공")
        void getMemberId_Success() {
            // given
            when(valueOperations.get(PREFIX_MEMBER_ID + TEST_UUID)).thenReturn(String.valueOf(TEST_MEMBER_ID));

            // when
            Optional<Long> result = redisAuthService.getMemberId(TEST_UUID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(TEST_MEMBER_ID);
            verify(valueOperations).get(PREFIX_MEMBER_ID + TEST_UUID);
        }

        @Test
        @DisplayName("MemberId 조회 - 존재하지 않으면 빈 Optional 반환")
        void getMemberId_ReturnsEmpty_WhenNotExists() {
            // given
            when(valueOperations.get(PREFIX_MEMBER_ID + TEST_UUID)).thenReturn(null);

            // when
            Optional<Long> result = redisAuthService.getMemberId(TEST_UUID);

            // then
            assertThat(result).isEmpty();
            verify(valueOperations).get(PREFIX_MEMBER_ID + TEST_UUID);
        }

        @Test
        @DisplayName("MemberId 삭제 - 성공")
        void deleteMemberId_Success() {
            // given
            when(redisTemplate.delete(PREFIX_MEMBER_ID + TEST_UUID)).thenReturn(true);

            // when
            redisAuthService.deleteMemberId(TEST_UUID);

            // then
            verify(redisTemplate).delete(PREFIX_MEMBER_ID + TEST_UUID);
        }

        @Test
        @DisplayName("로그아웃 시 RefreshToken과 MemberId 모두 삭제")
        void deleteRefreshTokenAndMemberId_Success() {
            // given
            when(redisTemplate.delete(PREFIX_REFRESH_TOKEN + TEST_UUID)).thenReturn(true);
            when(redisTemplate.delete(PREFIX_MEMBER_ID + TEST_UUID)).thenReturn(true);

            // when
            redisAuthService.deleteRefreshToken(TEST_UUID);
            redisAuthService.deleteMemberId(TEST_UUID);

            // then
            verify(redisTemplate).delete(PREFIX_REFRESH_TOKEN + TEST_UUID);
            verify(redisTemplate).delete(PREFIX_MEMBER_ID + TEST_UUID);
        }
    }
}