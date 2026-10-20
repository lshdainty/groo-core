package io.groo.core.type;

/**
 * 정렬 방향 Enum
 * <p>
 * 데이터 정렬 시 사용하는 방향을 정의합니다.
 * Spring Data의 Sort.Direction과 호환됩니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 요청 DTO에서 사용
 * public class SearchRequest {
 *     private SortDirection direction = SortDirection.DESC;
 *     private String sortBy = "createdAt";
 * }
 *
 * // Spring Data Sort로 변환
 * Sort sort = Sort.by(
 *     request.getDirection().toSpringDirection(),
 *     request.getSortBy()
 * );
 *
 * // QueryDSL에서 사용
 * OrderSpecifier<?> order = direction == SortDirection.ASC
 *     ? entity.createdAt.asc()
 *     : entity.createdAt.desc();
 * }</pre>
 *
 * @author groo
 * @see org.springframework.data.domain.Sort.Direction
 */
public enum SortDirection {

    /**
     * 오름차순 (A → Z, 1 → 9, 과거 → 최근)
     */
    ASC("asc", "오름차순"),

    /**
     * 내림차순 (Z → A, 9 → 1, 최근 → 과거)
     */
    DESC("desc", "내림차순");

    private final String value;
    private final String description;

    SortDirection(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 소문자 값 반환
     *
     * @return "asc" 또는 "desc"
     */
    public String getValue() {
        return value;
    }

    /**
     * 한글 설명 반환
     *
     * @return "오름차순" 또는 "내림차순"
     */
    public String getDescription() {
        return description;
    }

    /**
     * 오름차순 여부 확인
     *
     * @return 오름차순이면 true
     */
    public boolean isAscending() {
        return this == ASC;
    }

    /**
     * 내림차순 여부 확인
     *
     * @return 내림차순이면 true
     */
    public boolean isDescending() {
        return this == DESC;
    }

    /**
     * 문자열로부터 SortDirection 변환
     * <p>
     * 대소문자를 구분하지 않습니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * SortDirection dir = SortDirection.from("asc");   // ASC
     * SortDirection dir = SortDirection.from("DESC");  // DESC
     * SortDirection dir = SortDirection.from("invalid"); // DESC (기본값)
     * }</pre>
     *
     * @param value 변환할 문자열 ("asc" 또는 "desc")
     * @return SortDirection, 유효하지 않은 값이면 DESC 반환
     */
    public static SortDirection from(String value) {
        if (value == null) {
            return DESC;
        }

        for (SortDirection direction : values()) {
            if (direction.name().equalsIgnoreCase(value) ||
                    direction.value.equalsIgnoreCase(value)) {
                return direction;
            }
        }

        return DESC;
    }

    /**
     * 문자열로부터 SortDirection 변환 (기본값 지정)
     *
     * @param value        변환할 문자열
     * @param defaultValue 유효하지 않은 경우 사용할 기본값
     * @return SortDirection
     */
    public static SortDirection from(String value, SortDirection defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        for (SortDirection direction : values()) {
            if (direction.name().equalsIgnoreCase(value) ||
                    direction.value.equalsIgnoreCase(value)) {
                return direction;
            }
        }

        return defaultValue;
    }

    /**
     * 반대 방향 반환
     * <p>
     * ASC ↔ DESC 변환에 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * SortDirection current = SortDirection.ASC;
     * SortDirection opposite = current.opposite();  // DESC
     * }</pre>
     *
     * @return 반대 방향
     */
    public SortDirection opposite() {
        return this == ASC ? DESC : ASC;
    }
}
