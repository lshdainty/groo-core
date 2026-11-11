package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/** 인증 없이 보호된 API 를 부르면 401 {@code COMMON_411} 을 표준 봉투로 답한다(core 3.2.0). */
@Slf4j
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponder responder;

    public JsonAuthenticationEntryPoint(SecurityErrorResponder responder) {
        this.responder = responder;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("인증되지 않은 접근 시도 - URI: {}, Method: {}", request.getRequestURI(), request.getMethod());
        responder.write(request, response, ErrorCode.UNAUTHORIZED);
    }
}
