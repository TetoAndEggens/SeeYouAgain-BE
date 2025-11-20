package tetoandeggens.seeyouagainbe.board.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.board.dto.request.WritingBoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardDetailResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardListResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.PresignedUrlResponse;
import tetoandeggens.seeyouagainbe.board.service.BoardService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

@Tag(name = "Board", description = "게시물  관련 API")
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

	private final BoardService boardService;

	@PostMapping
	@Operation(
		summary = "실종/목격 동물 게시글 작성 API",
		description = "실종/목격 동물 게시글 작성 후 presigned URL 반환")
	public ApiResponse<PresignedUrlResponse> writeAnimalBoard(
		@RequestBody @Valid WritingBoardRequest request,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		PresignedUrlResponse presignedUrlResponse = boardService.writeAnimalBoard(request,
			customUserDetails.getMemberId());
		return ApiResponse.ok(presignedUrlResponse);
	}

	@GetMapping({"/list", "/list/{type}"})
	@Operation(
		summary = "실종/목격 동물 게시글 리스트 조회 API",
		description = "실종/목격 동물 게시글 리스트 조회")
	public ApiResponse<BoardListResponse> getAnimalBoardList(
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection,
		@PathVariable(required = false) String type
	) {
		BoardListResponse animalBoardList = boardService.getAnimalBoardList(request, sortDirection, type);

		return ApiResponse.ok(animalBoardList);
	}

	@GetMapping("/{boardId}")
	@Operation(
		summary = "실종/목격 동물 게시글 조회 API",
		description = "실종/목격 동물 게시글 조회")
	public ApiResponse<BoardDetailResponse> getAnimalBoard(
		@PathVariable Long boardId) {
		BoardDetailResponse response = boardService.getAnimalBoard(boardId);

		return ApiResponse.ok(response);
	}

	@DeleteMapping("/{boardId}")
	@Operation(
		summary = "실종/목격 동물 게시글 삭제 API",
		description = "실종/목격 동물 게시글 삭제")
	public ApiResponse<Void> deleteAnimalBoard(
		@PathVariable Long boardId,
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		boardService.deleteAnimalBoard(boardId, customUserDetails.getMemberId());

		return ApiResponse.noContent();
	}
}
