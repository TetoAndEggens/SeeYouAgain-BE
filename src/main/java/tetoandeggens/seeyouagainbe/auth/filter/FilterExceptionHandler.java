package tetoandeggens.seeyouagainbe.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * 필터 체인에서 발생하는 모든 예외를 GlobalExceptionHandler로 위임하는 필터
 */
@Slf4j
@Component
public class FilterExceptionHandler extends OncePerRequestFilter {

    private final HandlerExceptionResolver resolver;

    public FilterExceptionHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("Spring Security 필터 체인에서 예외 발생: {}", ex.getMessage(), ex);
            resolver.resolveException(request, response, null, ex);
        }
    }
}
