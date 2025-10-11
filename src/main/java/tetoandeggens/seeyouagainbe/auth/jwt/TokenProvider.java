package tetoandeggens.seeyouagainbe.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.domain.member.entity.Role;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public UserTokenResponse createLoginToken(String uuid, Role role) {
        String accessToken = createAccessToken(uuid, role.getRole());
        String refreshToken = createRefreshToken(uuid, role.getRole());
        saveRefreshToken(uuid, refreshToken);
        return new UserTokenResponse(accessToken, refreshToken);
    }

    public String createAccessToken(String uuid, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(uuid)
                .claim(AuthConstants.ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String createRefreshToken(String uuid, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(uuid)
                .claim(AuthConstants.ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 클레임 문자열이 비어 있습니다.");
        }
        return false;
    }

    public Authentication getAuthenticationByAccessToken(String accessToken) {
        Claims claims = getClaimsFromToken(accessToken);
        String uuid = claims.getSubject();
        String role = claims.get(AuthConstants.ROLE_CLAIM, String.class);

        CustomUserDetails customUserDetails = CustomUserDetails.fromClaims(uuid, role);
        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.info("토큰 만료");
            return e.getClaims();
        } catch (JwtException e) {
            log.error("잘못된 JWT 토큰입니다. {}", e.getMessage());
            throw new IllegalArgumentException("잘못된 JWT 토큰입니다.");
        }
    }

    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearerToken.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthConstants.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(AuthConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpirationMs / 1000));
        response.addCookie(cookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(AuthConstants.REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public void deleteRefreshToken(String uuid) {
        redisTemplate.delete(uuid);
    }

    public String reissueAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        Claims claims = getClaimsFromToken(refreshToken);
        String uuid = claims.getSubject();
        String role = claims.get(AuthConstants.ROLE_CLAIM, String.class);

        String storedRefreshToken = redisTemplate.opsForValue().get(uuid);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Redis에 저장된 Refresh Token과 일치하지 않습니다.");
        }

        return createAccessToken(uuid, role);
    }

    private void saveRefreshToken(String uuid, String refreshToken) {
        redisTemplate.opsForValue().set(
                uuid,
                refreshToken,
                refreshTokenExpirationMs,
                TimeUnit.MILLISECONDS
        );
    }
}
