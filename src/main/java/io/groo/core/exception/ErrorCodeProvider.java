package io.groo.core.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 공통 인터페이스
 * <p>
 * Core의 {@link ErrorCode}와 각 도메인별 ErrorCode가 구현하는 공통 인터페이스입니다.
 * {@link BusinessException}에서 이 인터페이스를 통해 다양한 ErrorCode를 일관되게 처리합니다.
 *
 * <h3>구현 예시</h3>
 * <pre>{@code
 * @Getter
 * @RequiredArgsConstructor
 * public enum HrErrorCode implements ErrorCodeProvider {
 *     USER_NOT_FOUND("USER_001", "error.user.not.found", HttpStatus.NOT_FOUND),
 *     VACATION_INSUFFICIENT("VACATION_001", "error.vacation.insufficient", HttpStatus.BAD_REQUEST);
 *
 *     private final String code;
 *     private final String messageKey;
 *     private final HttpStatus httpStatus;
 * }
 * }</pre>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // BusinessException에서 사용
 * throw new EntityNotFoundException(HrErrorCode.USER_NOT_FOUND);
 *
 * // MessageResolver에서 사용
 * String message = messageResolver.getMessage(HrErrorCode.USER_NOT_FOUND);
 * }</pre>
 *
 * @author groo
 * @see ErrorCode
 * @see BusinessException
 */
public interface ErrorCodeProvider {

    /**
     * 응답 코드를 반환합니다. (예: USER_001, VACATION_002)
     *
     * @return 응답 코드
     */
    String getCode();

    /**
     * 메시지 키를 반환합니다. (messages.properties의 키)
     *
     * @return 메시지 키
     */
    String getMessageKey();

    /**
     * HTTP 상태 코드를 반환합니다.
     *
     * @return HTTP 상태 코드
     */
    HttpStatus getHttpStatus();

    /**
     * HTTP 상태 코드 값을 반환합니다.
     *
     * @return HTTP 상태 코드 값
     */
    default int getHttpStatusCode() {
        return getHttpStatus().value();
    }
}
