package io.groo.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Core 공통 에러 코드
 * <p>
 * 프로젝트 전반에서 사용되는 공통 에러 코드를 정의합니다.
 * 도메인 특화 에러 코드는 각 모듈의 ErrorCode enum에서 별도로 정의합니다.
 *
 * <h3>에러 코드 구조</h3>
 * <ul>
 *   <li><b>COMMON_xxx</b>: 공통 에러 (인증, 권한, 유효성 검증 등)</li>
 *   <li><b>FILE_xxx</b>: 파일 관련 에러</li>
 * </ul>
 *
 * <h3>메시지 처리</h3>
 * <p>
 * 실제 에러 메시지는 {@code messages.properties}에서 다국어로 관리됩니다.
 * {@code messageKey} 필드가 메시지 파일의 키와 매핑됩니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 예외 발생
 * throw new InvalidValueException(ErrorCode.INVALID_INPUT);
 * throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
 *
 * // 메시지 조회
 * String message = messageResolver.getMessage(ErrorCode.NOT_FOUND);
 * }</pre>
 *
 * @author groo
 * @see ErrorCodeProvider
 * @see BusinessException
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeProvider {

    // ========================================
    // COMMON (공통)
    // ========================================
    SUCCESS("COMMON_200", "error.common.success", HttpStatus.OK),
    INVALID_INPUT("COMMON_400", "error.common.invalid.input", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("COMMON_401", "error.common.invalid.date.range", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("COMMON_402", "error.common.invalid.parameter", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_TYPE("COMMON_403", "error.common.unsupported.type", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON_411", "error.common.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON_412", "error.common.forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON_404", "error.common.not.found", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("COMMON_500", "error.common.internal.server", HttpStatus.INTERNAL_SERVER_ERROR),
    // 클라이언트 실수·경합인데 매핑이 없어 500 으로 새던 예외(3.1.0). groo-damda-back 이 각자 메우던 것을 올렸다.
    // 코드·메시지 키는 damda 가 쓰던 것과 같다 — damda 는 자기 번들 문구(~어요)로 덮고, 다른 서비스는 core 문구를 쓴다.
    MALFORMED_REQUEST_BODY("COMMON_400", "error.common.malformed.request.body", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("COMMON_405", "error.common.method.not.allowed", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("COMMON_415", "error.common.unsupported.media.type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INVALID_DATE_VALUE("COMMON_400", "error.common.invalid.date.value", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_VALUE("COMMON_400", "error.common.invalid.parameter.value", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("COMMON_400", "error.common.missing.parameter", HttpStatus.BAD_REQUEST),
    REQUIRED_VALUE_MISSING("COMMON_400", "error.common.required.value.missing", HttpStatus.BAD_REQUEST),
    INVALID_REFERENCE("COMMON_400", "error.common.invalid.reference", HttpStatus.BAD_REQUEST),
    CONCURRENT_MODIFICATION("COMMON_409", "error.common.concurrent.modification", HttpStatus.CONFLICT),
    // 필터·인터셉터에서 쓰는 보호 장치 응답(3.2.0)
    RATE_LIMITED("COMMON_429", "error.common.rate.limited", HttpStatus.TOO_MANY_REQUESTS),
    TEMPORARILY_UNAVAILABLE("COMMON_503", "error.common.temporarily.unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    // ========================================
    // FILE (파일)
    // ========================================
    FILE_NOT_FOUND("FILE_001", "error.file.notfound", HttpStatus.NOT_FOUND),
    FILE_INVALID_TYPE("FILE_002", "error.file.invalid.type", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE_003", "error.file.too.large", HttpStatus.BAD_REQUEST),

    ;

    /**
     * 응답 코드 (예: COMMON_200, FILE_001)
     */
    private final String code;

    /**
     * 메시지 키 (messages.properties의 키)
     */
    private final String messageKey;

    /**
     * HTTP 상태 코드
     */
    private final HttpStatus httpStatus;
}
