package io.groo.core.exception;

/**
 * 중복 데이터 예외 (409 Conflict)
 * <p>
 * 이미 존재하는 데이터를 다시 생성하려 할 때 발생시킵니다.
 * 유니크 제약 조건 위반이나 비즈니스 로직상 중복이 허용되지 않는 경우에 사용합니다.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>동일한 이메일로 사용자 등록 시도</li>
 *   <li>이미 존재하는 코드값 생성 시도</li>
 *   <li>유니크 인덱스 위반 감지</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 이메일 중복 검사
 * if (userRepository.existsByEmail(email)) {
 *     throw new DuplicateException(HrErrorCode.USER_DUPLICATE_EMAIL);
 * }
 *
 * // 커스텀 메시지 포함
 * throw new DuplicateException(HrErrorCode.USER_ALREADY_EXISTS, "이메일: " + email);
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 */
public class DuplicateException extends BusinessException {

    public DuplicateException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public DuplicateException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public DuplicateException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public DuplicateException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
