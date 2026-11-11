package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 쿠키로 로그인을 유지하는 서비스의 CSRF 방어 — 상태를 바꾸는 요청의 Origin(없으면 Referer)이 허용 목록에 있어야 통과(core 3.2.0).
 *
 * <p>세 서비스 모두 CSRF 토큰을 끄고 {@code SameSite=Lax} 쿠키에 기댄다. Lax 는 <b>다른 사이트</b>의 POST 에 쿠키를
 * 안 붙여 주지만, {@code *.hangroo.io} 형제 서브도메인끼리는 "같은 사이트" 라 막지 못한다 — 형제 서브도메인 하나에
 * XSS 가 생기면 그 페이지가 로그인된 사용자의 쿠키로 다른 서비스의 쓰기 API 를 부를 수 있다. 이 필터가 그 자리를 막는다.
 *
 * <p>검사 대상: POST·PUT·PATCH·DELETE 이면서 로그인 쿠키가 실린 요청. {@code Authorization: Bearer} 요청(앱·도구)은
 * 브라우저가 자동으로 붙이는 값이 아니라 CSRF 대상이 아니므로 뺀다. Origin·Referer 가 둘 다 없으면 서버 간 호출·도구로 보고 통과.
 * (groo-ieum-back 에서 올림)
 */
@Slf4j
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final List<String> allowedOrigins;
    private final Set<String> authCookies;
    private final SecurityErrorResponder responder;

    /**
     * @param allowedOrigins    허용할 프론트 오리진(예: {@code https://damda.hangroo.io}). 경로·끝 슬래시는 무시된다
     * @param accessTokenCookie 이 서비스의 로그인 쿠키 이름 — 이 쿠키가 실린 요청만 검사한다
     */
    public OriginCheckFilter(Collection<String> allowedOrigins, String accessTokenCookie, SecurityErrorResponder responder) {
        this(allowedOrigins, Set.of(accessTokenCookie), responder);
    }

    /**
     * @param authCookies 인증에 쓰이는 쿠키 이름들(예: sso 의 세션·관리자 쿠키) — 하나라도 실린 요청을 검사한다
     */
    public OriginCheckFilter(Collection<String> allowedOrigins, Set<String> authCookies, SecurityErrorResponder responder) {
        this.allowedOrigins = allowedOrigins.stream().map(OriginCheckFilter::originOf).filter(Objects::nonNull).toList();
        this.authCookies = Set.copyOf(authCookies);
        this.responder = responder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!needsCheck(request) || isAllowed(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("허용되지 않은 오리진의 상태 변경 요청 차단 - Method: {}, URI: {}, Origin: {}, Referer: {}",
                request.getMethod(), request.getRequestURI(), request.getHeader("Origin"), request.getHeader("Referer"));
        responder.write(request, response, ErrorCode.FORBIDDEN);
    }

    private boolean needsCheck(HttpServletRequest request) {
        if (!UNSAFE_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return false;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return false;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (authCookies.contains(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowed(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return allowedOrigins.contains(originOf(origin));
        }
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return allowedOrigins.contains(originOf(referer));
        }
        return true;
    }

    /** scheme://host[:port] 로 정규화. 기본 포트는 지운다. 해석 못 하면 null. */
    static String originOf(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (port == -1 || (port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
                port = -1;
            }
            return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (port == -1 ? "" : ":" + port);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
