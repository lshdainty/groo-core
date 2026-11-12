package io.groo.core.security.jwt;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.LocatorAdapter;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.security.Key;
import java.util.List;

/**
 * SSO 가 서명한 토큰의 공개키를 SSO JWKS 에서 찾는다 — damda·이음이 같은 코드를 따로 갖던 것을 올렸다(core 3.2.0).
 *
 * <ul>
 *   <li>JWKS 주소는 <b>설정값</b>({@code jwt.sso-jwks-uri})으로만 받는다. 토큰 헤더의 {@code jku}·{@code x5u} 는
 *       보지 않는다 — 그걸 따라가면 공격자가 자기 키로 서명한 토큰을 자기 서버의 키로 검증시키게 된다.</li>
 *   <li>RSA 키만 쓴다. 대칭키(HMAC)로 바꿔치기한 토큰은 여기서 키를 못 찾고 거절된다.</li>
 *   <li>{@code kid} 가 있으면 그 키만, 없으면 RSA 키 중 첫 번째. 키 회전 중이면 SSO 가 kid 를 붙이므로 맞는 키를 고른다.</li>
 *   <li>JWKS 조회는 시간 제한 · 재시도 · 장애 허용을 <b>명시</b>한다(보안 점검 IEU-6 후속). nimbus 기본값(캐시 5분 ·
 *       500ms · 재시도 없음)이면 SSO 가 잠깐 느리거나 내려간 사이 키를 못 찾아 SSO 이벤트가 그대로 버려졌다
 *       (pub/sub 은 다시 보내지 않는다). 연결 · 읽기 각 {@value #TIMEOUT_MS}ms, 실패하면 한 번 더, 캐시
 *       {@value #CACHE_TTL_MS}ms, 조회가 실패하면 마지막으로 받은 키를 {@value #OUTAGE_TOLERANCE_MS}ms 동안 계속 쓴다.
 *       모르는 kid 를 만나면 다시 받아 온다. 그래도 못 찾으면 경고 로그를 남기고 거절한다(닫힘).</li>
 *   <li>처음 쓸 때 만든다 — SSO 가 늦게 떠도 서비스 기동이 막히지 않게.</li>
 * </ul>
 *
 * <p>서비스는 이걸 {@code @Bean} 으로 만들어 jjwt 파서의 {@code keyLocator(...)} 에 넘긴다.
 */
@Slf4j
public class SsoJwksKeyLocator extends LocatorAdapter<Key> {

    /** 연결 · 읽기 시간 제한(각각) */
    static final int TIMEOUT_MS = 2_000;
    /** JWKS 문서 크기 상한 */
    static final int SIZE_LIMIT_BYTES = 64 * 1024;
    /** 받아 둔 JWKS 를 다시 받기 전까지 쓰는 시간 */
    static final long CACHE_TTL_MS = 15 * 60 * 1000L;
    /** 캐시를 새로 받을 때 기다리는 최대 시간 */
    static final long CACHE_REFRESH_TIMEOUT_MS = 5_000L;
    /** 조회가 실패할 때 마지막으로 받은 키를 계속 쓰는 시간 — SSO 장애 동안 이벤트 · 토큰 검증이 멈추지 않게 */
    static final long OUTAGE_TOLERANCE_MS = 24 * 60 * 60 * 1000L;

    private final String jwksUri;
    private volatile JWKSource<SecurityContext> jwkSource;

    public SsoJwksKeyLocator(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @Override
    protected Key locate(JwsHeader header) {
        String kid = header.getKeyId();
        try {
            JWKMatcher.Builder matcher = new JWKMatcher.Builder().keyType(KeyType.RSA);
            if (kid != null) {
                matcher.keyID(kid);
            }
            List<JWK> jwks = source().get(new JWKSelector(matcher.build()), null);
            if (jwks.isEmpty()) {
                throw new JwtException("No matching SSO JWK (kid=" + kid + ")");
            }
            return jwks.get(0).toRSAKey().toRSAPublicKey();
        } catch (JwtException e) {
            log.warn("SSO 공개키를 찾지 못했다(kid={}) — 거절한다: {}", kid, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("SSO JWKS 조회 실패(kid={}) — 거절한다: {}", kid, e.getMessage());
            throw new JwtException("Failed to resolve SSO signing key from JWKS", e);
        }
    }

    private JWKSource<SecurityContext> source() throws Exception {
        JWKSource<SecurityContext> s = this.jwkSource;
        if (s == null) {
            synchronized (this) {
                s = this.jwkSource;
                if (s == null) {
                    s = JWKSourceBuilder.<SecurityContext>create(URI.create(jwksUri).toURL(),
                                    new DefaultResourceRetriever(TIMEOUT_MS, TIMEOUT_MS, SIZE_LIMIT_BYTES))
                            .cache(CACHE_TTL_MS, CACHE_REFRESH_TIMEOUT_MS)
                            .retrying(true)
                            .outageTolerant(OUTAGE_TOLERANCE_MS)
                            .build();
                    this.jwkSource = s;
                }
            }
        }
        return s;
    }
}
