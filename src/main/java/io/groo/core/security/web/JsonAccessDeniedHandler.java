package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/** 인증은 됐지만 권한이 없으면 403 {@code COMMON_412} 를 표준 봉투로 답한다(core 3.2.0). */
@Slf4j
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponder responder;

    public JsonAccessDeniedHandler(SecurityErrorResponder responder) {
        this.responder = responder;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("권한 없는 접근 시도 - URI: {}, Method: {}", request.getRequestURI(), request.getMethod());
        responder.write(request, response, ErrorCode.FORBIDDEN);
    }
}
