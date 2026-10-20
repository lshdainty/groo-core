package io.groo.core.type;

/**
 * 화면 표시용 타입의 공통 인터페이스
 * <p>
 * API를 통해 프론트엔드에 Enum 값을 전달할 때 사용하는 공통 인터페이스입니다.
 * 다국어 메시지 키와 정렬 순서를 제공하여 일관된 방식으로 타입 목록을 반환합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>다국어 지원: 메시지 키를 통해 locale에 맞는 텍스트 반환</li>
 *   <li>정렬 순서: 화면에 표시할 순서 지정</li>
 *   <li>공통 API: 모든 Enum을 동일한 형식으로 반환</li>
 * </ul>
 *
 * <h3>구현 예시</h3>
 * <pre>{@code
 * public enum VacationType implements DisplayType {
 *     ANNUAL(1L),
 *     SICK(2L),
 *     PERSONAL(3L);
 *
 *     private static final String MESSAGE_KEY_PREFIX = "type.vacation.";
 *     private final Long orderSeq;
 *
 *     VacationType(Long orderSeq) {
 *         this.orderSeq = orderSeq;
 *     }
 *
 *     @Override
 *     public String getMessageKey() {
 *         return MESSAGE_KEY_PREFIX + this.name().toLowerCase();
 *     }
 *
 *     @Override
 *     public Long getOrderSeq() {
 *         return orderSeq;
 *     }
 * }
 * }</pre>
 *
 * <h3>API 응답 형식</h3>
 * <pre>{@code
 * // GET /api/types/vacation-type
 * [
 *   { "code": "ANNUAL", "name": "연차", "orderSeq": 1 },
 *   { "code": "SICK", "name": "병가", "orderSeq": 2 },
 *   { "code": "PERSONAL", "name": "개인휴가", "orderSeq": 3 }
 * ]
 * }</pre>
 *
 * <h3>프론트엔드 사용</h3>
 * <pre>{@code
 * // 드롭다운 옵션 로딩
 * const vacationTypes = await fetch('/api/types/vacation-type');
 * // Select 컴포넌트에 바인딩
 * <Select options={vacationTypes.map(t => ({ value: t.code, label: t.name }))} />
 * }</pre>
 *
 * @author groo
 * @see CountryCode
 */
public interface DisplayType {

    /**
     * 다국어 메시지 키 반환
     * <p>
     * MessageSource에서 locale에 맞는 텍스트를 조회할 때 사용합니다.
     *
     * <h4>네이밍 규칙</h4>
     * <pre>
     * type.{도메인}.{enum값}
     * 예: type.vacation.annual, type.country.kr
     * </pre>
     *
     * @return 메시지 키 (예: "type.vacation.annual")
     */
    String getMessageKey();

    /**
     * 정렬 순서 반환
     * <p>
     * 화면에 표시할 때 정렬 기준으로 사용합니다.
     * 숫자가 작을수록 먼저 표시됩니다.
     *
     * @return 정렬 순서 (1부터 시작 권장)
     */
    Long getOrderSeq();

    // ========================================
    // Default 메서드
    // ========================================

    /**
     * Enum 코드 반환 (name)
     * <p>
     * Enum 상수의 이름을 반환합니다. API 응답의 code 필드로 사용됩니다.
     *
     * @return Enum 상수 이름 (예: "ANNUAL", "KR")
     */
    default String getCode() {
        if (this instanceof Enum<?> e) {
            return e.name();
        }
        return toString();
    }

    /**
     * Enum 코드를 소문자로 반환
     *
     * @return 소문자 코드 (예: "annual", "kr")
     */
    default String getCodeLower() {
        return getCode().toLowerCase();
    }

    /**
     * Enum 코드를 대문자로 반환
     *
     * @return 대문자 코드 (예: "ANNUAL", "KR")
     */
    default String getCodeUpper() {
        return getCode().toUpperCase();
    }

    /**
     * Enum의 ordinal 값 반환
     *
     * @return ordinal 값, Enum이 아니면 0
     */
    default int getOrdinal() {
        if (this instanceof Enum<?> e) {
            return e.ordinal();
        }
        return 0;
    }

    /**
     * 정렬 순서가 지정되었는지 확인
     *
     * @return orderSeq가 null이 아니면 true
     */
    default boolean hasOrderSeq() {
        return getOrderSeq() != null;
    }

    /**
     * 정렬 순서를 int로 반환
     * <p>
     * null인 경우 Integer.MAX_VALUE를 반환하여 마지막에 정렬됩니다.
     *
     * @return 정렬 순서 (int)
     */
    default int getOrderSeqInt() {
        Long seq = getOrderSeq();
        return seq != null ? seq.intValue() : Integer.MAX_VALUE;
    }
}
