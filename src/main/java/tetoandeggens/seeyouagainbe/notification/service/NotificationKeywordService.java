package tetoandeggens.seeyouagainbe.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.NotificationKeywordErrorCode;
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

@Service
@RequiredArgsConstructor
public class NotificationKeywordService {

    private final NotificationKeywordRepository notificationKeywordRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<NotificationKeywordResponse> getSubscribedKeywords(Long memberId) {
        return notificationKeywordRepository.findAllDtoByMemberId(memberId);
    }

    @Transactional
    public NotificationKeywordResponse subscribe(Long memberId, NotificationKeywordRequest request) {
        if (!memberRepository.existsByIdAndIsPushEnabled(memberId, true)) {
            throw new CustomException(AuthErrorCode.PUSH_DISABLED);
        }

        if (notificationKeywordRepository.existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
                memberId, request.keyword(), request.keywordType(), request.keywordCategoryType())) {
            throw new CustomException(NotificationKeywordErrorCode.KEYWORD_ALREADY_SUBSCRIBED);
        }

        NotificationKeyword keyword = NotificationKeyword.builder()
                .keyword(request.keyword())
                .keywordType(request.keywordType())
                .keywordCategoryType(request.keywordCategoryType())
                .member(new Member(memberId))
                .build();

        NotificationKeyword savedKeyword = notificationKeywordRepository.save(keyword);

        return NotificationKeywordResponse.from(savedKeyword);
    }

    @Transactional
    public void unsubscribe(Long memberId, Long keywordId) {
        NotificationKeyword keyword = notificationKeywordRepository.findByIdAndMemberId(keywordId, memberId)
                .orElseThrow(() -> new CustomException(NotificationKeywordErrorCode.KEYWORD_NOT_FOUND));

        notificationKeywordRepository.delete(keyword);
    }

    @Transactional
    public BulkUpdateKeywordsResponse bulkUpdateKeywords(
            Long memberId,
            BulkUpdateKeywordsRequest request
    ) {
        if (!memberRepository.existsByIdAndIsPushEnabled(memberId, true)) {
            throw new CustomException(AuthErrorCode.PUSH_DISABLED);
        }

        List<Long> deletedKeywordIds = new ArrayList<>();
        if (request.hasKeywordsToDelete()) {
            deletedKeywordIds = deleteKeywords(memberId, request.keywordIdsToDelete());
        }

        List<NotificationKeywordResponse> addedKeywords = new ArrayList<>();
        if (request.hasKeywordsToAdd()) {
            addedKeywords = addKeywords(memberId, request.keywordsToAdd());
        }

        return BulkUpdateKeywordsResponse.of(addedKeywords, deletedKeywordIds);
    }

    private List<Long> deleteKeywords(Long memberId, List<Long> keywordIdsToDelete) {
        List<NotificationKeyword> keywordsToDelete = notificationKeywordRepository
                .findAllByIdInAndMemberIdOptimized(keywordIdsToDelete, memberId);

        List<Long> validIds = new ArrayList<>();
        for (NotificationKeyword keyword : keywordsToDelete) {
            validIds.add(keyword.getId());
        }

        if (!validIds.isEmpty()) {
            notificationKeywordRepository.deleteAllByIdInBatch(validIds);
        }

        return validIds;
    }

    private List<NotificationKeywordResponse> addKeywords(
            Long memberId,
            List<NotificationKeywordRequest> keywordsToAdd
    ) {
        List<NotificationKeyword> newKeywords = new ArrayList<>();

        for (NotificationKeywordRequest request : keywordsToAdd) {
            boolean exists = notificationKeywordRepository
                    .existsByMemberIdAndKeywordOptimized(
                            memberId,
                            request.keyword(),
                            request.keywordType(),
                            request.keywordCategoryType()
                    );

            if (!exists) {
                NotificationKeyword keyword = NotificationKeyword.builder()
                        .keyword(request.keyword())
                        .keywordType(request.keywordType())
                        .keywordCategoryType(request.keywordCategoryType())
                        .member(new Member(memberId))
                        .build();

                newKeywords.add(keyword);
            }
        }

        if (newKeywords.isEmpty()) {
            return new ArrayList<>();
        }

        List<NotificationKeyword> savedKeywords = notificationKeywordRepository.saveAll(newKeywords);

        List<NotificationKeywordResponse> responses = new ArrayList<>();
        for (NotificationKeyword saved : savedKeywords) {
            responses.add(NotificationKeywordResponse.from(saved));
        }

        return responses;
    }
}