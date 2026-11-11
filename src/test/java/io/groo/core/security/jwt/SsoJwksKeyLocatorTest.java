package io.groo.core.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SSO 공개키 조회 — 실제로 서명·검증해 본다. JWKS 는 파일 URL 로 흉내 낸다. */
class SsoJwksKeyLocatorTest {

    @TempDir
    Path dir;

    private RSAKey ssoKey;
    private SsoJwksKeyLocator locator;

    @BeforeEach
    void setUp() throws Exception {
        ssoKey = new RSAKeyGenerator(2048).keyID("k1").generate();
        Path jwks = dir.resolve("jwks.json");
        Files.writeString(jwks, new JWKSet(ssoKey.toPublicJWK()).toString());
        locator = new SsoJwksKeyLocator(jwks.toUri().toString());
    }

    private String parse(String token) {
        return Jwts.parser().keyLocator(locator).build().parseSignedClaims(token).getPayload().getSubject();
    }

    @Test
    void SSO_키로_서명한_토큰은_통과() throws Exception {
        String token = Jwts.builder().header().keyId("k1").and().subject("u1")
                .signWith(ssoKey.toPrivateKey(), Jwts.SIG.RS256).compact();

        assertThat(parse(token)).isEqualTo("u1");
    }

    @Test
    void 같은_kid_라도_다른_키로_서명하면_거절() throws Exception {
        RSAKey attacker = new RSAKeyGenerator(2048).keyID("k1").generate();
        String forged = Jwts.builder().header().keyId("k1").and().subject("admin")
                .signWith(attacker.toPrivateKey(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void 모르는_kid_는_거절() throws Exception {
        RSAKey other = new RSAKeyGenerator(2048).keyID("unknown").generate();
        String token = Jwts.builder().header().keyId("unknown").and().subject("u1")
                .signWith(other.toPrivateKey(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void 대칭키_HMAC_으로_바꿔치기한_토큰은_거절() {
        String hmac = Jwts.builder().header().keyId("k1").and().subject("admin")
                .signWith(Keys.hmacShaKeyFor(new byte[32]), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> parse(hmac)).isInstanceOf(JwtException.class);
    }
}
