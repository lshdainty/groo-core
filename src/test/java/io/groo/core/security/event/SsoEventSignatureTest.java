package io.groo.core.security.event;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.groo.core.security.jwt.SsoJwksKeyLocator;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.groo.core.security.event.SsoEventSignature.SESSION_EVENTS_CHANNEL;
import static io.groo.core.security.event.SsoEventSignature.USER_EVENTS_CHANNEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SSO 이벤트 서명 · 검증(보안 점검 IEU-6) — 실제 RS256 으로 서명하고, 받는 쪽은 JWKS(파일 URL)로 검증한다.
 */
@DisplayName("SSO 이벤트 서명")
class SsoEventSignatureTest {

    private static final String EVENT = "{\"type\":\"USER_DELETED\",\"userNo\":7,\"timestamp\":\"2026-11-12T01:02:03\"}";

    @TempDir
    Path dir;

    private RSAKey ssoKey;
    private SsoEventSigner signer;
    private SsoJwksKeyLocator locator;
    private final Set<String> seen = new HashSet<>();
    private StringRedisTemplate redis;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        ssoKey = new RSAKeyGenerator(2048).keyID("k1").generate();
        Path jwks = dir.resolve("jwks.json");
        Files.writeString(jwks, new JWKSet(ssoKey.toPublicJWK()).toString());
        locator = new SsoJwksKeyLocator(jwks.toUri().toString());
        signer = new SsoEventSigner(ssoKey.toRSAPrivateKey(), "k1");

        redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> seen.add(inv.getArgument(0)));
    }

    private SsoEventVerifier verifier(boolean required) {
        return new SsoEventVerifier(locator, redis, required);
    }

    @Test
    @DisplayName("서명한 이벤트는 검증을 통과하고 원문을 돌려준다 — 바깥 필드는 그대로라 옛 구독자도 읽는다")
    void roundTrip() {
        String signed = signer.sign(USER_EVENTS_CHANNEL, EVENT);

        assertThat(verifier(true).verify(USER_EVENTS_CHANNEL, signed)).contains(EVENT);
        Map<?, ?> outer = JsonMapper.builder().build().readValue(signed, Map.class);
        assertThat(outer.get("type")).isEqualTo("USER_DELETED");
        assertThat(outer.get("userNo")).isEqualTo(7);
        assertThat(outer.get("sig")).isNotNull();
    }

    @Test
    @DisplayName("바깥 필드를 바꿔치기해도 서명 안의 원문을 쓴다")
    void outerTamperIgnored() {
        ObjectNode tampered = (ObjectNode) JsonMapper.builder().build().readTree(signer.sign(USER_EVENTS_CHANNEL, EVENT));
        tampered.put("userNo", 1);

        assertThat(verifier(true).verify(USER_EVENTS_CHANNEL, tampered.toString())).contains(EVENT);
    }

    @Test
    @DisplayName("다른 채널로 다시 쏜 이벤트는 거절")
    void otherChannelRejected() {
        String signed = signer.sign(USER_EVENTS_CHANNEL, EVENT);

        assertThat(verifier(true).verify(SESSION_EVENTS_CHANNEL, signed)).isEmpty();
    }

    @Test
    @DisplayName("SSO 키가 아닌 키로 서명한 이벤트는 거절")
    void foreignKeyRejected() throws Exception {
        RSAKey attacker = new RSAKeyGenerator(2048).keyID("k1").generate();
        String forged = new SsoEventSigner(attacker.toRSAPrivateKey(), "k1").sign(USER_EVENTS_CHANNEL, EVENT);

        assertThat(verifier(true).verify(USER_EVENTS_CHANNEL, forged)).isEmpty();
    }

    @Test
    @DisplayName("SSO 키로 서명했어도 이벤트 서명이 아니면(typ 없음 — 예: access 토큰) 거절")
    void notAnEventTokenRejected() throws Exception {
        String accessLike = Jwts.builder().header().keyId("k1").and()
                .subject("hong").claim("chn", USER_EVENTS_CHANNEL).claim("evt", EVENT).id("j1")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(ssoKey.toRSAPrivateKey(), Jwts.SIG.RS256).compact();
        String message = EVENT.replace("}", ",\"sig\":\"" + accessLike + "\"}");

        assertThat(verifier(true).verify(USER_EVENTS_CHANNEL, message)).isEmpty();
    }

    @Test
    @DisplayName("만료된 서명은 거절")
    void expiredRejected() throws Exception {
        String old = new SsoEventSigner(ssoKey.toRSAPrivateKey(), "k1", Duration.ofMinutes(-2))
                .sign(USER_EVENTS_CHANNEL, EVENT);

        assertThat(verifier(true).verify(USER_EVENTS_CHANNEL, old)).isEmpty();
    }

    @Test
    @DisplayName("같은 서명을 다시 보내면(재전송) 두 번째는 거절")
    void replayRejected() {
        String signed = signer.sign(USER_EVENTS_CHANNEL, EVENT);
        SsoEventVerifier v = verifier(true);

        assertThat(v.verify(USER_EVENTS_CHANNEL, signed)).isPresent();
        assertThat(v.verify(USER_EVENTS_CHANNEL, signed)).isEmpty();
    }

    @Test
    @DisplayName("서명 없는 이벤트 — 전환 기간엔 받고, 강제하면 버린다")
    void unsigned() {
        assertThat(verifier(false).verify(USER_EVENTS_CHANNEL, EVENT)).contains(EVENT);
        assertThat(verifier(true).verify(USER_EVENTS_CHANNEL, EVENT)).isEmpty();
    }

    @Test
    @DisplayName("서명이 깨졌으면 전환 기간이어도 버린다 — 서명 없음과 다르다")
    void brokenSignatureRejectedEvenInTransition() {
        String message = EVENT.replace("}", ",\"sig\":\"not.a.jws\"}");

        assertThat(verifier(false).verify(USER_EVENTS_CHANNEL, message)).isEmpty();
    }

    @Test
    @DisplayName("JSON 이 아니면 버린다")
    void garbageRejected() {
        assertThat(verifier(false).verify(USER_EVENTS_CHANNEL, "not json")).isEmpty();
    }

    @Test
    @DisplayName("Redis 없이 만들면 재전송 검사는 건너뛴다")
    void noRedisNoReplayCheck() {
        String signed = signer.sign(USER_EVENTS_CHANNEL, EVENT);
        SsoEventVerifier v = new SsoEventVerifier(locator, null, true);

        assertThat(v.verify(USER_EVENTS_CHANNEL, signed)).isPresent();
        assertThat(v.verify(USER_EVENTS_CHANNEL, signed)).isPresent();
    }
}
