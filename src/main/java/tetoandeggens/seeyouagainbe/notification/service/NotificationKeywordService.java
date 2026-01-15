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
import tetoandeggens.seeyouagainbe.notification.dto.request.KeywordCheckDto;
import tetoandeggens.seeyouagainbe.notification.dto.request.NotificationKeywordRequest;
import tetoandeggens.seeyouagainbe.notification.dto.response.BulkUpdateKeywordsResponse;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;
import tetoandeggens.seeyouagainbe.notification.repository.NotificationKeywordRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        boolean exists = notificationKeywordRepository.existsByMemberIdAndKeyword(
                memberId,
                request.keyword(),
                request.keywordType(),
                request.keywordCategoryType()
        );

        if (exists) {
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

        long deletedCount = notificationKeywordRepository.deleteByIdAndMemberId(keywordId, memberId);

        if (deletedCount == 0) {
            throw new CustomException(NotificationKeywordErrorCode.KEYWORD_NOT_FOUND);
        }
    }

    @Transactional
    public BulkUpdateKeywordsResponse bulkUpdateKeywords(
            Long memberId,
            BulkUpdateKeywordsRequest request
    ) {

        if (!memberRepository.existsByIdAndIsPushEnabled(memberId, true)) {
            throw new CustomException(AuthErrorCode.PUSH_DISABLED);
        }

        List<Long> deletedKeywordIds = request.hasKeywordsToDelete()
                ? notificationKeywordRepository.deleteByIdsAndMemberId(request.keywordIdsToDelete(), memberId)
                : List.of();

        List<NotificationKeywordResponse> addedKeywords = request.hasKeywordsToAdd()
                ? addKeywordsBulk(memberId, request.keywordsToAdd())
                : List.of();

        return BulkUpdateKeywordsResponse.of(addedKeywords, deletedKeywordIds);
    }

    private List<NotificationKeywordResponse> addKeywordsBulk(
            Long memberId,
            List<NotificationKeywordRequest> keywordsToAdd
    ) {
        List<KeywordCheckDto> keywordCheckDtos = new ArrayList<>();
        for (NotificationKeywordRequest request : keywordsToAdd) {
            keywordCheckDtos.add(new KeywordCheckDto(
                    request.keyword(),
                    request.keywordType(),
                    request.keywordCategoryType()
            ));
        }

        List<NotificationKeywordResponse> existingKeywords =
                notificationKeywordRepository.findExistingKeywordsByMemberIdAndKeywords(memberId, keywordCheckDtos);

        Set<String> existingKeywordSet = new HashSet<>();
        for (NotificationKeywordResponse existing : existingKeywords) {
            String key = createKeywordKey(
                    existing.keyword(),
                    existing.keywordType(),
                    existing.keywordCategoryType()
            );
            existingKeywordSet.add(key);
        }

        List<NotificationKeyword> newKeywords = new ArrayList<>();
        for (NotificationKeywordRequest request : keywordsToAdd) {
            String key = createKeywordKey(
                    request.keyword(),
                    request.keywordType(),
                    request.keywordCategoryType()
            );

            if (!existingKeywordSet.contains(key)) {
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
            return List.of();
        }

        List<NotificationKeyword> savedKeywords = notificationKeywordRepository.saveAll(newKeywords);

        List<NotificationKeywordResponse> responses = new ArrayList<>();
        for (NotificationKeyword saved : savedKeywords) {
            responses.add(NotificationKeywordResponse.from(saved));
        }

        return responses;
    }

    private String createKeywordKey(String keyword, Object keywordType, Object keywordCategoryType) {
        return keyword + "|" + keywordType + "|" + keywordCategoryType;
    }
}