package tetoandeggens.seeyouagainbe.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.FcmErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.notification.dto.request.NotificationKeywordRequest;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;
import tetoandeggens.seeyouagainbe.notification.repository.NotificationKeywordRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationKeywordService {

    private final NotificationKeywordRepository notificationKeywordRepository;
    private final MemberRepository memberRepository;

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

    public List<NotificationKeywordResponse> getSubscribedKeywords(Long memberId) {
        return notificationKeywordRepository.findAllByMemberId(memberId).stream()
                .map(NotificationKeywordResponse::from)
                .collect(Collectors.toList());
    }
}