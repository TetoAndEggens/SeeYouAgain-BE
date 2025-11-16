package tetoandeggens.seeyouagainbe.violation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;
import tetoandeggens.seeyouagainbe.violation.dto.request.ViolationCreateRequest;
import tetoandeggens.seeyouagainbe.violation.service.ViolationService;

import java.util.UUID;

@Tag(name = "Violation", description = "신고 관련 API")
@RestController
@RequestMapping("/violation")
@RequiredArgsConstructor
public class ViolationController {
    private final ViolationService violationService;

    @PostMapping
    public ApiResponse<Void> createViolation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ViolationCreateRequest request
    ) {
        violationService.createViolation(
                UUID.fromString(userDetails.getUuid()),
                request
        );

        return ApiResponse.noContent();
    }
}