package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * {@code /actuator/prometheus} 를 <b>도커 내부망에서 직접 온 요청</b>(Prometheus 컨테이너)에만 연다(core 3.2.0).
 *
 * <p>nginx 가 이 경로를 외부에서 막지만, nginx 설정 한 줄이 빠지면 서버 내부 지표(요청 경로·DB 풀·JVM)가 그대로
 * 공개된다. 그래서 앱에서도 한 번 더 막는다: 전달 헤더(X-Forwarded-For·X-Real-IP·Forwarded)가 하나라도 있으면
 * 프록시를 거친 요청이므로 거절하고, 직접 연결 상대가 사설 대역일 때만 연다. (groo-ieum-back 에서 올림)
 */
@Slf4j
public class PrometheusAccessFilter extends OncePerRequestFilter {

    private static final String PATH = "/actuator/prometheus";
    private static final List<String> FORWARD_HEADERS = List.of("X-Forwarded-For", "X-Real-IP", "Forwarded");

    private final SecurityErrorResponder responder;

    public PrometheusAccessFilter(SecurityErrorResponder responder) {
        this.responder = responder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 원문(getRequestURI)이 아니라 디코딩·정규화한 경로로 본다 — %70rometheus · ;x · // 로 건너뛰지 못하게.
        String path = RequestPaths.normalized(request);
        return !(PATH.equals(path) || (PATH + "/").equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isInternalDirect(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("프록시를 거친 /actuator/prometheus 요청 차단 - remoteAddr: {}", request.getRemoteAddr());
        responder.write(request, response, ErrorCode.FORBIDDEN);
    }

    private static boolean isInternalDirect(HttpServletRequest request) {
        for (String header : FORWARD_HEADERS) {
            if (request.getHeader(header) != null) {
                return false;
            }
        }
        return ClientIpResolver.isTrustedProxy(request.getRemoteAddr());
    }
}
