package io.groo.core.exception;

/**
 * 인증 실패 예외 (401 Unauthorized)
 * <p>
 * 인증이 필요하거나 인증 정보가 유효하지 않을 때 발생시킵니다.
 * 인증은 되었으나 권한이 없는 경우는 {@link ForbiddenException}을 사용하세요.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>로그인이 필요한 API에 미인증 접근</li>
 *   <li>토큰이 만료되었거나 유효하지 않은 경우</li>
 *   <li>잘못된 인증 정보로 로그인 시도</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 토큰 검증 실패
 * if (!jwtProvider.validateToken(token)) {
 *     throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
 * }
 *
 * // 커스텀 메시지 포함
 * throw new UnauthorizedException(ErrorCode.UNAUTHORIZED, "토큰이 만료되었습니다.");
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 * @see ForbiddenException
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public UnauthorizedException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UnauthorizedException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
