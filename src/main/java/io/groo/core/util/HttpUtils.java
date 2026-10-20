package io.groo.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 요청 유틸리티
 * <p>
 * 현재 HTTP 요청에서 다양한 정보를 추출하는 유틸리티입니다.
 * Spring의 {@link RequestContextHolder}를 통해 현재 요청에 접근합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>클라이언트 IP 주소 추출 (프록시/로드밸런서 지원)</li>
 *   <li>User-Agent 추출</li>
 *   <li>Referer 추출</li>
 *   <li>AJAX 요청 여부 확인</li>
 *   <li>요청 URI/URL 추출</li>
 *   <li>요청 파라미터 추출</li>
 *   <li>헤더 값 추출</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 클라이언트 IP 조회
 * String clientIp = HttpUtils.getClientIp();
 * log.info("접속 IP: {}", clientIp);
 *
 * // User-Agent 확인
 * String userAgent = HttpUtils.getUserAgent();
 * if (userAgent != null && userAgent.contains("Mobile")) {
 *     // 모바일 기기
 * }
 *
 * // AJAX 요청 확인
 * if (HttpUtils.isAjaxRequest()) {
 *     // AJAX 요청 처리
 * }
 *
 * // 현재 요청 URI
 * String uri = HttpUtils.getRequestUri();
 * log.info("Request URI: {}", uri);
 *
 * // 특정 헤더 값 추출
 * String authHeader = HttpUtils.getHeader("Authorization");
 * }</pre>
 *
 * <h3>IP 추출 시 프록시 헤더 확인 순서</h3>
 * <ol>
 *   <li>X-Forwarded-For</li>
 *   <li>Proxy-Client-IP</li>
 *   <li>WL-Proxy-Client-IP (WebLogic)</li>
 *   <li>HTTP_CLIENT_IP</li>
 *   <li>HTTP_X_FORWARDED_FOR</li>
 *   <li>request.getRemoteAddr() (기본)</li>
 * </ol>
 *
 * <h3>참고사항</h3>
 * <ul>
 *   <li>웹 요청 컨텍스트가 없는 경우 null을 반환합니다.</li>
 *   <li>배치 작업이나 비동기 스레드에서는 요청 정보를 가져올 수 없습니다.</li>
 *   <li>X-Forwarded-For에 여러 IP가 있는 경우 첫 번째 IP를 반환합니다.</li>
 * </ul>
 *
 * @author groo
 * @see HttpServletRequest
 * @see RequestContextHolder
 */
public final class HttpUtils {

    private HttpUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 현재 HttpServletRequest 가져오기
     *
     * @return 현재 요청 객체, 요청 컨텍스트가 없으면 null
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    // ========================================
    // IP 주소
    // ========================================

    /**
     * 현재 HTTP 요청의 클라이언트 IP 주소 조회
     * <p>
     * 프록시나 로드 밸런서를 통한 요청의 경우에도
     * 실제 클라이언트의 IP 주소를 정확하게 추출합니다.
     *
     * <h4>반환 예시</h4>
     * <ul>
     *   <li>일반 접속: "203.0.113.50"</li>
     *   <li>프록시 경유: "203.0.113.50" (X-Forwarded-For에서 추출)</li>
     *   <li>localhost: "127.0.0.1" 또는 "0:0:0:0:0:0:0:1"</li>
     *   <li>요청 없음: null</li>
     * </ul>
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String clientIp = HttpUtils.getClientIp();
     * log.info("사용자 접속 IP: {}", clientIp);
     *
     * // IP 기반 접근 제어
     * if (clientIp != null && clientIp.startsWith("192.168.")) {
     *     // 내부 네트워크 접속
     * }
     * }</pre>
     *
     * @return 클라이언트 IP 주소, HTTP 요청 컨텍스트가 없으면 null
     */
    public static String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        return getClientIp(request);
    }

    /**
     * HttpServletRequest에서 클라이언트 IP 주소 조회
     * <p>
     * 프록시 관련 헤더들을 순서대로 확인하여 실제 클라이언트 IP를 추출합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * @GetMapping("/api/info")
     * public String getInfo(HttpServletRequest request) {
     *     String ip = HttpUtils.getClientIp(request);
     *     return "Your IP: " + ip;
     * }
     * }</pre>
     *
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소, request가 null이면 null
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");

        if (isInvalidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 헤더에 여러 IP가 있을 수 있으므로 첫 번째 IP를 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * IP 주소가 유효하지 않은지 확인
     *
     * @param ip 검사할 IP 주소
     * @return 유효하지 않으면 true
     */
    private static boolean isInvalidIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }

    // ========================================
    // 헤더 정보
    // ========================================

    /**
     * User-Agent 헤더 값 반환
     * <p>
     * 클라이언트의 브라우저/앱 정보를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String userAgent = HttpUtils.getUserAgent();
     * // 결과: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ..."
     *
     * // 모바일 기기 확인
     * if (userAgent != null && userAgent.toLowerCase().contains("mobile")) {
     *     // 모바일 처리
     * }
     * }</pre>
     *
     * @return User-Agent 문자열, 없으면 null
     */
    public static String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    /**
     * Referer 헤더 값 반환
     * <p>
     * 이전 페이지 URL을 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String referer = HttpUtils.getReferer();
     * log.info("이전 페이지: {}", referer);
     * }</pre>
     *
     * @return Referer URL, 없으면 null
     */
    public static String getReferer() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader("Referer") : null;
    }

    /**
     * 지정된 헤더 값 반환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String authHeader = HttpUtils.getHeader("Authorization");
     * String contentType = HttpUtils.getHeader("Content-Type");
     * }</pre>
     *
     * @param headerName 헤더 이름
     * @return 헤더 값, 없으면 null
     */
    public static String getHeader(String headerName) {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader(headerName) : null;
    }

    /**
     * Accept-Language 헤더 값 반환
     * <p>
     * 클라이언트가 선호하는 언어를 반환합니다.
     *
     * @return Accept-Language 값, 없으면 null
     */
    public static String getAcceptLanguage() {
        return getHeader("Accept-Language");
    }

    /**
     * Content-Type 헤더 값 반환
     *
     * @return Content-Type 값, 없으면 null
     */
    public static String getContentType() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getContentType() : null;
    }

    // ========================================
    // 요청 정보
    // ========================================

    /**
     * 요청 URI 반환
     * <p>
     * 쿼리 스트링을 제외한 요청 경로를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String uri = HttpUtils.getRequestUri();
     * // 결과: "/api/v1/users"
     * }</pre>
     *
     * @return 요청 URI, 요청이 없으면 null
     */
    public static String getRequestUri() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getRequestURI() : null;
    }

    /**
     * 전체 요청 URL 반환
     * <p>
     * 쿼리 스트링을 포함한 전체 URL을 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String url = HttpUtils.getRequestUrl();
     * // 결과: "https://example.com/api/v1/users?page=1"
     * }</pre>
     *
     * @return 전체 요청 URL, 요청이 없으면 null
     */
    public static String getRequestUrl() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        StringBuffer url = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            url.append("?").append(queryString);
        }

        return url.toString();
    }

    /**
     * HTTP 메서드 반환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String method = HttpUtils.getMethod();
     * // 결과: "GET", "POST", "PUT", "DELETE" 등
     * }</pre>
     *
     * @return HTTP 메서드, 요청이 없으면 null
     */
    public static String getMethod() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getMethod() : null;
    }

    /**
     * 쿼리 스트링 반환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String query = HttpUtils.getQueryString();
     * // 결과: "page=1&size=20"
     * }</pre>
     *
     * @return 쿼리 스트링, 없으면 null
     */
    public static String getQueryString() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getQueryString() : null;
    }

    /**
     * 요청 파라미터 값 반환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String page = HttpUtils.getParameter("page");
     * String keyword = HttpUtils.getParameter("keyword");
     * }</pre>
     *
     * @param name 파라미터 이름
     * @return 파라미터 값, 없으면 null
     */
    public static String getParameter(String name) {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getParameter(name) : null;
    }

    /**
     * 모든 요청 파라미터를 Map으로 반환
     * <p>
     * 현재 HTTP 요청의 모든 쿼리 파라미터와 폼 파라미터를 Map으로 반환합니다.
     * 동일한 이름의 파라미터가 여러 개인 경우 배열로 반환됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // GET /api/search?keyword=test&category=A&category=B
     * Map<String, String[]> params = HttpUtils.getParameterMap();
     * // params = {"keyword": ["test"], "category": ["A", "B"]}
     *
     * String[] categories = params.get("category");
     * // categories = ["A", "B"]
     * }</pre>
     *
     * @return 파라미터 Map, 요청이 없으면 빈 Map
     */
    public static Map<String, String[]> getParameterMap() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return Collections.emptyMap();
        }
        return request.getParameterMap();
    }

    /**
     * 모든 요청 파라미터를 단일값 Map으로 반환
     * <p>
     * 동일한 이름의 파라미터가 여러 개인 경우 첫 번째 값만 반환됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // GET /api/users?page=1&size=20&sort=name
     * Map<String, String> params = HttpUtils.getParameterMapSingleValue();
     * // params = {"page": "1", "size": "20", "sort": "name"}
     * }</pre>
     *
     * @return 파라미터 Map (단일값), 요청이 없으면 빈 Map
     */
    public static Map<String, String> getParameterMapSingleValue() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                result.put(key, values[0]);
            }
        });
        return result;
    }

    // ========================================
    // 요청 유형 확인
    // ========================================

    /**
     * AJAX 요청 여부 확인
     * <p>
     * X-Requested-With 헤더가 "XMLHttpRequest"인지 확인합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * if (HttpUtils.isAjaxRequest()) {
     *     // AJAX 요청 - JSON 응답
     * } else {
     *     // 일반 요청 - 페이지 리다이렉트
     * }
     * }</pre>
     *
     * @return AJAX 요청이면 true
     */
    public static boolean isAjaxRequest() {
        String requestedWith = getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    /**
     * JSON 요청 여부 확인
     * <p>
     * Content-Type이 application/json인지 확인합니다.
     *
     * @return JSON 요청이면 true
     */
    public static boolean isJsonRequest() {
        String contentType = getContentType();
        return contentType != null && contentType.contains("application/json");
    }

    /**
     * 멀티파트 요청 여부 확인
     * <p>
     * 파일 업로드 요청인지 확인합니다.
     *
     * @return 멀티파트 요청이면 true
     */
    public static boolean isMultipartRequest() {
        String contentType = getContentType();
        return contentType != null && contentType.contains("multipart/form-data");
    }

    /**
     * HTTPS 요청 여부 확인
     *
     * @return HTTPS 요청이면 true
     */
    public static boolean isSecure() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }

        // 직접 HTTPS인 경우
        if (request.isSecure()) {
            return true;
        }

        // 프록시/로드밸런서 뒤에 있는 경우
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto);
    }

    // ========================================
    // 기타
    // ========================================

    /**
     * 세션 ID 반환
     * <p>
     * 현재 세션의 ID를 반환합니다. 세션이 없으면 null을 반환합니다.
     *
     * @return 세션 ID, 세션이 없으면 null
     */
    public static String getSessionId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null || request.getSession(false) == null) {
            return null;
        }
        return request.getSession().getId();
    }

    /**
     * 컨텍스트 경로 반환
     * <p>
     * 애플리케이션의 컨텍스트 경로를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String contextPath = HttpUtils.getContextPath();
     * // 결과: "" 또는 "/myapp"
     * }</pre>
     *
     * @return 컨텍스트 경로
     */
    public static String getContextPath() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getContextPath() : "";
    }

    /**
     * 서버 이름 반환
     * <p>
     * 요청을 받은 서버의 호스트 이름을 반환합니다.
     *
     * @return 서버 이름
     */
    public static String getServerName() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getServerName() : null;
    }

    /**
     * 서버 포트 반환
     *
     * @return 서버 포트, 요청이 없으면 -1
     */
    public static int getServerPort() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getServerPort() : -1;
    }
}
