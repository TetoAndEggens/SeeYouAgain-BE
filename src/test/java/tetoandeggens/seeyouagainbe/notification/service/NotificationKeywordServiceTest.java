package tetoandeggens.seeyouagainbe.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.FcmErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.notification.dto.request.BulkUpdateKeywordsRequest;
import tetoandeggens.seeyouagainbe.notification.dto.request.NotificationKeywordRequest;
import tetoandeggens.seeyouagainbe.notification.dto.response.BulkUpdateKeywordsResponse;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;
import tetoandeggens.seeyouagainbe.notification.repository.NotificationKeywordRepository;

@DisplayName("NotificationKeywordService 단위 테스트")
class NotificationKeywordServiceTest extends ServiceTest {

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_KEYWORD = "골든리트리버";
    private static final Long TEST_KEYWORD_ID = 1L;

    @Autowired
    private NotificationKeywordService notificationKeywordService;

    @MockitoBean
    private NotificationKeywordRepository notificationKeywordRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("키워드 구독 테스트")
    class SubscribeKeywordTests {

        @Test
        @DisplayName("키워드 구독 - 성공 (품종 타입)")
        void subscribeKeyword_Success_Breed() {
            // given
            Member member = createTestMember();
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                    TEST_MEMBER_ID, TEST_KEYWORD, KeywordType.ABANDONED, KeywordCategoryType.BREED))
                    .willReturn(false);
            given(notificationKeywordRepository.save(any(NotificationKeyword.class)))
                    .willAnswer(invocation -> {
                        NotificationKeyword keyword = invocation.getArgument(0);
                        ReflectionTestUtils.setField(keyword, "id", TEST_KEYWORD_ID);
                        return keyword;
                    });

            // when
            NotificationKeywordResponse response = notificationKeywordService.subscribe(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.keyword()).isEqualTo(TEST_KEYWORD);
            assertThat(response.keywordType()).isEqualTo(KeywordType.ABANDONED);
            assertThat(response.keywordCategoryType()).isEqualTo(KeywordCategoryType.BREED);

            verify(notificationKeywordRepository).save(any(NotificationKeyword.class));
        }

        @Test
        @DisplayName("키워드 구독 - 성공 (지역 타입)")
        void subscribeKeyword_Success_Region() {
            // given
            Member member = createTestMember();
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    "서울",
                    KeywordType.WITNESS,
                    KeywordCategoryType.LOCATION
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                    TEST_MEMBER_ID, "서울", KeywordType.WITNESS, KeywordCategoryType.LOCATION))
                    .willReturn(false);
            given(notificationKeywordRepository.save(any(NotificationKeyword.class)))
                    .willAnswer(invocation -> {
                        NotificationKeyword keyword = invocation.getArgument(0);
                        ReflectionTestUtils.setField(keyword, "id", TEST_KEYWORD_ID);
                        return keyword;
                    });

            // when
            NotificationKeywordResponse response = notificationKeywordService.subscribe(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.keyword()).isEqualTo("서울");
            assertThat(response.keywordCategoryType()).isEqualTo(KeywordCategoryType.LOCATION);
            verify(notificationKeywordRepository).save(any(NotificationKeyword.class));
        }

        @Test
        @DisplayName("키워드 구독 - 회원이 존재하지 않으면 예외 발생")
        void subscribeKeyword_ThrowsException_WhenMemberNotFound() {
            // given
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationKeywordService.subscribe(TEST_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FcmErrorCode.MEMBER_NOT_FOUND);

            verify(notificationKeywordRepository, never()).save(any(NotificationKeyword.class));
        }

        @Test
        @DisplayName("키워드 구독 - 이미 구독 중이면 예외 발생")
        void subscribeKeyword_ThrowsException_WhenAlreadySubscribed() {
            // given
            Member member = createTestMember();
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                    TEST_MEMBER_ID, TEST_KEYWORD, KeywordType.ABANDONED, KeywordCategoryType.BREED))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> notificationKeywordService.subscribe(TEST_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FcmErrorCode.KEYWORD_ALREADY_SUBSCRIBED);

            verify(notificationKeywordRepository, never()).save(any(NotificationKeyword.class));
        }
    }

    @Nested
    @DisplayName("키워드 구독 해제 테스트")
    class UnsubscribeKeywordTests {

        @Test
        @DisplayName("키워드 구독 해제 - 성공")
        void unsubscribeKeyword_Success() {
            // given
            Member member = createTestMember();
            NotificationKeyword keyword = createTestKeyword(member);

            given(notificationKeywordRepository.findByIdAndMemberId(TEST_KEYWORD_ID, TEST_MEMBER_ID))
                    .willReturn(Optional.of(keyword));
            willDoNothing().given(notificationKeywordRepository).delete(keyword);

            // when
            assertThatCode(() -> notificationKeywordService.unsubscribe(TEST_MEMBER_ID, TEST_KEYWORD_ID))
                    .doesNotThrowAnyException();

            // then
            verify(notificationKeywordRepository).delete(keyword);
        }

        @Test
        @DisplayName("키워드 구독 해제 - 키워드가 존재하지 않으면 예외 발생")
        void unsubscribeKeyword_ThrowsException_WhenKeywordNotFound() {
            // given
            given(notificationKeywordRepository.findByIdAndMemberId(TEST_KEYWORD_ID, TEST_MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationKeywordService.unsubscribe(TEST_MEMBER_ID, TEST_KEYWORD_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FcmErrorCode.KEYWORD_NOT_FOUND);

            verify(notificationKeywordRepository, never()).delete(any(NotificationKeyword.class));
        }
    }

    @Nested
    @DisplayName("키워드 목록 조회 테스트")
    class GetSubscribedKeywordsTests {

        @Test
        @DisplayName("키워드 목록 조회 - 성공")
        void getSubscribedKeywords_Success() {
            // given
            Member member = createTestMember();
            List<NotificationKeyword> keywords = List.of(
                    createTestKeyword(member),
                    createAnotherTestKeyword(member)
            );

            given(notificationKeywordRepository.findAllByMemberId(TEST_MEMBER_ID))
                    .willReturn(keywords);

            // when
            List<NotificationKeywordResponse> responses = notificationKeywordService
                    .getSubscribedKeywords(TEST_MEMBER_ID);

            // then
            assertThat(responses).hasSize(2);
            verify(notificationKeywordRepository).findAllByMemberId(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("키워드 목록 조회 - 빈 리스트 반환")
        void getSubscribedKeywords_ReturnsEmptyList() {
            // given
            given(notificationKeywordRepository.findAllByMemberId(TEST_MEMBER_ID))
                    .willReturn(List.of());

            // when
            List<NotificationKeywordResponse> responses = notificationKeywordService
                    .getSubscribedKeywords(TEST_MEMBER_ID);

            // then
            assertThat(responses).isEmpty();
            verify(notificationKeywordRepository).findAllByMemberId(TEST_MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("키워드 일괄 업데이트 테스트")
    class BulkUpdateKeywordsTests {

        @Test
        @DisplayName("키워드 일괄 업데이트 - 추가만 있는 경우 성공")
        void bulkUpdateKeywords_Success_OnlyAdd() {
            // given
            Member member = createTestMember();
            List<NotificationKeywordRequest> keywordsToAdd = List.of(
                    new NotificationKeywordRequest(TEST_KEYWORD, KeywordType.ABANDONED,
                            KeywordCategoryType.BREED),
                    new NotificationKeywordRequest("서울", KeywordType.ABANDONED,
                            KeywordCategoryType.LOCATION)
            );

            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    keywordsToAdd,
                    List.of()
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                    anyLong(), anyString(), any(), any()))
                    .willReturn(false);
            given(notificationKeywordRepository.save(any(NotificationKeyword.class)))
                    .willAnswer(invocation -> {
                        NotificationKeyword keyword = invocation.getArgument(0);
                        ReflectionTestUtils.setField(keyword, "id", 1L);
                        return keyword;
                    });

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).hasSize(2);
            assertThat(response.deletedKeywordIds()).isEmpty();
            verify(notificationKeywordRepository, times(2)).save(any(NotificationKeyword.class));
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 삭제만 있는 경우 성공")
        void bulkUpdateKeywords_Success_OnlyDelete() {
            // given
            Member member = createTestMember();
            NotificationKeyword keyword1 = createTestKeyword(member);
            NotificationKeyword keyword2 = createAnotherTestKeyword(member);
            List<Long> keywordIdsToDelete = List.of(1L, 2L);

            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    List.of(),
                    keywordIdsToDelete
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(notificationKeywordRepository.findByIdAndMemberId(1L, TEST_MEMBER_ID))
                    .willReturn(Optional.of(keyword1));
            given(notificationKeywordRepository.findByIdAndMemberId(2L, TEST_MEMBER_ID))
                    .willReturn(Optional.of(keyword2));
            willDoNothing().given(notificationKeywordRepository).delete(any(NotificationKeyword.class));

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).isEmpty();
            assertThat(response.deletedKeywordIds()).hasSize(2);
            verify(notificationKeywordRepository, times(2)).delete(any(NotificationKeyword.class));
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 추가와 삭제 모두 있는 경우 성공")
        void bulkUpdateKeywords_Success_AddAndDelete() {
            // given
            Member member = createTestMember();
            NotificationKeyword existingKeyword = createTestKeyword(member);

            List<NotificationKeywordRequest> keywordsToAdd = List.of(
                    new NotificationKeywordRequest("말티즈", KeywordType.ABANDONED,
                            KeywordCategoryType.BREED)
            );
            List<Long> keywordIdsToDelete = List.of(1L);

            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    keywordsToAdd,
                    keywordIdsToDelete
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));
            given(notificationKeywordRepository.findByIdAndMemberId(1L, TEST_MEMBER_ID))
                    .willReturn(Optional.of(existingKeyword));
            given(notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                    anyLong(), anyString(), any(), any()))
                    .willReturn(false);
            given(notificationKeywordRepository.save(any(NotificationKeyword.class)))
                    .willAnswer(invocation -> {
                        NotificationKeyword keyword = invocation.getArgument(0);
                        ReflectionTestUtils.setField(keyword, "id", 2L);
                        return keyword;
                    });
            willDoNothing().given(notificationKeywordRepository).delete(any(NotificationKeyword.class));

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).hasSize(1);
            assertThat(response.deletedKeywordIds()).hasSize(1);
            verify(notificationKeywordRepository).save(any(NotificationKeyword.class));
            verify(notificationKeywordRepository).delete(any(NotificationKeyword.class));
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 빈 요청이면 빈 응답 반환")
        void bulkUpdateKeywords_ReturnsEmpty_WhenEmptyRequest() {
            // given
            Member member = createTestMember();
            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    List.of(),
                    List.of()
            );

            given(memberRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(member));

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).isEmpty();
            assertThat(response.deletedKeywordIds()).isEmpty();
            verify(notificationKeywordRepository, never()).save(any());
            verify(notificationKeywordRepository, never()).delete(any());
        }
    }

    // Helper methods
    private Member createTestMember() {
        Member member = Member.builder()
                .loginId("testuser")
                .nickName("테스트유저")
                .phoneNumber("01012345678")
                .password("encodedPassword")
                .build();
        ReflectionTestUtils.setField(member, "id", TEST_MEMBER_ID);
        ReflectionTestUtils.setField(member, "isPushEnabled", true);
        return member;
    }


    private NotificationKeyword createTestKeyword(Member member) {
        NotificationKeyword keyword = NotificationKeyword.builder()
                .keyword(TEST_KEYWORD)
                .keywordType(KeywordType.ABANDONED)
                .keywordCategoryType(KeywordCategoryType.BREED)
                .member(member)
                .build();
        ReflectionTestUtils.setField(keyword, "id", TEST_KEYWORD_ID);
        return keyword;
    }

    private NotificationKeyword createAnotherTestKeyword(Member member) {
        NotificationKeyword keyword = NotificationKeyword.builder()
                .keyword("서울")
                .keywordType(KeywordType.WITNESS)
                .keywordCategoryType(KeywordCategoryType.LOCATION)
                .member(member)
                .build();
        ReflectionTestUtils.setField(keyword, "id", 2L);
        return keyword;
    }
}