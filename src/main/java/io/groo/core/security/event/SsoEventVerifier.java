package io.groo.core.security.event;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * SSO Redis 이벤트의 서명을 확인한다 — 담다 · 이음 구독자가 처리하기 전에 부른다(core 3.4.0, 보안 점검 IEU-6).
 *
 * <p>예전 구독자는 Redis 로 들어온 이벤트를 그대로 믿었다. Redis 에 발행할 수 있는 누구든 위조
 * {@code USER_DELETED} · {@code SESSION_REVOKED_ALL} 을 보낼 수 있었다. 이제 SSO 서명 키(JWKS 로 공개된 RS256)로
 * 서명된 것만 처리한다. 모양은 {@link SsoEventSignature}.
 *
 * <h3>판정</h3>
 * <ul>
 *   <li>서명 있음 → 서명 · 만료 · {@code typ} · 채널 · {@code jti}(재전송) 를 보고, 통과하면 <b>서명 안의 원문</b>
 *       ({@code evt}) 을 돌려준다. 하나라도 틀리면 비운다.</li>
 *   <li>서명 없음 → {@code requireSignature} 가 꺼져 있으면(전환 기간) 경고를 남기고 받는다. 켜져 있으면 버린다.</li>
 * </ul>
 * 거절은 예외가 아니라 빈 값이다 — 구독자는 처리하지 않고 넘어가면 된다(이유는 여기서 warn 으로 남긴다).
 *
 * <p>빈이 아니다 — 서비스가 기존 {@code SsoJwksKeyLocator} 와 Redis 로 직접 만든다.
 */
@Slf4j
public class SsoEventVerifier {

    /** 서버 시계 차이 허용(초) */
    static final long CLOCK_SKEW_SECONDS = 30;
    private static final String JTI_KEY_PREFIX = "sso:event:jti:";

    private final Locator<Key> keyLocator;
    private final StringRedisTemplate redis;
    private final boolean requireSignature;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    /**
     * @param keyLocator       SSO 공개키 조회(보통 {@code SsoJwksKeyLocator})
     * @param redis            {@code jti} 를 기억할 곳 — null 이면 재전송 검사를 하지 않는다
     * @param requireSignature 서명 없는 이벤트를 버릴지(전환이 끝나면 켠다)
     */
    public SsoEventVerifier(Locator<Key> keyLocator, StringRedisTemplate redis, boolean requireSignature) {
        this.keyLocator = keyLocator;
        this.redis = redis;
        this.requireSignature = requireSignature;
    }

    /**
     * @param channel 이 메시지를 받은 Redis 채널
     * @param message Redis 로 받은 문자열
     * @return 처리해도 되는 이벤트 JSON — 서명이 있으면 서명 안의 원문, 없고 허용이면 받은 그대로. 거절이면 빈 값
     */
    public Optional<String> verify(String channel, String message) {
        String sig;
        try {
            JsonNode tree = objectMapper.readTree(message);
            JsonNode sigNode = tree == null ? null : tree.get(SsoEventSignature.SIG_FIELD);
            sig = sigNode != null && sigNode.isString() ? sigNode.asString() : null;
        } catch (Exception e) {
            log.warn("SSO 이벤트를 읽지 못했다 — 처리하지 않는다. channel={}", channel);
            return Optional.empty();
        }

        if (sig == null || sig.isBlank()) {
            if (requireSignature) {
                log.warn("서명 없는 SSO 이벤트 — 처리하지 않는다. channel={}", channel);
                return Optional.empty();
            }
            log.warn("서명 없는 SSO 이벤트를 받았다(전환 기간이라 처리한다). channel={}", channel);
            return Optional.of(message);
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(keyLocator)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(sig)
                    .getPayload();
        } catch (Exception e) {
            log.warn("SSO 이벤트 서명 검증 실패 — 처리하지 않는다. channel={}, reason={}", channel, e.getMessage());
            return Optional.empty();
        }

        String type = claims.get(SsoEventSignature.CLAIM_TYPE, String.class);
        String signedChannel = claims.get(SsoEventSignature.CLAIM_CHANNEL, String.class);
        String event = claims.get(SsoEventSignature.CLAIM_EVENT, String.class);
        if (!SsoEventSignature.TYPE.equals(type) || !channel.equals(signedChannel)
                || event == null || claims.getId() == null
                || claims.getIssuedAt() == null || claims.getExpiration() == null) {
            log.warn("SSO 이벤트 서명의 내용이 맞지 않는다(typ · 채널 · 필수 값) — 처리하지 않는다. channel={}", channel);
            return Optional.empty();
        }

        if (!firstSeen(claims.getId(), claims.getExpiration())) {
            log.warn("이미 처리한 SSO 이벤트를 다시 받았다(재전송) — 처리하지 않는다. channel={}, jti={}",
                    channel, claims.getId());
            return Optional.empty();
        }
        return Optional.of(event);
    }

    /** jti 를 만료까지 기억한다. 처음 보면 true. Redis 가 없거나 실패하면 막지 않는다(가용성). */
    private boolean firstSeen(String jti, Date expiration) {
        if (redis == null) {
            return true;
        }
        try {
            long ttlMillis = Math.max(expiration.getTime() - System.currentTimeMillis(), 0)
                    + CLOCK_SKEW_SECONDS * 1000;
            Boolean first = redis.opsForValue().setIfAbsent(JTI_KEY_PREFIX + jti, "1", Duration.ofMillis(ttlMillis));
            return !Boolean.FALSE.equals(first);
        } catch (Exception e) {
            log.warn("SSO 이벤트 jti 를 기억하지 못했다 — 재전송 검사 없이 처리한다. reason={}", e.getMessage());
            return true;
        }
    }
}
