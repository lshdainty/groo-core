package io.groo.core.exception;

/**
 * 접근 권한 없음 예외 (403 Forbidden)
 * <p>
 * 인증은 되었으나 해당 리소스에 대한 접근 권한이 없을 때 발생시킵니다.
 * 인증 자체가 안 된 경우는 {@link UnauthorizedException}을 사용하세요.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>권한이 부족한 API 접근 시도</li>
 *   <li>다른 사용자의 리소스에 접근 시도</li>
 *   <li>관리자 전용 기능에 일반 사용자 접근</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 권한 검사
 * if (!user.hasRole(Role.ADMIN)) {
 *     throw new ForbiddenException(ErrorCode.FORBIDDEN);
 * }
 *
 * // 리소스 소유권 검사
 * if (!post.isOwnedBy(currentUser)) {
 *     throw new ForbiddenException(HrErrorCode.PERMISSION_DENIED, "본인의 게시글만 수정할 수 있습니다.");
 * }
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 * @see UnauthorizedException
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public ForbiddenException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public ForbiddenException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ForbiddenException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
