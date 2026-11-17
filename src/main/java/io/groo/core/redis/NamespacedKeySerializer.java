package io.groo.core.redis;

import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 서비스의 Redis 키를 한 접두사({@code <이름>:}) 아래로 모으는 키 직렬화기(core 3.5.0, 보안 점검 INF-2 · SSO-13).
 *
 * <p>여러 서비스가 Redis 한 대를 같이 쓴다. Redis 의 계정 권한(ACL)은 DB 번호가 아니라 <b>키 이름 패턴</b>으로만
 * 나눌 수 있어서, 서비스 계정을 {@code ~damda:*} 처럼 묶으려면 그 서비스가 쓰는 키가 빠짐없이 그 접두사로
 * 시작해야 한다. 키를 만드는 자리마다 접두사를 손으로 붙이면 하나만 빠뜨려도 권한 밖 키가 되므로, 직렬화 단계에서
 * 한 번에 붙인다 — 코드는 접두사 없는 이름({@code revoked-jti:abc})을 쓰고 Redis 에는
 * {@code ieum:revoked-jti:abc} 로 들어간다.
 *
 * <p>읽을 때(SCAN · KEYS 결과)는 접두사를 떼어 코드가 쓰는 이름으로 돌려준다. 다른 서비스의 키처럼 접두사가 없는
 * 값은 그대로 돌려준다.
 *
 * <p>채널 이름(pub/sub)과 해시 필드에는 붙지 않는다 — 키에만 쓰인다.
 */
public final class NamespacedKeySerializer implements RedisSerializer<String> {

    /** 소문자로 시작하는 소문자 · 숫자 · 하이픈. 패턴 문자({@code * ? [ ]})와 구분자({@code :})는 받지 않는다. */
    private static final Pattern NAMESPACE = Pattern.compile("[a-z][a-z0-9-]{0,31}");

    private final String prefix;

    /**
     * @param namespace 서비스 이름(예 {@code damda}) — 구분자 {@code :} 는 여기서 붙인다
     */
    public NamespacedKeySerializer(String namespace) {
        if (namespace == null || !NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException(
                    "Redis 키 접두사는 소문자로 시작하는 소문자 · 숫자 · 하이픈 32자 이내여야 한다: " + namespace);
        }
        this.prefix = namespace + ":";
    }

    /** Redis 에 실제로 붙는 접두사(구분자 포함, 예 {@code damda:}) */
    public String prefix() {
        return prefix;
    }

    @Override
    public byte[] serialize(String key) {
        if (key == null) {
            return null;
        }
        return (prefix + key).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        String raw = new String(bytes, StandardCharsets.UTF_8);
        return raw.startsWith(prefix) ? raw.substring(prefix.length()) : raw;
    }
}
