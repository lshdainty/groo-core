package io.groo.core.exception;

/**
 * 비즈니스 규칙 위반 예외 (400 Bad Request)
 * <p>
 * 비즈니스 로직상 허용되지 않는 동작을 시도할 때 발생시킵니다.
 * 단순 입력값 검증은 {@link InvalidValueException}을, 비즈니스 규칙 위반은 이 예외를 사용하세요.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>휴가 잔여일수가 부족한 경우</li>
 *   <li>이미 시작된 휴가를 취소하려는 경우</li>
 *   <li>소속 직원이 있는 부서를 삭제하려는 경우</li>
 *   <li>상태 전이가 허용되지 않는 경우</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 잔여일수 검사
 * if (vacation.getBalance() < requestedDays) {
 *     throw new BusinessRuleViolationException(HrErrorCode.VACATION_INSUFFICIENT_BALANCE);
 * }
 *
 * // 상태 검사
 * if (vacation.isStarted()) {
 *     throw new BusinessRuleViolationException(HrErrorCode.VACATION_CANNOT_CANCEL,
 *         "이미 시작된 휴가는 취소할 수 없습니다.");
 * }
 *
 * // 연관 데이터 검사
 * if (department.hasMembers()) {
 *     throw new BusinessRuleViolationException(HrErrorCode.DEPARTMENT_HAS_MEMBERS);
 * }
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 * @see InvalidValueException
 */
public class BusinessRuleViolationException extends BusinessException {

    public BusinessRuleViolationException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public BusinessRuleViolationException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public BusinessRuleViolationException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public BusinessRuleViolationException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
