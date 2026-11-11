package io.groo.core.logging;

import io.groo.core.security.AuditorPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 요청 로그 필터 공통 뼈대 — 서비스가 여는 자리(제외 경로·사용자 식별·마스킹)가 의도대로 움직이는지.
 * 본문 사본·응답 복사 같은 흐름은 각 서비스의 RoundTrip 테스트가 실제 필터 체인으로 지킨다.
 */
class AbstractRequestResponseLoggingFilterTest {

    /** 서비스 쪽 하위 클래스를 흉내 낸다. */
    static class SampleFilter extends AbstractRequestResponseLoggingFilter {
        SampleFilter() {
            super(SensitiveDataMasker.withExtraKeys("clientId"));
        }

        @Override
        protected Collection<String> excludedPathPrefixes() {
            return Stream.concat(DEFAULT_EXCLUDED_PATH_PREFIXES.stream(), Stream.of("/api/v1/import/sms")).toList();
        }

        @Override
        protected Collection<String> excludedExactPaths() {
            return List.of("/api/v1/export");
        }

        boolean skips(HttpServletRequest request) {
            return shouldNotFilter(request);
        }

        String user() {
            return currentUserId();
        }
    }

    private final SampleFilter filter = new SampleFilter();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest get(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    @Test
    void 기본_제외_경로와_서비스가_더한_경로는_앞부분_일치로_빠진다() {
        assertThat(filter.skips(get("/actuator/prometheus"))).isTrue();
        assertThat(filter.skips(get("/api/v1/import/sms/parse"))).isTrue();
        assertThat(filter.skips(get("/api/v1/assets"))).isFalse();
    }

    @Test
    void 스트리밍_경로는_정확히_일치할_때만_빠진다() {
        assertThat(filter.skips(get("/api/v1/export"))).isTrue();
        assertThat(filter.skips(get("/api/v1/export/counts"))).isFalse();
    }

    @Test
    void 주체가_AuditorPrincipal_이면_객체_문자열이_아니라_사용자_ID를_남긴다() {
        AuditorPrincipal principal = () -> "lsheng";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES));

        assertThat(filter.user()).isEqualTo("lsheng");
    }

    @Test
    void 익명이면_사용자_없음() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "k", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(filter.user()).isNull();
    }

    @Test
    void 서비스가_준_마스커로_가린다() {
        String masked = filter.maskSensitiveData("{\"clientId\":\"abc\",\"name\":\"x\"}");

        assertThat(masked).doesNotContain("abc").contains("\"name\":\"x\"");
    }

    @Test
    void RequestContextHolder_가_비어도_요청에서_IP_를_찾아_로그에_남긴다() throws Exception {
        java.util.List<String> lines = new java.util.ArrayList<>();
        SampleFilter capturing = new SampleFilter() {
            @Override
            protected void logRequestResponse(org.springframework.web.util.ContentCachingRequestWrapper request,
                                              org.springframework.web.util.ContentCachingResponseWrapper response,
                                              String traceId, long executionTime) {
                lines.add(io.groo.core.util.HttpUtils.getClientIp(request));
            }
        };
        MockHttpServletRequest request = get("/api/v1/assets");
        request.setRemoteAddr("198.51.100.7");

        capturing.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(lines).containsExactly("198.51.100.7");
    }

    @Test
    void 필터를_거친_뒤에도_응답_본문은_그대로_나간다() throws Exception {
        MockHttpServletRequest request = get("/api/v1/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> res.getWriter().write("{\"ok\":true}"));

        assertThat(response.getContentAsString()).isEqualTo("{\"ok\":true}");
    }
}
