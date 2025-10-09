package tetoandeggens.seeyouagainbe.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tetoandeggens.seeyouagainbe.global.exception.ErrorCode;
import tetoandeggens.seeyouagainbe.global.response.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {

    public static void writeErrorResponse(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            ErrorCode errorCode
    ) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    public static <T> void writeSuccessResponseWithHeaders(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            T data,
            HttpStatus status
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String jsonResponse = objectMapper.writeValueAsString(data);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    public static void writeNoContent(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().flush();
    }
}
