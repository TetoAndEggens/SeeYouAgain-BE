package tetoandeggens.seeyouagainbe.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tetoandeggens.seeyouagainbe.auth.service.OAuth2Service;

import java.io.IOException;

@Tag(name = "OAuth2 Callback", description = "OAuth2 인증 콜백 처리 API")
@RestController
@RequiredArgsConstructor
public class OAuth2CallbackController {

    private final OAuth2Service oAuth2Service;

    @Operation(
            summary = "[OAuth2] 카카오 로그인 콜백",
            description = "카카오 OAuth2 인증 후 콜백 처리\n\n" +
                    "요청 경로: GET /login/oauth2/code/kakao\n" +
                    "카카오 콘솔 Redirect URI와 일치해야 함\n" +
                    "- http://localhost:8080/login/oauth2/code/kakao\n" +
                    "- https://dev-api.seeyouagain.store/login/oauth2/code/kakao"
    )
    @GetMapping("/login/oauth2/code/kakao")
    public void kakaoCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) throws IOException {
        oAuth2Service.socialLogin("kakao", code, response);
    }

    @Operation(
            summary = "[OAuth2] 네이버 로그인 콜백",
            description = "네이버 OAuth2 인증 후 콜백 처리\n\n" +
                    "요청 경로: GET /login/oauth2/code/naver\n" +
                    "네이버 콘솔 Redirect URI와 일치해야 함\n" +
                    "- http://localhost:8080/login/oauth2/code/naver\n" +
                    "- https://dev-api.seeyouagain.store/login/oauth2/code/naver"
    )
    @GetMapping("/login/oauth2/code/naver")
    public void naverCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response
    ) throws IOException {
        oAuth2Service.socialLogin("naver", code, response);
    }

    @Operation(
            summary = "[OAuth2] 구글 로그인 콜백",
            description = "구글 OAuth2 인증 후 콜백 처리\n\n" +
                    "요청 경로: GET /login/oauth2/code/google\n" +
                    "구글 콘솔 Redirect URI와 일치해야 함\n" +
                    "- http://localhost:8080/login/oauth2/code/google\n" +
                    "- https://dev-api.seeyouagain.store/login/oauth2/code/google"
    )
    @GetMapping("/login/oauth2/code/google")
    public void googleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) throws IOException {
        oAuth2Service.socialLogin("google", code, response);
    }
}
