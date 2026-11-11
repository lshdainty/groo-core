package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.ErrorCodeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * IP 별 고정 구간 레이트리밋(Redis INCR + EXPIRE) — sso 4종·이음 1종이 같은 코드를 복사해 쓰던 것을 올렸다(core 3.2.0).
 *
 * <p>IP 는 {@link ClientIpResolver} 로 정한다. 복사본 일부는 위조 가능한 {@code X-Forwarded-For} 맨 앞 값을 써서
 * 요청마다 헤더를 바꾸면 제한이 통째로 우회됐다.
 *
 * <p><b>Redis 가 죽었을 때</b>는 서비스가 고른다 — {@code failClosed=true} 면 503 으로 막고(비밀번호 찾기처럼 남용 피해가
 * 큰 자리), false 면 제한 없이 통과시킨다(가용성이 더 중요한 자리).
 *
 * <pre>{@code
 * registry.addInterceptor(new RedisRateLimitInterceptor(redis, responder, "sso:rate:pwforgot", 20, Duration.ofMinutes(1), true))
 *         .addPathPatterns("/api/v1/auth/password/forgot");
 * }</pre>
 */
@Slf4j
public class RedisRateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;
    private final SecurityErrorResponder responder;
    private final String keyPrefix;
    private final int maxRequests;
    private final Duration window;
    private final boolean failClosed;

    /**
     * @param keyPrefix   Redis 키 앞부분(서비스·용도별로 겹치지 않게, 예 {@code sso:rate:pwforgot})
     * @param maxRequests 구간당 허용 횟수 — 넘으면 429
     * @param window      구간 길이
     * @param failClosed  Redis 장애 시 막을지(503) 여부
     */
    public RedisRateLimitInterceptor(StringRedisTemplate redis, SecurityErrorResponder responder, String keyPrefix,
                                     int maxRequests, Duration window, boolean failClosed) {
        this.redis = redis;
        this.responder = responder;
        this.keyPrefix = keyPrefix;
        this.maxRequests = maxRequests;
        this.window = window;
        this.failClosed = failClosed;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = ClientIpResolver.resolve(request);
        String key = keyPrefix + ":" + (ip != null ? ip : "unknown");
        Long count;
        try {
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, window.toSeconds(), TimeUnit.SECONDS);
            }
        } catch (RuntimeException e) {
            if (failClosed) {
                log.warn("레이트리밋 Redis 실패 — 차단(fail-closed): key={}", keyPrefix, e);
                return reject(request, response, ErrorCode.TEMPORARILY_UNAVAILABLE);
            }
            log.warn("레이트리밋 Redis 실패 — 제한 없이 통과(fail-open): key={}, err={}", keyPrefix, e.getMessage());
            return true;
        }
        if (count == null || count > maxRequests) {
            log.warn("rate limit exceeded - key: {}, ip: {}", keyPrefix, ip);
            return reject(request, response, ErrorCode.RATE_LIMITED);
        }
        return true;
    }

    private boolean reject(HttpServletRequest request, HttpServletResponse response, ErrorCodeProvider code)
            throws java.io.IOException {
        responder.write(request, response, code);
        return false;
    }
}
