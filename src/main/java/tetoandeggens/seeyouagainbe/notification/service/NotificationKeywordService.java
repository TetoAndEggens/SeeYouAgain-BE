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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationKeywordService {

    private final NotificationKeywordRepository notificationKeywordRepository;
    private final MemberRepository memberRepository;

    public List<NotificationKeywordResponse> getSubscribedKeywords(Long memberId) {
        return notificationKeywordRepository.findAllByMemberId(memberId).stream()
                .map(NotificationKeywordResponse::from)
                .collect(Collectors.toList());
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
        log.info("키워드 구독 완료 - MemberId: {}, Keyword: {}", memberId, request.keyword());

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
            log.warn("빈 일괄 업데이트 요청 - MemberId: {}", memberId);
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
            log.info("키워드 일괄 삭제 완료 - MemberId: {}, 삭제 개수: {}",
                    memberId, deletedKeywordIds.size());
        }

        // 4. 추가 처리 (나중에 추가)
        List<NotificationKeywordResponse> addedKeywords = new ArrayList<>();
        if (request.hasKeywordsToAdd()) {
            addedKeywords = addKeywords(memberId, member, request.keywordsToAdd());
            log.info("키워드 일괄 추가 완료 - MemberId: {}, 추가 개수: {}",
                    memberId, addedKeywords.size());
        }

        log.info("키워드 일괄 업데이트 완료 - MemberId: {}, 추가: {}개, 삭제: {}개",
                memberId, addedKeywords.size(), deletedKeywordIds.size());

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

                log.debug("키워드 삭제 - KeywordId: {}, Keyword: {}",
                        keywordId, keyword.getKeyword());
            } catch (Exception e) {
                log.error("키워드 삭제 실패 - KeywordId: {}", keywordId, e);
                // 하나 실패해도 계속 진행 (선택적 처리)
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
                    log.warn("이미 구독 중인 키워드 - MemberId: {}, Keyword: {}",
                            memberId, request.keyword());
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

                log.debug("키워드 추가 - Keyword: {}, Type: {}, Category: {}",
                        request.keyword(), request.keywordType(), request.keywordCategoryType());
            } catch (Exception e) {
                log.error("키워드 추가 실패 - Keyword: {}", request.keyword(), e);
            }
        }

        return addedKeywords;
    }
}