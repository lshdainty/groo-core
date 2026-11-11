package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 차단 목록의 IP 를 403 으로 끊는다(core 3.2.0, groo-ieum-back 에서 올림). IP 는 {@link ClientIpResolver} 로 정한다 —
 * 위조 가능한 헤더 값으로 판단하면 차단을 쉽게 우회하고, 남의 IP 를 대신 차단당하게 만들 수도 있다.
 */
@Slf4j
public class IpBlockFilter extends OncePerRequestFilter {

    private final IpBlocklist blocklist;
    private final SecurityErrorResponder responder;

    public IpBlockFilter(IpBlocklist blocklist, SecurityErrorResponder responder) {
        this.blocklist = blocklist;
        this.responder = responder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = ClientIpResolver.resolve(request);
        if (blocklist.isBlocked(ip)) {
            log.warn("IP BLOCKED - IP: {}, URI: {}, Method: {}", ip, request.getRequestURI(), request.getMethod());
            responder.write(request, response, ErrorCode.FORBIDDEN);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
