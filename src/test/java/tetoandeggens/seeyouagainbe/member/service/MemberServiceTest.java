package tetoandeggens.seeyouagainbe.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.member.dto.request.UpdatePushEnabledRequest;
import tetoandeggens.seeyouagainbe.member.dto.response.MyInfoResponse;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("MemberService 테스트")
class MemberServiceTest extends ServiceTest {

    @Autowired
    private MemberService memberService;

    @MockitoBean
    private MemberRepository memberRepository;

    private Member testMember;
    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_NICKNAME = "테스트유저";
    private static final String TEST_PROFILE = "https://example.com/profile.jpg";

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .loginId("testuser")
                .password("password123!")
                .nickName(TEST_NICKNAME)
                .phoneNumber("01012345678")
                .profile(TEST_PROFILE)
                .build();

        ReflectionTestUtils.setField(testMember, "id", TEST_MEMBER_ID);
        ReflectionTestUtils.setField(testMember, "isPushEnabled", false);
    }

    @Nested
    @DisplayName("마이페이지 정보 조회")
    class GetMyInfo {

        @Test
        @DisplayName("마이페이지 정보 조회 - 성공")
        void getMyInfo_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.of(testMember));

            // when
            MyInfoResponse response = memberService.getMyInfo(TEST_MEMBER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.nickName()).isEqualTo(TEST_NICKNAME);
            assertThat(response.profile()).isEqualTo(TEST_PROFILE);
        }

        @Test
        @DisplayName("마이페이지 정보 조회 - 프로필 이미지가 없는 경우")
        void getMyInfo_Success_WithNullProfile() {
            // given
            Member memberWithoutProfile = Member.builder()
                    .loginId("testuser")
                    .password("password123!")
                    .nickName(TEST_NICKNAME)
                    .phoneNumber("01012345678")
                    .profile(null)
                    .build();

            ReflectionTestUtils.setField(memberWithoutProfile, "id", TEST_MEMBER_ID);

            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.of(memberWithoutProfile));

            // when
            MyInfoResponse response = memberService.getMyInfo(TEST_MEMBER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.nickName()).isEqualTo(TEST_NICKNAME);
            assertThat(response.profile()).isNull();
        }

        @Test
        @DisplayName("마이페이지 정보 조회 - 존재하지 않는 회원이면 예외 발생")
        void getMyInfo_MemberNotFound_ThrowsException() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    memberService.getMyInfo(TEST_MEMBER_ID)
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("마이페이지 정보 조회 - 삭제된 회원이면 예외 발생")
        void getMyInfo_DeletedMember_ThrowsException() {
            // given
            testMember.updateDeleteStatus();

            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    memberService.getMyInfo(TEST_MEMBER_ID)
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("푸시 알림 토글 업데이트")
    class UpdatePushEnabled {

        @Test
        @DisplayName("푸시 알림 활성화 - 성공")
        void updatePushEnabled_EnablePush_Success() {
            // given
            UpdatePushEnabledRequest request = new UpdatePushEnabledRequest(true);
            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.of(testMember));

            // when
            memberService.updatePushEnabled(TEST_MEMBER_ID, request);

            // then
            assertThat(testMember.getIsPushEnabled()).isTrue();
        }

        @Test
        @DisplayName("푸시 알림 비활성화 - 성공")
        void updatePushEnabled_DisablePush_Success() {
            // given
            ReflectionTestUtils.setField(testMember, "isPushEnabled", true);
            UpdatePushEnabledRequest request = new UpdatePushEnabledRequest(false);

            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.of(testMember));

            // when
            memberService.updatePushEnabled(TEST_MEMBER_ID, request);

            // then
            assertThat(testMember.getIsPushEnabled()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 회원 - 예외 발생")
        void updatePushEnabled_MemberNotFound_ThrowsException() {
            // given
            UpdatePushEnabledRequest request = new UpdatePushEnabledRequest(true);
            given(memberRepository.findByIdAndIsDeletedFalse(TEST_MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    memberService.updatePushEnabled(TEST_MEMBER_ID, request)
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
        }
    }
}