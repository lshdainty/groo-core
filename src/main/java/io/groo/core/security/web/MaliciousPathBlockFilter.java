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
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 자동 스캐너가 두드리는 경로({@code .git}·{@code .env}·백업·덤프·웹셸·관리 도구)를 컨트롤러에 닿기 전에 403 으로 끊는다
 * (core 3.2.0, groo-ieum-back 에서 올림).
 *
 * <p>Grafana 로그에서 봇이 {@code /api/.env}·{@code /actuator} 를 훑는 것이 실제로 보인다. 이 서비스들은 그런 파일을
 * 내보내지 않으므로 응답 자체는 404 였지만, 이 필터가 있으면 의도가 분명한 요청을 로그에 한 줄로 남기고(WARN) 일찍 끝낸다.
 * 서비스 고유 경로를 더 막으려면 생성자로 패턴을 더한다. 실제 API 와 겹치지 않는지는 추가할 때 확인할 것
 * (지금 기본 목록은 세 서비스의 매핑과 겹치지 않는다 — sso {@code /.well-known/jwks.json} 포함 확인).
 */
@Slf4j
public class MaliciousPathBlockFilter extends OncePerRequestFilter {

    public static final List<Pattern> DEFAULT_PATTERNS = List.of(
            Pattern.compile(".*/\\.git(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.svn(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.hg(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.bzr(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.env(\\..*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.htaccess$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/web\\.config$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/php\\.ini$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/httpd\\.conf$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/nginx\\.conf$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.idea(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.vscode(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.eclipse(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.settings(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.classpath$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.project$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\.DS_Store$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/Thumbs\\.db$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/desktop\\.ini$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.bak$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.backup$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.old$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.orig$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.tmp$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.temp$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.swp$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.swo$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.save$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*~$"),
            Pattern.compile(".*\\.sql$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.sqlite$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.db$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/config\\.php$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/config\\.yml$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/config\\.json$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/application\\.yml$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/application\\.properties$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/application-.*\\.yml$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/application-.*\\.properties$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.tar$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.tar\\.gz$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.tgz$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.zip$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.rar$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.7z$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/eval-stdin\\.php$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/shell\\.php$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/c99\\.php$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/r57\\.php$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/adminer\\.php$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/phpMyAdmin(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/phpmyadmin(/.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.php$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.asp$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.aspx$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.jsp\\.bak$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.java\\.bak$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.log$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/logs(/.*)?$", Pattern.CASE_INSENSITIVE)
    );

    private final List<Pattern> patterns;
    private final SecurityErrorResponder responder;

    public MaliciousPathBlockFilter(SecurityErrorResponder responder) {
        this(responder, List.of());
    }

    public MaliciousPathBlockFilter(SecurityErrorResponder responder, List<Pattern> extraPatterns) {
        this.responder = responder;
        this.patterns = Stream.concat(DEFAULT_PATTERNS.stream(), extraPatterns.stream()).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        // 원문과 디코딩·정규화한 경로를 둘 다 본다 — /%2Eenv · /.git;x/config 처럼 한 글자로 건너뛰지 못하게.
        String path = RequestPaths.normalized(request);
        for (Pattern pattern : patterns) {
            if (pattern.matcher(uri).matches() || pattern.matcher(path).matches()) {
                log.warn("Malicious path blocked - IP: {}, URI: {}, Method: {}, Pattern: {}",
                        ClientIpResolver.resolve(request), uri, request.getMethod(), pattern.pattern());
                responder.write(request, response, ErrorCode.FORBIDDEN);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
