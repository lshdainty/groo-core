package io.groo.core.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외의 기본 클래스
 * <p>
 * 비즈니스 로직 처리 중 발생하는 모든 예외의 상위 클래스입니다.
 * {@link ErrorCodeProvider}를 포함하여 일관된 에러 응답을 제공합니다.
 *
 * <h3>예외 계층 구조</h3>
 * <pre>
 * BusinessException (기본)
 * ├── EntityNotFoundException (404)     - 엔티티 조회 실패
 * ├── ResourceNotFoundException (404)   - 리소스 조회 실패
 * ├── DuplicateException (409)          - 중복 데이터
 * ├── InvalidValueException (400)       - 유효하지 않은 값
 * ├── BusinessRuleViolationException (400) - 비즈니스 규칙 위반
 * ├── UnauthorizedException (401)       - 인증 실패
 * ├── ForbiddenException (403)          - 권한 없음
 * └── ExternalServiceException (5xx)    - 외부 서비스 연동 실패
 * </pre>
 *
 * <h3>메시지 처리</h3>
 * <p>
 * 실제 에러 메시지는 {@code GlobalExceptionHandler}에서 {@code MessageSource}를 통해
 * 다국어로 처리됩니다. 생성자에 전달된 메시지는 로깅 용도로 사용됩니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // ErrorCode만 사용
 * throw new BusinessException(ErrorCode.INVALID_INPUT);
 *
 * // 커스텀 메시지 포함 (로깅용)
 * throw new BusinessException(ErrorCode.INVALID_INPUT, "필수 파라미터 누락: userId");
 *
 * // 원인 예외 포함
 * throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
 * }</pre>
 *
 * @author groo
 * @see ErrorCodeProvider
 * @see ErrorCode
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCodeProvider errorCode;

    /**
     * ErrorCodeProvider만 사용하는 생성자
     * 메시지는 GlobalExceptionHandler에서 MessageSource를 통해 가져옴
     */
    public BusinessException(ErrorCodeProvider errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCodeProvider + 커스텀 메시지 생성자
     * 커스텀 메시지를 직접 지정할 때 사용
     */
    public BusinessException(ErrorCodeProvider errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /**
     * ErrorCodeProvider + Cause 생성자
     */
    public BusinessException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode.getMessageKey(), cause);
        this.errorCode = errorCode;
    }

    /**
     * ErrorCodeProvider + 커스텀 메시지 + Cause 생성자
     */
    public BusinessException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }
}
