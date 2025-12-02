package tetoandeggens.seeyouagainbe.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
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
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.NotificationKeywordErrorCode;
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
    @DisplayName("키워드 목록 조회 테스트")
    class GetSubscribedKeywordsTests {

        @Test
        @DisplayName("키워드 목록 조회 - 성공 (DTO 직접 조회)")
        void getSubscribedKeywords_Success() {
            // given
            List<NotificationKeywordResponse> mockResponses = List.of(
                    new NotificationKeywordResponse(
                            1L, TEST_KEYWORD, KeywordType.ABANDONED,
                            KeywordCategoryType.BREED, LocalDateTime.now()
                    ),
                    new NotificationKeywordResponse(
                            2L, "서울", KeywordType.WITNESS,
                            KeywordCategoryType.LOCATION, LocalDateTime.now()
                    )
            );

            given(notificationKeywordRepository.findAllDtoByMemberId(TEST_MEMBER_ID))
                    .willReturn(mockResponses);

            // when
            List<NotificationKeywordResponse> responses = notificationKeywordService
                    .getSubscribedKeywords(TEST_MEMBER_ID);

            // then
            assertThat(responses).hasSize(2);
            verify(notificationKeywordRepository).findAllDtoByMemberId(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("키워드 목록 조회 - 빈 리스트 반환")
        void getSubscribedKeywords_ReturnsEmptyList() {
            // given
            given(notificationKeywordRepository.findAllDtoByMemberId(TEST_MEMBER_ID))
                    .willReturn(List.of());

            // when
            List<NotificationKeywordResponse> responses = notificationKeywordService
                    .getSubscribedKeywords(TEST_MEMBER_ID);

            // then
            assertThat(responses).isEmpty();
            verify(notificationKeywordRepository).findAllDtoByMemberId(TEST_MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("키워드 구독 테스트")
    class SubscribeKeywordTests {

        @Test
        @DisplayName("키워드 구독 - 성공 (품종 타입)")
        void subscribeKeyword_Success_Breed() {
            // given
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(true);
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
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    "서울",
                    KeywordType.WITNESS,
                    KeywordCategoryType.LOCATION
            );

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(true);
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
        @DisplayName("키워드 구독 - 푸시 알림이 비활성화되어 있으면 예외 발생")
        void subscribeKeyword_ThrowsException_WhenPushDisabled() {
            // given
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> notificationKeywordService.subscribe(TEST_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PUSH_DISABLED);

            verify(notificationKeywordRepository, never()).save(any(NotificationKeyword.class));
        }

        @Test
        @DisplayName("키워드 구독 - 이미 구독 중이면 예외 발생")
        void subscribeKeyword_ThrowsException_WhenAlreadySubscribed() {
            // given
            NotificationKeywordRequest request = new NotificationKeywordRequest(
                    TEST_KEYWORD,
                    KeywordType.ABANDONED,
                    KeywordCategoryType.BREED
            );

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(true);
            given(notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                    TEST_MEMBER_ID, TEST_KEYWORD, KeywordType.ABANDONED, KeywordCategoryType.BREED))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> notificationKeywordService.subscribe(TEST_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", NotificationKeywordErrorCode.KEYWORD_ALREADY_SUBSCRIBED);

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
                    .hasFieldOrPropertyWithValue("errorCode", NotificationKeywordErrorCode.KEYWORD_NOT_FOUND);

            verify(notificationKeywordRepository, never()).delete(any(NotificationKeyword.class));
        }
    }

    @Nested
    @DisplayName("키워드 일괄 업데이트 테스트")
    class BulkUpdateKeywordsTests {

        @Test
        @DisplayName("키워드 일괄 업데이트 - 추가만 있는 경우 성공")
        void bulkUpdateKeywords_Success_OnlyAdd() {
            // given
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

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(true);
            given(notificationKeywordRepository.existsByMemberIdAndKeywordOptimized(
                    anyLong(), anyString(), any(), any()))
                    .willReturn(false);
            given(notificationKeywordRepository.saveAll(anyList()))
                    .willAnswer(invocation -> {
                        List<NotificationKeyword> keywords = invocation.getArgument(0);
                        for (int i = 0; i < keywords.size(); i++) {
                            ReflectionTestUtils.setField(keywords.get(i), "id", (long) (i + 1));
                        }
                        return keywords;
                    });

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).hasSize(2);
            assertThat(response.deletedKeywordIds()).isEmpty();
            verify(notificationKeywordRepository).saveAll(anyList());
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

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(true);
            given(notificationKeywordRepository.findAllByIdInAndMemberIdOptimized(keywordIdsToDelete, TEST_MEMBER_ID))
                    .willReturn(List.of(keyword1, keyword2));
            willDoNothing().given(notificationKeywordRepository).deleteAllByIdInBatch(anyList());

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).isEmpty();
            assertThat(response.deletedKeywordIds()).hasSize(2);
            verify(notificationKeywordRepository).deleteAllByIdInBatch(anyList());
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

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(true);
            given(notificationKeywordRepository.findAllByIdInAndMemberIdOptimized(keywordIdsToDelete, TEST_MEMBER_ID))
                    .willReturn(List.of(existingKeyword));
            given(notificationKeywordRepository.existsByMemberIdAndKeywordOptimized(
                    anyLong(), anyString(), any(), any()))
                    .willReturn(false);
            given(notificationKeywordRepository.saveAll(anyList()))
                    .willAnswer(invocation -> {
                        List<NotificationKeyword> keywords = invocation.getArgument(0);
                        ReflectionTestUtils.setField(keywords.get(0), "id", 2L);
                        return keywords;
                    });
            willDoNothing().given(notificationKeywordRepository).deleteAllByIdInBatch(anyList());

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).hasSize(1);
            assertThat(response.deletedKeywordIds()).hasSize(1);
            verify(notificationKeywordRepository).saveAll(anyList());
            verify(notificationKeywordRepository).deleteAllByIdInBatch(anyList());
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 중복 키워드는 추가되지 않음")
        void bulkUpdateKeywords_SkipsDuplicates() {
            // given
            List<NotificationKeywordRequest> keywordsToAdd = List.of(
                    new NotificationKeywordRequest(TEST_KEYWORD, KeywordType.ABANDONED,
                            KeywordCategoryType.BREED),
                    new NotificationKeywordRequest("서울", KeywordType.WITNESS,
                            KeywordCategoryType.LOCATION)
            );

            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    keywordsToAdd,
                    List.of()
            );

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(true);
            // 첫 번째는 중복, 두 번째는 중복 아님
            given(notificationKeywordRepository.existsByMemberIdAndKeywordOptimized(
                    TEST_MEMBER_ID, TEST_KEYWORD, KeywordType.ABANDONED, KeywordCategoryType.BREED))
                    .willReturn(true);
            given(notificationKeywordRepository.existsByMemberIdAndKeywordOptimized(
                    TEST_MEMBER_ID, "서울", KeywordType.WITNESS, KeywordCategoryType.LOCATION))
                    .willReturn(false);
            given(notificationKeywordRepository.saveAll(anyList()))
                    .willAnswer(invocation -> {
                        List<NotificationKeyword> keywords = invocation.getArgument(0);
                        ReflectionTestUtils.setField(keywords.get(0), "id", 1L);
                        return keywords;
                    });

            // when
            BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request);

            // then
            assertThat(response.addedKeywords()).hasSize(1);
            assertThat(response.addedKeywords().get(0).keyword()).isEqualTo("서울");
        }

        @Test
        @DisplayName("키워드 일괄 업데이트 - 푸시 알림이 비활성화되어 있으면 예외 발생")
        void bulkUpdateKeywords_ThrowsException_WhenPushDisabled() {
            // given
            BulkUpdateKeywordsRequest request = new BulkUpdateKeywordsRequest(
                    List.of(),
                    List.of()
            );

            given(memberRepository.existsByIdAndIsPushEnabled(TEST_MEMBER_ID, true))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> notificationKeywordService.bulkUpdateKeywords(
                    TEST_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PUSH_DISABLED);

            verify(notificationKeywordRepository, never()).saveAll(anyList());
            verify(notificationKeywordRepository, never()).deleteAllByIdInBatch(anyList());
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