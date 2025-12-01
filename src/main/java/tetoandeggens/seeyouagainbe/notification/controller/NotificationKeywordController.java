package tetoandeggens.seeyouagainbe.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;
import tetoandeggens.seeyouagainbe.notification.dto.request.NotificationKeywordRequest;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.service.NotificationKeywordService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications/keywords")
@RequiredArgsConstructor
public class NotificationKeywordController {

    private final NotificationKeywordService notificationKeywordService;

    @PostMapping
    @Operation(
            summary = "키워드 구독 API",
            description = "알림을 받고 싶은 키워드를 구독 (품종 또는 지역)"
    )
    public ApiResponse<NotificationKeywordResponse> subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationKeywordRequest request
    ) {
        log.info("키워드 구독 요청 - MemberId: {}, Keyword: {}, Type: {}, Category: {}",
                userDetails.getMemberId(), request.keyword(),
                request.keywordType(), request.keywordCategoryType());

        NotificationKeywordResponse response = notificationKeywordService.subscribe(
                userDetails.getMemberId(),
                request
        );

        return ApiResponse.created(response);
    }

    @DeleteMapping("/{keywordId}")
    @Operation(
            summary = "키워드 구독 해제 API",
            description = "구독 중인 키워드를 구독 해제"
    )
    public ApiResponse<Void> unsubscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long keywordId
    ) {
        log.info("키워드 구독 해제 요청 - MemberId: {}, KeywordId: {}",
                userDetails.getMemberId(), keywordId);

        notificationKeywordService.unsubscribe(userDetails.getMemberId(), keywordId);

        return ApiResponse.noContent();
    }

    @GetMapping
    @Operation(
            summary = "구독 키워드 목록 조회 API",
            description = "사용자가 구독 중인 모든 키워드 목록을 조회"
    )
    public ApiResponse<List<NotificationKeywordResponse>> getSubscribedKeywords(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("구독 키워드 목록 조회 요청 - MemberId: {}", userDetails.getMemberId());

        List<NotificationKeywordResponse> keywords = notificationKeywordService
                .getSubscribedKeywords(userDetails.getMemberId());

        return ApiResponse.ok(keywords);
    }
}