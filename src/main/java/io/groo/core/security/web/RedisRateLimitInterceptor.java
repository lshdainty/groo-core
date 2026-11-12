package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.ErrorCodeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.Inet4Address;
import java.net.InetAddress;
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
        String key = keyPrefix + ":" + subjectOf(ip);
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

    /**
     * 한도를 세는 단위. IPv4 는 주소 하나, IPv6 는 <b>/64 대역</b>이다(core 3.3.0, 보안 점검 SSO-11).
     *
     * <p>IPv6 는 가입자 한 명이 /64(주소 2^64개)를 통째로 받는 게 보통이라, 주소 하나를 키로 쓰면 요청마다
     * 주소를 바꿔 한도를 끝없이 우회한다. 앞 64비트만 키로 쓴다. IPv4-mapped 주소({@code ::ffff:1.2.3.4})는
     * IPv4 로 본다. 리터럴로 읽을 수 없는 값은 원문 그대로 쓴다.
     */
    static String subjectOf(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        if (ip.indexOf(':') < 0) {
            return ip;
        }
        try {
            String literal = ip.startsWith("[") && ip.endsWith("]") ? ip.substring(1, ip.length() - 1) : ip;
            int zone = literal.indexOf('%');
            if (zone >= 0) {
                literal = literal.substring(0, zone);
            }
            InetAddress address = InetAddress.ofLiteral(literal);
            if (address instanceof Inet4Address) {
                return address.getHostAddress();
            }
            byte[] bytes = address.getAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i += 2) {
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(Integer.toHexString(((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff)));
            }
            return sb.append("::/64").toString();
        } catch (IllegalArgumentException e) {
            return ip;
        }
    }

    private boolean reject(HttpServletRequest request, HttpServletResponse response, ErrorCodeProvider code)
            throws java.io.IOException {
        responder.write(request, response, code);
        return false;
    }
}
