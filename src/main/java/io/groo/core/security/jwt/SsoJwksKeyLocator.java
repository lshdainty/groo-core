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
 *   <li>JWKS 는 nimbus {@link JWKSourceBuilder} 기본값으로 캐시·재시도한다(모르는 kid 를 만나면 다시 받아 온다).
 *       처음 쓸 때 만든다 — SSO 가 늦게 떠도 서비스 기동이 막히지 않게.</li>
 * </ul>
 *
 * <p>서비스는 이걸 {@code @Bean} 으로 만들어 jjwt 파서의 {@code keyLocator(...)} 에 넘긴다.
 */
public class SsoJwksKeyLocator extends LocatorAdapter<Key> {

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
            throw e;
        } catch (Exception e) {
            throw new JwtException("Failed to resolve SSO signing key from JWKS", e);
        }
    }

    private JWKSource<SecurityContext> source() throws Exception {
        JWKSource<SecurityContext> s = this.jwkSource;
        if (s == null) {
            synchronized (this) {
                s = this.jwkSource;
                if (s == null) {
                    s = JWKSourceBuilder.create(URI.create(jwksUri).toURL()).build();
                    this.jwkSource = s;
                }
            }
        }
        return s;
    }
}
