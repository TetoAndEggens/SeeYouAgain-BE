package tetoandeggens.seeyouagainbe.auth.filter;

import static tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode.*;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.service.CookieService;
import tetoandeggens.seeyouagainbe.auth.util.ResponseUtil;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final TokenProvider tokenProvider;
	private final CookieService cookieService;
	private final String[] whiteList;
	private final String[] blackList;
	private final ObjectMapper objectMapper;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private static final String BOARD_PATH_PREFIX = "/board";
	private static final String HTTP_METHOD_GET = "GET";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		String accessToken = cookieService.resolveAccessToken(request);
		boolean isOptionalAuth = isOptionalAuthPath(request);

		try {
			if (accessToken != null) {
				tokenProvider.validateToken(accessToken);
				Authentication authentication = tokenProvider.getAuthenticationByAccessToken(accessToken);

				if (isBlackListPath(request.getRequestURI())) {
					checkUserBanned(authentication);
				}

				SecurityContextHolder.getContext().setAuthentication(authentication);
			} else if (!isOptionalAuth) {
				SecurityContextHolder.clearContext();
				ResponseUtil.writeErrorResponse(response, objectMapper, TOKEN_NOT_FOUND);
				return;
			}
		} catch (CustomException e) {
			SecurityContextHolder.clearContext();
			if (!isOptionalAuth) {
				ResponseUtil.writeErrorResponse(response, objectMapper, e.getErrorCode());
				return;
			}
		} catch (ExpiredJwtException e) {
			SecurityContextHolder.clearContext();
			if (!isOptionalAuth) {
				ResponseUtil.writeErrorResponse(response, objectMapper, EXPIRED_TOKEN);
				return;
			}
		} catch (IncorrectClaimException e) {
			SecurityContextHolder.clearContext();
			if (!isOptionalAuth) {
				ResponseUtil.writeErrorResponse(response, objectMapper, INCORRECT_CLAIM_TOKEN);
				return;
			}
		} catch (UsernameNotFoundException e) {
			SecurityContextHolder.clearContext();
			if (!isOptionalAuth) {
				ResponseUtil.writeErrorResponse(response, objectMapper, MEMBER_NOT_FOUND);
				return;
			}
		} catch (Exception e) {
			SecurityContextHolder.clearContext();
			if (!isOptionalAuth) {
				ResponseUtil.writeErrorResponse(response, objectMapper, INVALID_TOKEN);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String requestURI = request.getRequestURI();

		boolean isInWhiteList = Arrays.stream(whiteList)
			.anyMatch(pattern -> pathMatcher.match(pattern, requestURI));

		return isInWhiteList;
	}

	private boolean isOptionalAuthPath(HttpServletRequest request) {
		String requestURI = request.getRequestURI();
		String method = request.getMethod();

		return requestURI.startsWith(BOARD_PATH_PREFIX) && HTTP_METHOD_GET.equals(method);
	}

	private void checkUserBanned(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails)authentication.getPrincipal();

		if (userDetails.getIsBanned()) {
			throw new CustomException(ACCOUNT_BANNED);
		}
	}

	private boolean isBlackListPath(String requestURI) {
		return Arrays.stream(blackList)
			.anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
	}
}