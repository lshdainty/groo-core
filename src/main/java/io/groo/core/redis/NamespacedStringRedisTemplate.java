package io.groo.core.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.KeyScanOptions;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 모든 키에 서비스 접두사를 붙이는 {@link StringRedisTemplate}(core 3.5.0, 보안 점검 INF-2 · SSO-13).
 *
 * <p>서비스는 이 템플릿을 {@code stringRedisTemplate} 빈으로 등록하고, 키 이름에는 서비스 접두사를 쓰지 않는다.
 * 그러면 그 서비스가 쓰는 키는 전부 {@code <이름>:} 아래에 놓이고, Redis 계정을 {@code ~<이름>:*} 로 묶어도
 * 권한 밖 키가 생기지 않는다. core 의 {@code RedisRateLimitInterceptor} · {@code SsoEventVerifier} 처럼 템플릿을
 * 받아 쓰는 공통 코드의 키도 같이 그 아래로 들어간다.
 *
 * <pre>{@code
 * @Bean
 * public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
 *     return new NamespacedStringRedisTemplate(connectionFactory, "damda");
 * }
 * }</pre>
 *
 * <p>접두사가 붙는 것은 키뿐이다. 채널 이름({@code convertAndSend}), 값, 해시 필드는 그대로다.
 * {@link #scan(ScanOptions)} 은 찾는 패턴에도 접두사를 붙여 자기 키 안에서만 찾는다.
 *
 * <p>연결을 직접 꺼내 바이트 키로 명령을 보내는 코드({@code execute(RedisCallback)})에는 접두사가 붙지 않는다 —
 * 그런 자리에서는 {@link NamespacedKeySerializer#prefix()} 를 직접 붙인다.
 */
public class NamespacedStringRedisTemplate extends StringRedisTemplate {

    private final NamespacedKeySerializer keySerializer;

    /**
     * @param connectionFactory Redis 연결
     * @param namespace         서비스 이름(예 {@code damda}) — Redis 에는 {@code damda:} 로 붙는다
     */
    public NamespacedStringRedisTemplate(RedisConnectionFactory connectionFactory, String namespace) {
        this.keySerializer = new NamespacedKeySerializer(namespace);
        setKeySerializer(keySerializer);
        setConnectionFactory(connectionFactory);
        afterPropertiesSet();
    }

    /** Redis 에 실제로 붙는 접두사(구분자 포함, 예 {@code damda:}) */
    public String keyPrefix() {
        return keySerializer.prefix();
    }

    /** 찾는 패턴에 접두사를 붙여 자기 키 안에서만 찾는다. 돌려주는 이름은 접두사를 뗀 것이다. */
    @Override
    public Cursor<String> scan(ScanOptions options) {
        return super.scan(namespaced(options, keySerializer.prefix()));
    }

    static ScanOptions namespaced(ScanOptions options, String prefix) {
        String pattern = options.getPattern();
        ScanOptions.ScanOptionsBuilder builder = ScanOptions.scanOptions()
                .match(prefix + (pattern == null ? "*" : pattern));
        if (options.getCount() != null) {
            builder.count(options.getCount());
        }
        if (options instanceof KeyScanOptions keyOptions && keyOptions.getType() != null) {
            builder.type(keyOptions.getType());
        }
        return builder.build();
    }
}
