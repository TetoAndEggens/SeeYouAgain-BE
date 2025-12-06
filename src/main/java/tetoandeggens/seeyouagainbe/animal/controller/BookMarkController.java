package tetoandeggens.seeyouagainbe.animal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkResponse;
import tetoandeggens.seeyouagainbe.animal.service.BookMarkService;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

import java.util.List;

@Tag(name = "BookMark", description = "북마크(찜) 관련 API")
@RestController
@RequestMapping("/bookmark")
@RequiredArgsConstructor
public class BookMarkController {

    private final BookMarkService bookMarkService;

    @Operation(
            summary = "내 북마크 목록 조회 API",
            description = "내가 북마크한 유기 동물 목록을 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<BookMarkAnimalResponse>> getMyBookMarks(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<BookMarkAnimalResponse> response = bookMarkService.getMyBookMarks(userDetails.getMemberId());
        return ApiResponse.ok(response);
    }

    @Operation(
            summary = "북마크 토글 API",
            description = "유기 동물에 대한 북마크를 추가하거나 취소합니다. 북마크가 없으면 추가, 있으면 삭제 상태를 토글합니다."
    )
    @PostMapping("/animals/{animalId}")
    public ApiResponse<BookMarkResponse> toggleBookMark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long animalId
    ) {
        BookMarkResponse response = bookMarkService.toggleBookMark(userDetails.getMemberId(), animalId);
        return ApiResponse.ok(response);
    }
}
