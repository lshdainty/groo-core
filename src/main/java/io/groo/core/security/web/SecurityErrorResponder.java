package io.groo.core.security.web;

import io.groo.core.controller.ApiResponse;
import io.groo.core.exception.ErrorCodeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 필터·인터셉터처럼 {@code @RestControllerAdvice} 바깥에서 오류를 직접 써야 하는 자리의 응답기(core 3.2.0).
 *
 * <p>컨트롤러 예외와 <b>같은 봉투</b>({@link ApiResponse#error})·같은 다국어 문구로 쓴다. 예전엔 필터마다 JSON 을
 * 손으로 만들어 {@code {"status":401,"message":"인증이 필요합니다."}} 처럼 봉투도 언어도 제각각이었다.
 *
 * <p>core 는 빈을 만들지 않는다. 서비스가 {@code @Bean} 으로 하나 만들어 필터·핸들러에 넘긴다.
 */
public class SecurityErrorResponder {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public SecurityErrorResponder(ObjectMapper objectMapper, MessageSource messageSource, LocaleResolver localeResolver) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCodeProvider errorCode)
            throws IOException {
        Locale locale = localeResolver.resolveLocale(request);
        String message = messageSource.getMessage(errorCode.getMessageKey(), null, errorCode.getCode(), locale);
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode.getCode(), message));
    }
}
