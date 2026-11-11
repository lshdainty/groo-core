package io.groo.core.security.web;

import io.groo.core.exception.UnauthorizedException;
import io.groo.core.security.LoginUser;
import io.groo.core.util.HttpUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** core 3.2.0 보안 공통 모듈 — 서비스가 넘기는 설정대로 막고 통과시키는지. */
class SecurityWebCommonsTest {

    private SecurityErrorResponder responder;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("message/core-messages");
        source.setDefaultEncoding("UTF-8");
        responder = new SecurityErrorResponder(JsonMapper.builder().build(), source, new FixedLocaleResolver(Locale.KOREAN));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest req(String method, String uri, String remoteAddr) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, uri);
        r.setRemoteAddr(remoteAddr);
        return r;
    }

    @Nested
    class 클라이언트_IP {

        @Test
        void 직접_들어온_요청의_전달_헤더는_믿지_않는다() {
            MockHttpServletRequest r = req("GET", "/", "203.0.113.9");
            r.addHeader("X-Forwarded-For", "1.2.3.4");
            r.addHeader("X-Real-IP", "5.6.7.8");

            assertThat(ClientIpResolver.resolve(r)).isEqualTo("203.0.113.9");
        }

        @Test
        void 우리_nginx_를_거친_요청은_X_Real_IP_를_쓴다() {
            MockHttpServletRequest r = req("GET", "/", "172.18.0.4");
            r.addHeader("X-Real-IP", "198.51.100.7");
            r.addHeader("X-Forwarded-For", "6.6.6.6, 198.51.100.7");

            assertThat(ClientIpResolver.resolve(r)).isEqualTo("198.51.100.7");
        }

        @Test
        void HttpUtils_도_맨_앞_XFF_를_믿지_않는다() {
            MockHttpServletRequest r = req("GET", "/", "172.18.0.4");
            r.addHeader("X-Forwarded-For", "6.6.6.6, 198.51.100.7");   // 6.6.6.6 = 요청자가 넣은 위조값

            assertThat(HttpUtils.getClientIp(r)).isEqualTo("198.51.100.7");
        }
    }

    @Nested
    class 오리진_검사 {

        private MockHttpServletResponse run(MockHttpServletRequest r) throws Exception {
            OriginCheckFilter f = new OriginCheckFilter(List.of("https://damda.hangroo.io/"), "desk_access_token", responder);
            MockHttpServletResponse res = new MockHttpServletResponse();
            f.doFilter(r, res, new MockFilterChain());
            return res;
        }

        private MockHttpServletRequest cookiePost(String origin) {
            MockHttpServletRequest r = req("POST", "/api/v1/expenses", "172.18.0.4");
            r.setCookies(new Cookie("desk_access_token", "jwt"));
            if (origin != null) {
                r.addHeader("Origin", origin);
            }
            return r;
        }

        @Test
        void 허용된_오리진의_쿠키_요청은_통과() throws Exception {
            assertThat(run(cookiePost("https://damda.hangroo.io")).getStatus()).isEqualTo(200);
        }

        @Test
        void 형제_서브도메인에서_온_쿠키_요청은_막는다() throws Exception {
            MockHttpServletResponse res = run(cookiePost("https://evil.hangroo.io"));

            assertThat(res.getStatus()).isEqualTo(403);
            assertThat(res.getContentAsString()).contains("\"success\":false").contains("COMMON_412");
        }

        @Test
        void Bearer_요청과_GET_은_검사하지_않는다() throws Exception {
            MockHttpServletRequest bearer = cookiePost("https://evil.hangroo.io");
            bearer.addHeader("Authorization", "Bearer x");
            assertThat(run(bearer).getStatus()).isEqualTo(200);

            MockHttpServletRequest get = req("GET", "/api/v1/expenses", "172.18.0.4");
            get.setCookies(new Cookie("desk_access_token", "jwt"));
            get.addHeader("Origin", "https://evil.hangroo.io");
            assertThat(run(get).getStatus()).isEqualTo(200);
        }

        @Test
        void 오리진_정규화() {
            assertThat(OriginCheckFilter.originOf("HTTPS://Damda.Hangroo.io:443/path")).isEqualTo("https://damda.hangroo.io");
            assertThat(OriginCheckFilter.originOf("not a url")).isNull();
        }
    }

    @Nested
    class 지표_접근 {

        private MockHttpServletResponse run(MockHttpServletRequest r) throws Exception {
            MockHttpServletResponse res = new MockHttpServletResponse();
            new PrometheusAccessFilter(responder).doFilter(r, res, new MockFilterChain());
            return res;
        }

        @Test
        void 내부망에서_직접_온_요청만_연다() throws Exception {
            assertThat(run(req("GET", "/actuator/prometheus", "172.21.0.2")).getStatus()).isEqualTo(200);
        }

        @Test
        void 프록시를_거쳤거나_외부면_막는다() throws Exception {
            MockHttpServletRequest proxied = req("GET", "/actuator/prometheus", "172.18.0.4");
            proxied.addHeader("X-Real-IP", "198.51.100.7");
            assertThat(run(proxied).getStatus()).isEqualTo(403);
            assertThat(run(req("GET", "/actuator/prometheus", "198.51.100.7")).getStatus()).isEqualTo(403);
        }
    }

    @Nested
    class IP_차단과_스캐너_경로 {

        @Test
        void 차단_목록의_IP_는_403() throws Exception {
            MockHttpServletResponse res = new MockHttpServletResponse();
            new IpBlockFilter("6.6.6.6"::equals, responder).doFilter(req("GET", "/", "6.6.6.6"), res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(403);
        }

        @Test
        void 스캐너_경로는_막고_JWKS_같은_정상_경로는_통과() throws Exception {
            MaliciousPathBlockFilter f = new MaliciousPathBlockFilter(responder, List.of(Pattern.compile(".*/secret$")));
            for (String bad : List.of("/api/.env", "/.git/config", "/backup.sql", "/wp/shell.php", "/x/secret")) {
                MockHttpServletResponse res = new MockHttpServletResponse();
                f.doFilter(req("GET", bad, "172.18.0.4"), res, new MockFilterChain());
                assertThat(res.getStatus()).as(bad).isEqualTo(403);
            }
            MockHttpServletResponse ok = new MockHttpServletResponse();
            f.doFilter(req("GET", "/.well-known/jwks.json", "172.18.0.4"), ok, new MockFilterChain());
            assertThat(ok.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    class 레이트리밋 {

        @SuppressWarnings("unchecked")
        private RedisRateLimitInterceptor interceptor(Long count, boolean failClosed, boolean redisDown) {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            ValueOperations<String, String> ops = mock(ValueOperations.class);
            when(redis.opsForValue()).thenReturn(ops);
            if (redisDown) {
                when(ops.increment(anyString())).thenThrow(new IllegalStateException("redis down"));
            } else {
                when(ops.increment(anyString())).thenReturn(count);
            }
            return new RedisRateLimitInterceptor(redis, responder, "t:rate", 3, Duration.ofMinutes(1), failClosed);
        }

        @Test
        void 한도_안은_통과_넘으면_429() throws Exception {
            MockHttpServletResponse res = new MockHttpServletResponse();
            assertThat(interceptor(3L, true, false).preHandle(req("POST", "/", "172.18.0.4"), res, null)).isTrue();

            MockHttpServletResponse over = new MockHttpServletResponse();
            assertThat(interceptor(4L, true, false).preHandle(req("POST", "/", "172.18.0.4"), over, null)).isFalse();
            assertThat(over.getStatus()).isEqualTo(429);
            assertThat(over.getContentAsString()).contains("COMMON_429");
        }

        @Test
        void Redis_장애_시_fail_closed_는_503_fail_open_은_통과() throws Exception {
            MockHttpServletResponse closed = new MockHttpServletResponse();
            assertThat(interceptor(null, true, true).preHandle(req("POST", "/", "172.18.0.4"), closed, null)).isFalse();
            assertThat(closed.getStatus()).isEqualTo(503);

            assertThat(interceptor(null, false, true).preHandle(req("POST", "/", "172.18.0.4"), new MockHttpServletResponse(), null)).isTrue();
        }
    }

    @Nested
    class 로그인_사용자_인자 {

        record Me(String id) {
        }

        @SuppressWarnings("unused")
        void handler(@LoginUser Me me, Me plain) {
        }

        private final AbstractLoginUserArgumentResolver<Me> resolver = new AbstractLoginUserArgumentResolver<>(Me.class) {
            @Override
            protected Me toLoginUser(Authentication authentication) {
                return authentication.getPrincipal() instanceof String s ? new Me(s) : null;
            }
        };

        private MethodParameter param(int index) throws Exception {
            return new MethodParameter(getClass().getDeclaredMethod("handler", Me.class, Me.class), index);
        }

        @Test
        void 어노테이션과_타입이_맞을_때만_채운다() throws Exception {
            assertThat(resolver.supportsParameter(param(0))).isTrue();
            assertThat(resolver.supportsParameter(param(1))).isFalse();
        }

        @Test
        void 로그인_사용자를_만들고_없으면_기본_401() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("u1", null, AuthorityUtils.NO_AUTHORITIES));
            assertThat(resolver.resolveArgument(param(0), null, null, null)).isEqualTo(new Me("u1"));

            SecurityContextHolder.clearContext();
            assertThatThrownBy(() -> resolver.resolveArgument(param(0), null, null, null))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}
