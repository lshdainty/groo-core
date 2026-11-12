package io.groo.core.security.event;

import io.jsonwebtoken.Jwts;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * SSO 이벤트에 서명을 덧붙인다 — groo-sso-back 이 Redis 로 내보내기 직전에 쓴다(core 3.4.0, 보안 점검 IEU-6).
 *
 * <p>모양은 {@link SsoEventSignature}. 빈이 아니다 — 서명 키를 가진 서비스(SSO)만 직접 만든다.
 */
public class SsoEventSigner {

    /** 기본 수명 — 구독은 실시간이라 몇 초면 닿는다. 서버 시계 차이를 넉넉히 덮는 값. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final PrivateKey signingKey;
    private final String keyId;
    private final Duration ttl;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    /**
     * @param signingKey SSO JWT 와 같은 RS256 개인키
     * @param keyId      JWKS 에 공개된 키의 kid — 받는 쪽이 이것으로 공개키를 고른다
     * @param ttl        서명 수명({@code exp − iat})
     */
    public SsoEventSigner(PrivateKey signingKey, String keyId, Duration ttl) {
        this.signingKey = signingKey;
        this.keyId = keyId;
        this.ttl = ttl;
    }

    public SsoEventSigner(PrivateKey signingKey, String keyId) {
        this(signingKey, keyId, DEFAULT_TTL);
    }

    /**
     * 이벤트 JSON(객체) 에 {@value SsoEventSignature#SIG_FIELD} 를 덧붙여 돌려준다.
     *
     * @param channel   발행할 Redis 채널
     * @param eventJson 이벤트 JSON 객체 문자열
     * @return 원래 필드 + sig 가 든 JSON 문자열
     * @throws IllegalArgumentException 이벤트가 JSON 객체가 아닐 때
     */
    public String sign(String channel, String eventJson) {
        JsonNode tree = objectMapper.readTree(eventJson);
        if (!(tree instanceof ObjectNode node)) {
            throw new IllegalArgumentException("SSO event must be a JSON object");
        }
        Instant now = Instant.now();
        String jws = Jwts.builder()
                .header().keyId(keyId).and()
                .claim(SsoEventSignature.CLAIM_TYPE, SsoEventSignature.TYPE)
                .claim(SsoEventSignature.CLAIM_CHANNEL, channel)
                .claim(SsoEventSignature.CLAIM_EVENT, eventJson)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey, Jwts.SIG.RS256)
                .compact();
        node.put(SsoEventSignature.SIG_FIELD, jws);
        return objectMapper.writeValueAsString(node);
    }
}
