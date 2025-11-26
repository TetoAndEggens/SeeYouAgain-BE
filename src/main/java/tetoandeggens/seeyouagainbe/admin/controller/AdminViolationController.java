package tetoandeggens.seeyouagainbe.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.admin.dto.request.ViolationProcessRequest;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationDetailResponse;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationListResponse;
import tetoandeggens.seeyouagainbe.admin.service.AdminViolationService;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;
import tetoandeggens.seeyouagainbe.global.response.PageResponse;

@Tag(name = "Admin Violation", description = "관리자 신고 처리 API")
@RestController
@RequestMapping("/admin/violations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminViolationController {

    private final AdminViolationService adminViolationService;

    @Operation(
            summary = "신고 목록 조회",
            description = "신고 목록을 페이징하여 조회합니다. 상태별 필터링이 가능합니다."
    )
    @GetMapping
    public ApiResponse<PageResponse<ViolationListResponse>> getViolationList(
            @Parameter(description = "신고 상태 (WAITING, VIOLATED, NORMAL)")
            @RequestParam(required = false) ViolatedStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PageResponse<ViolationListResponse> response = adminViolationService.getViolationList(status, pageable);
        return ApiResponse.ok(response);
    }

    @Operation(
            summary = "신고 상세 조회",
            description = "신고 상세 정보를 조회합니다."
    )
    @GetMapping("/{violationId}")
    public ApiResponse<ViolationDetailResponse> getViolationDetail(
            @Parameter(description = "신고 ID")
            @PathVariable Long violationId
    ) {
        ViolationDetailResponse detail = adminViolationService.getViolationDetail(violationId);
        return ApiResponse.ok(detail);
    }

    @Operation(
            summary = "신고 처리",
            description = "신고를 처리합니다. 위반 또는 위반 아님으로 판정하고, 필요 시 콘텐츠를 삭제합니다."
    )
    @PatchMapping("/{violationId}/process")
    public ApiResponse<Void> processViolation(
            @Parameter(description = "신고 ID")
            @PathVariable Long violationId,
            @Valid @RequestBody ViolationProcessRequest request
    ) {
        adminViolationService.processViolation(violationId, request);
        return ApiResponse.noContent();
    }
}
