package io.groo.core.logging;

import io.groo.core.security.AuditorPrincipal;
import io.groo.core.util.HttpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 모든 HTTP 요청/응답에 대한 포괄적인 로깅을 수행하는 필터
 * - Trace ID (UUID) 생성 및 MDC 설정
 * - Request/Response Body 캡처
 * - 실행 시간 측정
 * - User ID, Client IP, User-Agent 수집
 * - 가독성 좋은 한 줄 포맷으로 로그 출력
 *
 * <h2>원본 요청·응답은 절대 건드리지 않는다</h2>
 * 본문은 {@link ContentCachingRequestWrapper}/{@link ContentCachingResponseWrapper} 가 들고 있는
 * <b>사본</b>({@code getContentAsByteArray()})만 읽는다. 서블릿 입력 스트림을 직접 읽으면
 * 컨트롤러가 빈 본문을 받아 모든 POST/PUT 이 조용히 깨진다. 마스킹은
 * {@link #maskSensitiveData(String)} 이 <b>새 문자열</b>을 돌려주는 순수 함수라
 * 바이트 배열이나 요청 객체를 제자리에서 고치는 일이 없다.
 *
 * <p>{@code copyBodyToResponse()} 는 로깅이 어떻게 끝나든 반드시 실행된다
 * — {@link #doFilterInternal} 의 중첩 {@code finally} 참고. 빠뜨리면 클라이언트가 빈 응답을 받는다.
 */
public abstract class AbstractRequestResponseLoggingFilter extends OncePerRequestFilter {

    /**
     * 로거 이름은 <b>서비스의 하위 클래스</b>다(예: {@code io.groo.damda...RequestResponseLoggingFilter}).
     * core 클래스 이름으로 찍으면 서비스별 로그 레벨 설정·로그 수집 조건이 한꺼번에 어긋난다.
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String TRACE_ID_KEY = "requestId";
    private static final int MAX_BODY_LENGTH = 500;
    private static final int MAX_USER_AGENT_LENGTH = 50;
    private static final int CONTENT_CACHE_LIMIT = 10 * 1024; // 10KB
    /** 모든 서비스가 빼는 경로(앞부분 일치). 서비스는 {@link #excludedPathPrefixes()} 에서 여기에 더한다. */
    protected static final List<String> DEFAULT_EXCLUDED_PATH_PREFIXES = List.of(
            "/actuator/health",
            "/actuator/prometheus",
            "/favicon.ico"
    );

    private final SensitiveDataMasker masker;

    /**
     * @param masker 요청·응답 본문과 쿼리스트링에 씌울 마스커. 서비스 고유 키는
     *               {@code SensitiveDataMasker.withExtraKeys(...)} 로 얹는다 — core 기본 목록을 고치지 않는다.
     *               {@code static final} 로 한 번만 만들어 넘겨라(요청마다 만들지 말 것).
     */
    protected AbstractRequestResponseLoggingFilter(SensitiveDataMasker masker) {
        this.masker = masker;
    }

    /**
     * 로깅에서 뺄 경로 — <b>앞부분 일치</b>. 기본은 {@link #DEFAULT_EXCLUDED_PATH_PREFIXES}.
     * <p>
     * 본문이 개인정보 원문이라 마스커로 못 가리는 경로(예: 문자 원문을 받는 API), SSE 처럼 끝나지 않는
     * 경로를 여기에 더한다. 하위 경로까지 통째로 빠진다는 점에 주의.
     */
    protected Collection<String> excludedPathPrefixes() {
        return DEFAULT_EXCLUDED_PATH_PREFIXES;
    }

    /**
     * 로깅에서 뺄 경로 — <b>정확 일치</b>. 기본은 없음.
     * <p>
     * 스트리밍(비동기) 응답({@code StreamingResponseBody} 다운로드 등)을 여기에 올린다. 이 필터의
     * {@link ContentCachingResponseWrapper} 는 본문을 캐시에 쓰고 finally 에서 복사하는데, 스트리밍은
     * 초기 dispatch 가 먼저 끝나 <b>빈 캐시</b>가 복사되고 실제 본문이 버려진다(0바이트 다운로드).
     * 앞부분 일치가 아니라 정확 일치인 건 하위의 일반 JSON 경로({@code /export/counts} 등)는 계속 로깅해야
     * 해서다. 빠뜨리면 {@code doFilterInternal} 의 async 경고 로그가 알려 준다.
     */
    protected Collection<String> excludedExactPaths() {
        return List.of();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 제외할 경로는 로깅 없이 통과
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Trace ID 생성 및 MDC 설정
        String traceId = generateTraceId();
        MDC.put(TRACE_ID_KEY, traceId);

        // Request/Response Body를 여러 번 읽을 수 있도록 래핑
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, CONTENT_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // 다음 필터로 전달
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // 중첩 finally 인 이유: 로깅이 Error(예: StackOverflowError)를 던져도
            // copyBodyToResponse() 를 건너뛰면 안 된다. 예전 구조는 세 줄이 나란히 있어서
            // 로깅이 죽으면 응답 본문이 통째로 비어 나갔다 — 로깅은 부가 기능이고,
            // 실패해도 클라이언트 응답은 온전해야 한다.
            try {
                long executionTime = System.currentTimeMillis() - startTime;
                logRequestResponse(wrappedRequest, wrappedResponse, traceId, executionTime);
            } catch (Throwable t) {
                // 로깅은 부가 기능이다 — 실패하면 로그를 포기하고 요청은 통과시킨다.
                // Exception 은 logRequestResponse 안에서 이미 삼켜지므로 여기 오는 건 Error 다.
                // 그걸 밖으로 내보내면 컨테이너가 500 으로 바꿔 치고, 잘 나간 응답이 사고가 된다.
                try {
                    log.error("Failed to log request/response", t);
                } catch (Throwable ignored) {
                    // 로거 자체가 죽은 상황. 여기서 더 할 수 있는 게 없다.
                }
            } finally {
                try {
                    if (request.isAsyncStarted()) {
                        // 스트리밍 응답이 이 필터에 래핑된 채 비동기로 넘어갔다 — 본문은
                        // 아직 캐시에 없고, 여기서 복사하면 빈 응답(Content-Length: 0)이
                        // 커밋되어 이후 쓰이는 본문이 잘린다. 복사를 건너뛰어도 캐시에
                        // 갇힌 본문을 살릴 수는 없다 — 근본 처치는 경로를
                        // excludedExactPaths() 에 올리는 것이고, 이 경고가 그 신호다.
                        log.warn("스트리밍(비동기) 응답이 로깅 필터에 래핑됨 — excludedExactPaths() 에 추가 필요: {}",
                                request.getRequestURI());
                    } else {
                        // Response Body를 실제 응답으로 복사 (중요!)
                        wrappedResponse.copyBodyToResponse();
                    }
                } finally {
                    // MDC 정리
                    MDC.clear();
                }
            }
        }
    }

    /** 제외 경로 판정. 서비스 테스트가 자기 제외 목록을 직접 확인할 수 있게 public 으로 연다. */
    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludedExactPaths().contains(path)
                || excludedPathPrefixes().stream().anyMatch(path::startsWith);
    }

    /**
     * Trace ID 생성 (UUID 기반 8자리)
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 요청/응답 정보를 가독성 좋은 한 줄 포맷으로 로깅
     * 포맷: [traceId] | status | time | METHOD URI | IP:ip | User:user | Agent:agent | Req:body | Res:body
     *
     * <p>가시성이 {@code protected} 인 것은 테스트에서 "로깅이 터져도 응답 본문은 온전하다" 를
     * 재현하기 위해서다({@code RequestResponseLoggingFilterRoundTripTest}). 운영 코드에서
     * 이 메서드를 밖에서 부르지 않는다.
     */
    protected void logRequestResponse(ContentCachingRequestWrapper request,
                                     ContentCachingResponseWrapper response,
                                     String traceId,
                                     long executionTime) {
        try {
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String clientIp = HttpUtils.getClientIp();
            String userId = currentUserId();
            String userAgent = getUserAgent(request);
            String requestBody = getRequestBody(request);
            String responseBody = getResponseBody(response);

            // URI에 쿼리스트링 포함 (쿼리스트링에 담긴 민감값도 마스킹)
            String fullUri = queryString != null ? uri + "?" + maskSensitiveData(queryString) : uri;

            // Body 잘림 여부 확인
            boolean requestBodyTruncated = requestBody != null && requestBody.length() > MAX_BODY_LENGTH;
            boolean responseBodyTruncated = responseBody != null && responseBody.length() > MAX_BODY_LENGTH;

            // 로그 메시지 구성
            StringBuilder logMessage = new StringBuilder();
            logMessage.append(String.format("[%s] | %d | %4dms | %s %s",
                    traceId, status, executionTime, method, fullUri));

            // IP 정보
            logMessage.append(" | IP:").append(clientIp != null ? clientIp : "-");

            // 사용자 정보
            logMessage.append(" | User:").append(userId != null ? userId : "anonymous");

            // User-Agent 정보
            logMessage.append(" | Agent:").append(userAgent != null ? userAgent : "-");

            // Request Body (있는 경우만)
            if (requestBody != null && !requestBody.isEmpty()) {
                logMessage.append(" | Req:").append(truncate(requestBody, MAX_BODY_LENGTH));
            }

            // Response Body (있는 경우만)
            if (responseBody != null && !responseBody.isEmpty()) {
                logMessage.append(" | Res:").append(truncate(responseBody, MAX_BODY_LENGTH));
            }

            // 상태 코드에 따라 로그 레벨 분리
            if (status >= 500) {
                log.error("{}", logMessage);
            } else if (status >= 400) {
                log.warn("{}", logMessage);
            } else {
                log.info("{}", logMessage);
            }

            // Body가 잘린 경우 DEBUG 레벨로 전체 출력.
            // 넘기는 값은 이미 마스킹된 문자열이다(getRequestBody/getResponseBody 가 마스킹해서 돌려준다)
            // — 여기서 원본을 다시 읽으면 잘려서 가려졌던 토큰이 전문으로 되살아난다.
            if (requestBodyTruncated || responseBodyTruncated) {
                logFullBody(traceId, requestBody, responseBody, requestBodyTruncated, responseBodyTruncated);
            }

        } catch (Throwable t) {
            // Exception 이 아니라 Throwable 이다. 로깅에서 난 Error 가 여기를 뚫고 나가면
            // 바깥 finally 의 copyBodyToResponse() 까지 흔든다. 로깅은 여기서 끝내고 삼킨다.
            log.error("Failed to log request/response", t);
        }
    }

    /**
     * 잘린 Body의 <b>마스킹된</b> 전문을 DEBUG 레벨로 출력.
     *
     * <p>인자로 받는 두 문자열은 {@link #getRequestBody}/{@link #getResponseBody} 가 이미 마스킹한
     * 값이다. 원본 바이트를 다시 읽어 찍으면 안 된다 — 한 줄 로그에서 500자로 잘려 안 보이던
     * 완전한 JWT 가 이 DEBUG 줄에 통째로 남는다.
     */
    private void logFullBody(String traceId, String requestBody, String responseBody,
                              boolean requestBodyTruncated, boolean responseBodyTruncated) {
        if (requestBodyTruncated && requestBody != null) {
            log.debug("[{}] Full Request Body: {}", traceId, sanitizeForLog(requestBody));
        }
        if (responseBodyTruncated && responseBody != null) {
            log.debug("[{}] Full Response Body: {}", traceId, sanitizeForLog(responseBody));
        }
    }

    /**
     * 로그 출력을 위해 줄바꿈 제거
     */
    private String sanitizeForLog(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("\n", " ").replace("\r", "");
    }

    /**
     * User-Agent 헤더 추출 (길이 제한 적용).
     *
     * <p>로그에 나가는 헤더는 이것 하나다. {@code Authorization}·{@code Cookie} 는 읽지도
     * 찍지도 않는다 — 늘리지 마라. 그래도 본문·쿼리에 실려 온 {@code Bearer ...} 와 JWT 는
     * core 마스커의 이름 무관 규칙이 마지막으로 막는다.
     */
    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            return truncate(userAgent, MAX_USER_AGENT_LENGTH);
        }
        return null;
    }

    /**
     * 문자열을 최대 길이로 자르고 말줄임표 추가
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        str = str.replace("\n", " ").replace("\r", "");
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Request Body 추출 — 캐시된 <b>사본</b>을 읽고 마스킹한 새 문자열을 돌려준다.
     * 원본 바이트 배열과 서블릿 입력 스트림은 건드리지 않는다.
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return maskSensitiveData(body);
        }
        return null;
    }

    /**
     * Response Body 추출 — 캐시된 <b>사본</b>을 읽고 마스킹한 새 문자열을 돌려준다.
     * 실제 응답 본문은 {@code copyBodyToResponse()} 로 그대로 나간다.
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return maskSensitiveData(body);
        }
        return null;
    }

    /**
     * 현재 인증된 사용자 ID 추출
     */
    /**
     * 로그에 남길 사용자 식별자.
     * <p>
     * 주체가 {@link AuditorPrincipal} 이면 그 {@code getUserId()} 를 쓴다. 예전 복사본들은
     * {@code authentication.getName()} 만 썼는데, 서비스의 JWT 주체 객체는 Spring 이 이름을 꺼낼 줄 모르는
     * 타입이라 {@code toString()} 으로 떨어져 로그에 {@code User:JwtUserPrincipal@2a2e0e54} 가 찍혔다 —
     * 누가 보낸 요청인지 알 수 없었다(2026-11-09 Grafana 로그 분석에서 발견, 3.1.0 수정).
     */
    protected String currentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            if (authentication.getPrincipal() instanceof AuditorPrincipal principal) {
                return principal.getUserId();
            }
            return authentication.getName();
        } catch (Exception e) {
            log.debug("Failed to get current user", e);
            return null;
        }
    }

    /**
     * 민감한 정보 마스킹 — 규칙은 core {@link SensitiveDataMasker} 에 있다.
     *
     * <p><b>로그 문자열 전용이다.</b> 입력을 바꾸지 않고 항상 새 문자열을 돌려준다.
     * 요청·응답 본문과 URL 쿼리스트링을 로그에 담기 <b>직전</b>에만 부른다.
     * 어떤 입력에도 예외를 던지지 않는다(마스커가 내부에서 {@code Throwable} 을 삼키고
     * 원문 대신 {@code ***} 를 돌려준다 — 원문 유출보다 로그 손실이 낫다).
     */
    /** 로그로 나갈 <b>사본</b>에만 씌운다 — 새 문자열을 돌려주는 순수 함수다(테스트에서 직접 부르므로 public). */
    public String maskSensitiveData(String text) {
        return masker.apply(text);
    }
}
