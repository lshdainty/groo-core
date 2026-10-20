package io.groo.core.type;

/**
 * Y/N 타입 Enum
 * <p>
 * 데이터베이스의 Y/N 컬럼이나 boolean 값을 표현하는 Enum입니다.
 * JPA Entity에서 Y/N 문자열을 타입 안전하게 처리할 수 있습니다.
 *
 * <h3>사용 시나리오</h3>
 * <table border="1">
 *   <tr><th>값</th><th>의미</th><th>DB 값</th><th>boolean</th></tr>
 *   <tr><td>Y</td><td>예, 참, 활성화</td><td>"Y"</td><td>true</td></tr>
 *   <tr><td>N</td><td>아니오, 거짓, 비활성화</td><td>"N"</td><td>false</td></tr>
 * </table>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // Entity에서 사용
 * @Entity
 * public class User {
 *     @Enumerated(EnumType.STRING)
 *     @Column(length = 1)
 *     private YNType isDeleted = YNType.N;
 *
 *     @Enumerated(EnumType.STRING)
 *     @Column(length = 1)
 *     private YNType isActive = YNType.Y;
 * }
 *
 * // 조건 검사
 * if (user.getIsDeleted().isY()) {
 *     throw new EntityNotFoundException("삭제된 사용자");
 * }
 *
 * // null 안전한 검사 (static 메서드)
 * if (YNType.isY(user.getIsDeleted())) {
 *     // 삭제된 사용자 처리
 * }
 *
 * // boolean 변환
 * boolean isActive = user.getIsActive().toBoolean();  // true
 *
 * // 반대 값
 * YNType opposite = YNType.Y.opposite();  // N
 *
 * // 문자열에서 변환
 * YNType type = YNType.from("Y");     // Y
 * YNType type = YNType.from("yes");   // Y
 * YNType type = YNType.from("true");  // Y
 * YNType type = YNType.from("1");     // Y
 * YNType type = YNType.from(true);    // Y
 * }</pre>
 *
 * @author groo
 */
public enum YNType {

    /**
     * 예, 참, 활성화
     * <p>
     * boolean true에 해당하며, 데이터베이스에는 "Y"로 저장됩니다.
     */
    Y("Y", "예", true),

    /**
     * 아니오, 거짓, 비활성화
     * <p>
     * boolean false에 해당하며, 데이터베이스에는 "N"으로 저장됩니다.
     */
    N("N", "아니오", false);

    private final String value;
    private final String description;
    private final boolean booleanValue;

    YNType(String value, String description, boolean booleanValue) {
        this.value = value;
        this.description = description;
        this.booleanValue = booleanValue;
    }

    // ========================================
    // Getter 메서드
    // ========================================

    /**
     * 데이터베이스 저장 값 반환
     *
     * @return "Y" 또는 "N"
     */
    public String getValue() {
        return value;
    }

    /**
     * 한글 설명 반환
     *
     * @return "예" 또는 "아니오"
     */
    public String getDescription() {
        return description;
    }

    /**
     * boolean 값 반환
     *
     * @return Y이면 true, N이면 false
     */
    public boolean toBoolean() {
        return booleanValue;
    }

    // ========================================
    // 인스턴스 판별 메서드
    // ========================================

    /**
     * Y 여부 확인
     *
     * @return Y이면 true
     */
    public boolean isY() {
        return this == Y;
    }

    /**
     * N 여부 확인
     *
     * @return N이면 true
     */
    public boolean isN() {
        return this == N;
    }

    /**
     * true 여부 확인 (isY의 별칭)
     *
     * @return Y이면 true
     */
    public boolean isTrue() {
        return this == Y;
    }

    /**
     * false 여부 확인 (isN의 별칭)
     *
     * @return N이면 true
     */
    public boolean isFalse() {
        return this == N;
    }

    // ========================================
    // 변환 메서드
    // ========================================

    /**
     * 반대 값 반환
     * <p>
     * Y ↔ N 변환에 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * YNType current = YNType.Y;
     * YNType opposite = current.opposite();  // N
     *
     * // 토글 기능 구현
     * user.setIsActive(user.getIsActive().opposite());
     * }</pre>
     *
     * @return 반대 값 (Y → N, N → Y)
     */
    public YNType opposite() {
        return this == Y ? N : Y;
    }

    /**
     * NOT 연산 (opposite의 별칭)
     *
     * @return 반대 값 (Y → N, N → Y)
     */
    public YNType not() {
        return opposite();
    }

    // ========================================
    // 정적 판별 메서드 (null 안전)
    // ========================================

    /**
     * Y 여부 확인 (null 안전)
     * <p>
     * null이 전달되면 false를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // null 안전한 검사
     * if (YNType.isY(user.getIsDeleted())) {
     *     throw new EntityNotFoundException("삭제된 사용자");
     * }
     * }</pre>
     *
     * @param type 검사할 YNType (null 허용)
     * @return Y이면 true, null이거나 N이면 false
     */
    public static boolean isY(YNType type) {
        return type != null && type == Y;
    }

    /**
     * N 여부 확인 (null 안전)
     * <p>
     * null이 전달되면 false를 반환합니다.
     *
     * @param type 검사할 YNType (null 허용)
     * @return N이면 true, null이거나 Y이면 false
     */
    public static boolean isN(YNType type) {
        return type != null && type == N;
    }

    /**
     * Y가 아닌지 확인 (null 또는 N)
     * <p>
     * null이거나 N이면 true를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 삭제되지 않은 사용자만 조회
     * if (YNType.isNotY(user.getIsDeleted())) {
     *     // 활성 사용자 처리
     * }
     * }</pre>
     *
     * @param type 검사할 YNType (null 허용)
     * @return null이거나 N이면 true
     */
    public static boolean isNotY(YNType type) {
        return type == null || type == N;
    }

    /**
     * N이 아닌지 확인 (null 또는 Y)
     *
     * @param type 검사할 YNType (null 허용)
     * @return null이거나 Y이면 true
     */
    public static boolean isNotN(YNType type) {
        return type == null || type == Y;
    }

    /**
     * true 여부 확인 (isY의 별칭, null 안전)
     *
     * @param type 검사할 YNType (null 허용)
     * @return Y이면 true
     */
    public static boolean isTrue(YNType type) {
        return isY(type);
    }

    /**
     * false 여부 확인 (isN의 별칭, null 안전)
     *
     * @param type 검사할 YNType (null 허용)
     * @return N이면 true
     */
    public static boolean isFalse(YNType type) {
        return isN(type);
    }

    // ========================================
    // 정적 변환 메서드
    // ========================================

    /**
     * 문자열로부터 YNType 변환
     * <p>
     * 다양한 형식의 문자열을 지원합니다.
     *
     * <h4>Y로 변환되는 값</h4>
     * <ul>
     *   <li>"Y", "y" (대소문자 무관)</li>
     *   <li>"YES", "yes"</li>
     *   <li>"TRUE", "true"</li>
     *   <li>"1"</li>
     *   <li>"T", "t"</li>
     *   <li>"ON", "on"</li>
     * </ul>
     *
     * <h4>N으로 변환되는 값</h4>
     * <ul>
     *   <li>"N", "n" (대소문자 무관)</li>
     *   <li>"NO", "no"</li>
     *   <li>"FALSE", "false"</li>
     *   <li>"0"</li>
     *   <li>"F", "f"</li>
     *   <li>"OFF", "off"</li>
     *   <li>null, 빈 문자열, 기타 값</li>
     * </ul>
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * YNType type = YNType.from("Y");      // Y
     * YNType type = YNType.from("yes");    // Y
     * YNType type = YNType.from("true");   // Y
     * YNType type = YNType.from("1");      // Y
     * YNType type = YNType.from("on");     // Y
     *
     * YNType type = YNType.from("N");      // N
     * YNType type = YNType.from("no");     // N
     * YNType type = YNType.from("false");  // N
     * YNType type = YNType.from("0");      // N
     * YNType type = YNType.from(null);     // N
     * YNType type = YNType.from("abc");    // N (기본값)
     * }</pre>
     *
     * @param value 변환할 문자열
     * @return YNType, 유효하지 않은 값이면 N 반환
     */
    public static YNType from(String value) {
        if (value == null || value.isBlank()) {
            return N;
        }

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "Y", "YES", "TRUE", "1", "T", "ON" -> Y;
            default -> N;
        };
    }

    /**
     * 문자열로부터 YNType 변환 (기본값 지정)
     *
     * @param value        변환할 문자열
     * @param defaultValue 유효하지 않은 경우 사용할 기본값
     * @return YNType
     */
    public static YNType from(String value, YNType defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "Y", "YES", "TRUE", "1", "T", "ON" -> Y;
            case "N", "NO", "FALSE", "0", "F", "OFF" -> N;
            default -> defaultValue;
        };
    }

    /**
     * boolean으로부터 YNType 변환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * YNType type = YNType.from(true);   // Y
     * YNType type = YNType.from(false);  // N
     *
     * // 조건에 따른 값 설정
     * user.setIsActive(YNType.from(isActive));
     * }</pre>
     *
     * @param value boolean 값
     * @return true이면 Y, false이면 N
     */
    public static YNType from(boolean value) {
        return value ? Y : N;
    }

    /**
     * Boolean으로부터 YNType 변환 (null 안전)
     * <p>
     * null이면 N을 반환합니다.
     *
     * @param value Boolean 값 (null 허용)
     * @return true이면 Y, false 또는 null이면 N
     */
    public static YNType from(Boolean value) {
        return Boolean.TRUE.equals(value) ? Y : N;
    }

    /**
     * Boolean으로부터 YNType 변환 (기본값 지정)
     *
     * @param value        Boolean 값 (null 허용)
     * @param defaultValue null인 경우 사용할 기본값
     * @return YNType
     */
    public static YNType from(Boolean value, YNType defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value ? Y : N;
    }

    /**
     * 조건에 따라 YNType 반환
     * <p>
     * 삼항 연산자 대신 사용하여 가독성을 높입니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 삼항 연산자 대신
     * YNType type = age >= 18 ? YNType.Y : YNType.N;
     *
     * // when 사용
     * YNType type = YNType.when(age >= 18);
     * }</pre>
     *
     * @param condition 조건
     * @return 조건이 true이면 Y, false이면 N
     */
    public static YNType when(boolean condition) {
        return condition ? Y : N;
    }

    // ========================================
    // 유틸리티 메서드
    // ========================================

    /**
     * YNType을 boolean으로 안전하게 변환
     * <p>
     * null이면 false를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * boolean isActive = YNType.toBoolean(user.getIsActive());
     * boolean isDeleted = YNType.toBoolean(user.getIsDeleted());
     * }</pre>
     *
     * @param type YNType (null 허용)
     * @return Y이면 true, null이거나 N이면 false
     */
    public static boolean toBoolean(YNType type) {
        return type != null && type.booleanValue;
    }

    /**
     * YNType을 boolean으로 안전하게 변환 (기본값 지정)
     *
     * @param type         YNType (null 허용)
     * @param defaultValue null인 경우 반환할 기본값
     * @return boolean 값
     */
    public static boolean toBoolean(YNType type, boolean defaultValue) {
        if (type == null) {
            return defaultValue;
        }
        return type.booleanValue;
    }
}
