package io.groo.core.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.KeyScanOptions;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import org.mockito.invocation.Invocation;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 서비스별 Redis 키 접두사(보안 점검 INF-2 · SSO-13) — 키에만 붙고, 한 자리에서 빠짐없이 붙는지 본다.
 */
@DisplayName("Redis 키 접두사 템플릿")
class NamespacedStringRedisTemplateTest {

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("키를 쓸 때 접두사를 붙이고, 읽을 때 뗀다")
    void serializerAddsAndStripsPrefix() {
        NamespacedKeySerializer serializer = new NamespacedKeySerializer("damda");

        assertThat(serializer.prefix()).isEqualTo("damda:");
        assertThat(serializer.serialize("session:revoked:abc")).isEqualTo(bytes("damda:session:revoked:abc"));
        assertThat(serializer.deserialize(bytes("damda:session:revoked:abc"))).isEqualTo("session:revoked:abc");
    }

    @Test
    @DisplayName("접두사가 없는 키(다른 서비스의 키)는 읽을 때 그대로 돌려준다")
    void foreignKeyIsReturnedAsIs() {
        NamespacedKeySerializer serializer = new NamespacedKeySerializer("damda");

        assertThat(serializer.deserialize(bytes("ieum:revoked-jti:abc"))).isEqualTo("ieum:revoked-jti:abc");
        // 이름이 접두사로 시작만 하고 구분자가 없는 것은 자기 키가 아니다
        assertThat(serializer.deserialize(bytes("damdax:abc"))).isEqualTo("damdax:abc");
    }

    @Test
    @DisplayName("null 은 null 로 둔다")
    void nullStaysNull() {
        NamespacedKeySerializer serializer = new NamespacedKeySerializer("damda");

        assertThat(serializer.serialize(null)).isNull();
        assertThat(serializer.deserialize(null)).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "Damda", "damda:", "da*", "da mda", "1damda", "damda_x", "a?b", "a[b]"})
    @DisplayName("접두사에 구분자 · 패턴 문자 · 대문자 · 빈 값은 받지 않는다")
    void rejectsUnsafeNamespace(String namespace) {
        assertThatThrownBy(() -> new NamespacedKeySerializer(namespace))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("템플릿은 키 직렬화기만 바꾼다 — 값 · 해시 · 채널 이름은 그대로")
    void templateChangesOnlyKeySerializer() {
        NamespacedStringRedisTemplate template =
                new NamespacedStringRedisTemplate(mock(RedisConnectionFactory.class), "ieum");

        assertThat(template).isInstanceOf(StringRedisTemplate.class);
        assertThat(template.keyPrefix()).isEqualTo("ieum:");
        assertThat(template.getKeySerializer()).isInstanceOf(NamespacedKeySerializer.class);
        assertThat(template.getValueSerializer()).isSameAs(RedisSerializer.string());
        assertThat(template.getHashKeySerializer()).isSameAs(RedisSerializer.string());
        assertThat(template.getHashValueSerializer()).isSameAs(RedisSerializer.string());
        assertThat(template.getStringSerializer()).isSameAs(RedisSerializer.string());
    }

    @Test
    @DisplayName("값 쓰기 · 삭제가 Redis 로 나갈 때 키에 접두사가 붙어 있다")
    void commandsCarryPrefixedKey() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class, RETURNS_DEEP_STUBS);
        when(factory.getConnection()).thenReturn(connection);
        NamespacedStringRedisTemplate template = new NamespacedStringRedisTemplate(factory, "ieum");

        template.opsForValue().set("revoked-jti:abc", "1", Duration.ofSeconds(30));
        template.delete("revoked-user:7");

        // 어느 명령 인터페이스로 나가든(버전마다 다르다) Redis 로 가는 키는 접두사가 붙은 것뿐이어야 한다
        assertThat(keysSentTo(connection))
                .contains("ieum:revoked-jti:abc", "ieum:revoked-user:7")
                .doesNotContain("revoked-jti:abc", "revoked-user:7");
    }

    /** 연결(과 그 하위 명령 객체)로 넘어간 바이트 인자를 문자열로 모은다. */
    private static List<String> keysSentTo(RedisConnection connection) {
        List<String> sent = new ArrayList<>();
        for (Object mock : List.of(connection, connection.stringCommands(), connection.keyCommands())) {
            for (Invocation invocation : mockingDetails(mock).getInvocations()) {
                for (Object argument : invocation.getArguments()) {
                    if (argument instanceof byte[] one) {
                        sent.add(new String(one, StandardCharsets.UTF_8));
                    } else if (argument instanceof byte[][] many) {
                        Arrays.stream(many).map(b -> new String(b, StandardCharsets.UTF_8)).forEach(sent::add);
                    }
                }
            }
        }
        return sent;
    }

    @Test
    @DisplayName("이벤트 발행의 채널 이름에는 접두사가 붙지 않는다")
    void channelNameIsNotPrefixed() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class, RETURNS_DEEP_STUBS);
        when(factory.getConnection()).thenReturn(connection);
        NamespacedStringRedisTemplate template = new NamespacedStringRedisTemplate(factory, "sso");

        template.convertAndSend("porest:sso:session-events", "{}");

        verify(connection).publish(eq(bytes("porest:sso:session-events")), any());
    }

    @Test
    @DisplayName("SCAN 은 찾는 패턴에 접두사를 붙이고 개수 · 종류 조건은 그대로 둔다")
    void scanPatternIsNamespaced() {
        ScanOptions given = ScanOptions.scanOptions().match("login:pair:*").count(200).type(DataType.STRING).build();

        ScanOptions actual = NamespacedStringRedisTemplate.namespaced(given, "sso:");

        assertThat(actual.getPattern()).isEqualTo("sso:login:pair:*");
        assertThat(actual.getCount()).isEqualTo(200L);
        assertThat(actual).isInstanceOf(KeyScanOptions.class);
        assertThat(((KeyScanOptions) actual).getType()).isEqualTo(DataType.STRING.code());
    }

    @Test
    @DisplayName("패턴 없는 SCAN 은 자기 접두사 전체로 좁힌다")
    void scanWithoutPatternIsLimitedToNamespace() {
        ScanOptions actual = NamespacedStringRedisTemplate.namespaced(ScanOptions.NONE, "sso:");

        assertThat(actual.getPattern()).isEqualTo("sso:*");
        assertThat(actual.getCount()).isNull();
        assertThat(actual).isNotInstanceOf(KeyScanOptions.class);
    }
}
