package tetoandeggens.seeyouagainbe.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.FcmErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.notification.dto.request.BulkUpdateKeywordsRequest;
import tetoandeggens.seeyouagainbe.notification.dto.request.NotificationKeywordRequest;
import tetoandeggens.seeyouagainbe.notification.dto.response.BulkUpdateKeywordsResponse;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;
import tetoandeggens.seeyouagainbe.notification.repository.NotificationKeywordRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationKeywordService {

    private final NotificationKeywordRepository notificationKeywordRepository;
    private final MemberRepository memberRepository;

    public List<NotificationKeywordResponse> getSubscribedKeywords(Long memberId) {
        List<NotificationKeyword> keywords = notificationKeywordRepository.findAllByMemberId(memberId);
        List<NotificationKeywordResponse> responses = new ArrayList<>();

        for (NotificationKeyword keyword : keywords) {
            NotificationKeywordResponse response = NotificationKeywordResponse.from(keyword);
            responses.add(response);
        }

        return responses;
    }

    @Transactional
    public NotificationKeywordResponse subscribe(Long memberId, NotificationKeywordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.MEMBER_NOT_FOUND));

        if (!member.getIsPushEnabled()) {
            throw new CustomException(FcmErrorCode.PUSH_DISABLED);
        }

        if (notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                memberId, request.keyword(), request.keywordType(), request.keywordCategoryType())) {
            throw new CustomException(FcmErrorCode.KEYWORD_ALREADY_SUBSCRIBED);
        }

        NotificationKeyword keyword = NotificationKeyword.builder()
                .keyword(request.keyword())
                .keywordType(request.keywordType())
                .keywordCategoryType(request.keywordCategoryType())
                .member(member)
                .build();

        NotificationKeyword savedKeyword = notificationKeywordRepository.save(keyword);

        return NotificationKeywordResponse.from(savedKeyword);
    }

    @Transactional
    public void unsubscribe(Long memberId, Long keywordId) {
        NotificationKeyword keyword = notificationKeywordRepository.findByIdAndMemberId(keywordId, memberId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.KEYWORD_NOT_FOUND));

        notificationKeywordRepository.delete(keyword);
        log.info("키워드 구독 해제 완료 - MemberId: {}, KeywordId: {}", memberId, keywordId);
    }

    @Transactional
    public BulkUpdateKeywordsResponse bulkUpdateKeywords(
            Long memberId,
            BulkUpdateKeywordsRequest request
    ) {
        // 1. 요청이 비어있는지 확인
        if (request.isEmpty()) {
            return BulkUpdateKeywordsResponse.of(new ArrayList<>(), new ArrayList<>());
        }

        // 2. Member 조회 및 푸시 알림 활성화 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.MEMBER_NOT_FOUND));

        if (!member.getIsPushEnabled()) {
            throw new CustomException(FcmErrorCode.PUSH_DISABLED);
        }

        // 3. 삭제 처리 (먼저 삭제)
        List<Long> deletedKeywordIds = new ArrayList<>();
        if (request.hasKeywordsToDelete()) {
            deletedKeywordIds = deleteKeywords(memberId, request.keywordIdsToDelete());
        }

        // 4. 추가 처리 (나중에 추가)
        List<NotificationKeywordResponse> addedKeywords = new ArrayList<>();
        if (request.hasKeywordsToAdd()) {
            addedKeywords = addKeywords(memberId, member, request.keywordsToAdd());
        }

        return BulkUpdateKeywordsResponse.of(addedKeywords, deletedKeywordIds);
    }

    // 키워드 일괄 삭제
    private List<Long> deleteKeywords(Long memberId, List<Long> keywordIdsToDelete) {
        List<Long> deletedIds = new ArrayList<>();

        for (Long keywordId : keywordIdsToDelete) {
            try {
                NotificationKeyword keyword = notificationKeywordRepository
                        .findByIdAndMemberId(keywordId, memberId)
                        .orElseThrow(() -> new CustomException(FcmErrorCode.KEYWORD_NOT_FOUND));

                notificationKeywordRepository.delete(keyword);
                deletedIds.add(keywordId);
            } catch (Exception e) {
                log.error("키워드 삭제 실패 - KeywordId: {}", keywordId, e);
            }
        }

        return deletedIds;
    }

    // 키워드 일괄 추가
    private List<NotificationKeywordResponse> addKeywords(
            Long memberId,
            Member member,
            List<NotificationKeywordRequest> keywordsToAdd
    ) {
        List<NotificationKeywordResponse> addedKeywords = new ArrayList<>();

        for (NotificationKeywordRequest request : keywordsToAdd) {
            try {
                // 중복 체크
                boolean exists = notificationKeywordRepository
                        .existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                                memberId,
                                request.keyword(),
                                request.keywordType(),
                                request.keywordCategoryType()
                        );

                if (exists) {
                    continue; // 중복은 스킵
                }

                // 새 키워드 저장
                NotificationKeyword keyword = NotificationKeyword.builder()
                        .keyword(request.keyword())
                        .keywordType(request.keywordType())
                        .keywordCategoryType(request.keywordCategoryType())
                        .member(member)
                        .build();

                NotificationKeyword savedKeyword = notificationKeywordRepository.save(keyword);
                addedKeywords.add(NotificationKeywordResponse.from(savedKeyword));
            } catch (Exception e) {
                log.error("키워드 추가 실패 - Keyword: {}", request.keyword(), e);
            }
        }

        return addedKeywords;
    }
}