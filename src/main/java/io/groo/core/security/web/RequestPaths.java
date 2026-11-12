package io.groo.core.security.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

/**
 * 보안 필터가 경로를 비교할 때 쓰는 <b>정규화된 경로</b>(core 3.3.0).
 *
 * <p>{@code getRequestURI()} 는 디코딩 전 원문이다. 그런데 톰캣과 스프링은 디코딩·정규화한 경로로 컨트롤러를
 * 고른다. 그래서 원문으로 비교하는 필터는 {@code /actuator/%70rometheus} · {@code /actuator/prometheus;x} ·
 * {@code /actuator//prometheus} 처럼 한 글자만 바꾼 요청을 놓치고, 요청은 그대로 엔드포인트에 닿는다
 * (보안 점검 SSO-11). 여기서는 퍼센트 디코딩 · {@code ;} 파라미터 제거 · 연속 슬래시 정리를 한 경로를 돌려준다.
 */
public final class RequestPaths {

    private static final UrlPathHelper DECODING = new UrlPathHelper();

    static {
        DECODING.setUrlDecode(true);
        DECODING.setRemoveSemicolonContent(true);
    }

    private RequestPaths() {
    }

    /** 컨텍스트 경로를 뺀, 디코딩·정규화된 경로. 디코딩에 실패하면 원문 기준으로 정리한다. */
    public static String normalized(HttpServletRequest request) {
        String path;
        try {
            path = DECODING.getPathWithinApplication(request);
        } catch (RuntimeException e) {
            path = request.getRequestURI();
        }
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.replaceAll("/{2,}", "/");
    }
}
