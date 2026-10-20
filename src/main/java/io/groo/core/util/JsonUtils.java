package io.groo.core.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 변환 유틸리티
 * <p>
 * Jackson ObjectMapper를 래핑하여 정적 메서드로 JSON 변환 기능을 제공합니다.
 * DI 없이 유틸리티 클래스나 static 컨텍스트에서 JSON 변환이 필요할 때 사용합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>객체 → JSON 문자열 변환</li>
 *   <li>JSON 문자열 → 객체 변환</li>
 *   <li>Pretty Print 지원</li>
 *   <li>Java 8 날짜/시간 타입 지원</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 객체를 JSON으로 변환
 * UserDto user = new UserDto(1L, "홍길동", "hong@example.com");
 * String json = JsonUtils.toJson(user);
 * // 결과: {"id":1,"name":"홍길동","email":"hong@example.com"}
 *
 * // JSON을 객체로 변환
 * String json = "{\"id\":1,\"name\":\"홍길동\"}";
 * UserDto user = JsonUtils.fromJson(json, UserDto.class);
 *
 * // 제네릭 타입 변환 (List, Map 등)
 * String jsonArray = "[{\"id\":1},{\"id\":2}]";
 * List<UserDto> users = JsonUtils.fromJson(jsonArray, new TypeReference<List<UserDto>>() {});
 *
 * // 로깅 시 사용
 * log.debug("Request body: {}", JsonUtils.toJson(request));
 *
 * // Pretty Print (디버깅용)
 * log.debug("Response:\n{}", JsonUtils.toPrettyJson(response));
 * }</pre>
 *
 * <h3>참고사항</h3>
 * <ul>
 *   <li>Spring 컨텍스트에서는 주입받은 ObjectMapper 사용을 권장합니다.</li>
 *   <li>변환 실패 시 null을 반환하고 WARN 로그를 남깁니다.</li>
 *   <li>Java 8 날짜/시간 타입(LocalDate, LocalDateTime 등)을 지원합니다.</li>
 * </ul>
 *
 * @author groo
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER;
    private static final ObjectMapper PRETTY_OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        PRETTY_OBJECT_MAPPER = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private JsonUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 객체를 JSON 문자열로 변환
     * <p>
     * 변환 실패 시 null을 반환하고 경고 로그를 남깁니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * UserDto user = new UserDto(1L, "홍길동");
     * String json = JsonUtils.toJson(user);
     * // 결과: {"id":1,"name":"홍길동"}
     * }</pre>
     *
     * @param object 변환할 객체
     * @return JSON 문자열, 실패 시 null
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JacksonException e) {
            log.warn("Failed to convert object to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 객체를 포맷팅된 JSON 문자열로 변환
     * <p>
     * 들여쓰기가 적용된 읽기 쉬운 JSON을 생성합니다.
     * 디버깅이나 로깅 목적으로 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * UserDto user = new UserDto(1L, "홍길동");
     * String json = JsonUtils.toPrettyJson(user);
     * // 결과:
     * // {
     * //   "id" : 1,
     * //   "name" : "홍길동"
     * // }
     * }</pre>
     *
     * @param object 변환할 객체
     * @return 포맷팅된 JSON 문자열, 실패 시 null
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return PRETTY_OBJECT_MAPPER.writeValueAsString(object);
        } catch (JacksonException e) {
            log.warn("Failed to convert object to pretty JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 문자열을 객체로 변환
     * <p>
     * 변환 실패 시 null을 반환하고 경고 로그를 남깁니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String json = "{\"id\":1,\"name\":\"홍길동\"}";
     * UserDto user = JsonUtils.fromJson(json, UserDto.class);
     * }</pre>
     *
     * @param json  JSON 문자열
     * @param clazz 변환할 클래스 타입
     * @param <T>   반환 타입
     * @return 변환된 객체, 실패 시 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JacksonException e) {
            log.warn("Failed to convert JSON to object: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 문자열을 제네릭 타입 객체로 변환
     * <p>
     * List, Map 등 제네릭 타입의 객체로 변환할 때 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // List 변환
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<UserDto> users = JsonUtils.fromJson(jsonArray, new TypeReference<List<UserDto>>() {});
     *
     * // Map 변환
     * String jsonMap = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
     * Map<String, String> map = JsonUtils.fromJson(jsonMap, new TypeReference<Map<String, String>>() {});
     * }</pre>
     *
     * @param json          JSON 문자열
     * @param typeReference 변환할 타입 레퍼런스
     * @param <T>           반환 타입
     * @return 변환된 객체, 실패 시 null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JacksonException e) {
            log.warn("Failed to convert JSON to object: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 객체를 다른 타입으로 변환
     * <p>
     * JSON을 중간 단계로 사용하여 객체를 다른 타입으로 변환합니다.
     * DTO 간 변환이나 Map → 객체 변환에 유용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // Map을 DTO로 변환
     * Map<String, Object> map = Map.of("id", 1, "name", "홍길동");
     * UserDto user = JsonUtils.convert(map, UserDto.class);
     *
     * // DTO 간 변환
     * UserCreateRequest request = new UserCreateRequest("홍길동", "hong@example.com");
     * UserDto dto = JsonUtils.convert(request, UserDto.class);
     * }</pre>
     *
     * @param source    원본 객체
     * @param targetType 변환할 대상 클래스 타입
     * @param <T>       반환 타입
     * @return 변환된 객체, 실패 시 null
     */
    public static <T> T convert(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.convertValue(source, targetType);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to convert object: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 문자열 유효성 검사
     * <p>
     * 주어진 문자열이 유효한 JSON 형식인지 확인합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * boolean valid = JsonUtils.isValidJson("{\"key\":\"value\"}");  // true
     * boolean invalid = JsonUtils.isValidJson("{invalid}");          // false
     * }</pre>
     *
     * @param json 검사할 JSON 문자열
     * @return 유효한 JSON이면 true
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }

        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }
}
