package tetoandeggens.seeyouagainbe.board.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.board.dto.request.AnimalBoardRequest;
import tetoandeggens.seeyouagainbe.board.dto.response.AnimalBoardResponse;
import tetoandeggens.seeyouagainbe.board.service.BoardService;
import tetoandeggens.seeyouagainbe.global.response.ApiResponse;

@Tag(name = "Board", description = "게시물  관련 API")
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

	private final BoardService boardService;

	@PostMapping
	@Operation(
		summary = "실종/유기 동물 게시글 작성 API",
		description = "실종/유기 동물 게시글 작성 후 presigned URL 반환")
	public ApiResponse<AnimalBoardResponse> writeAnimalBoard(
		@RequestBody @Valid AnimalBoardRequest request,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		AnimalBoardResponse animalBoardResponse = boardService.writeAnimalBoard(request,
			customUserDetails.getMemberId());
		return ApiResponse.ok(animalBoardResponse);
	}
}
