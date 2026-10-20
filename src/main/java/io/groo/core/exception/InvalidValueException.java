package io.groo.core.exception;

/**
 * 유효하지 않은 값 예외 (400 Bad Request)
 * <p>
 * 입력값이 형식에 맞지 않거나 허용 범위를 벗어날 때 발생시킵니다.
 * Bean Validation 외에 추가적인 비즈니스 유효성 검증에 사용합니다.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>날짜 범위가 유효하지 않은 경우 (시작일 > 종료일)</li>
 *   <li>숫자 값이 허용 범위를 벗어난 경우</li>
 *   <li>필수 파라미터가 누락된 경우</li>
 *   <li>형식이 맞지 않는 값이 입력된 경우</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 날짜 유효성 검사
 * if (startDate.isAfter(endDate)) {
 *     throw new InvalidValueException(ErrorCode.INVALID_DATE_RANGE);
 * }
 *
 * // 커스텀 메시지 포함
 * throw new InvalidValueException(HrErrorCode.VACATION_INVALID_DATE, "시작일이 종료일보다 늦습니다.");
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 * @see BusinessRuleViolationException
 */
public class InvalidValueException extends BusinessException {

    public InvalidValueException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public InvalidValueException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public InvalidValueException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public InvalidValueException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
