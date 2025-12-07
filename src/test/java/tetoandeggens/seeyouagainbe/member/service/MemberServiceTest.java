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
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

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

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .loginId("testuser")
                .password("password123!")
                .nickName("테스트유저")
                .phoneNumber("01012345678")
                .build();

        ReflectionTestUtils.setField(testMember, "id", TEST_MEMBER_ID);
        ReflectionTestUtils.setField(testMember, "isPushEnabled", false);
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
                    .willReturn(java.util.Optional.of(testMember));

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
                    .willReturn(java.util.Optional.of(testMember));

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
                    .willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    memberService.updatePushEnabled(TEST_MEMBER_ID, request)
            )
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
        }
    }
}