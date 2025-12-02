package tetoandeggens.seeyouagainbe.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;
import tetoandeggens.seeyouagainbe.notification.dto.request.BulkUpdateKeywordsRequest;
import tetoandeggens.seeyouagainbe.notification.dto.request.NotificationKeywordRequest;
import tetoandeggens.seeyouagainbe.notification.dto.response.BulkUpdateKeywordsResponse;
import tetoandeggens.seeyouagainbe.notification.dto.response.NotificationKeywordResponse;
import tetoandeggens.seeyouagainbe.notification.service.NotificationKeywordService;

import java.util.List;

@RestController
@RequestMapping("/notification/keyword")
@RequiredArgsConstructor
public class NotificationKeywordController {

    private final NotificationKeywordService notificationKeywordService;

    @GetMapping
    @Operation(
            summary = "구독 키워드 목록 조회 API",
            description = "사용자가 구독 중인 모든 키워드 목록을 조회"
    )
    public ApiResponse<List<NotificationKeywordResponse>> getSubscribedKeywords(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<NotificationKeywordResponse> keywords = notificationKeywordService
                .getSubscribedKeywords(userDetails.getMemberId());

        return ApiResponse.ok(keywords);
    }

    @PostMapping
    @Operation(
            summary = "키워드 구독 API",
            description = "알림을 받고 싶은 키워드를 구독 (품종 또는 지역)"
    )
    public ApiResponse<NotificationKeywordResponse> subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationKeywordRequest request
    ) {
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
        notificationKeywordService.unsubscribe(userDetails.getMemberId(), keywordId);
        return ApiResponse.noContent();
    }

    @PutMapping("/updateAll")
    @Operation(
            summary = "키워드 일괄 업데이트 API (추천)",
            description = "추가할 키워드와 삭제할 키워드를 한 번에 처리합니다. " +
                    "프론트엔드의 '저장하기' 버튼 클릭 시 사용하는 API입니다."
    )
    public ApiResponse<BulkUpdateKeywordsResponse> bulkUpdateKeywords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BulkUpdateKeywordsRequest request
    ) {
        BulkUpdateKeywordsResponse response = notificationKeywordService.bulkUpdateKeywords(
                userDetails.getMemberId(),
                request
        );

        return ApiResponse.ok(response);
    }
}