package io.groo.core.exception;

/**
 * 리소스 조회 실패 예외 (404 Not Found)
 * <p>
 * 파일, 외부 리소스 등 엔티티가 아닌 리소스를 찾을 수 없을 때 발생시킵니다.
 * 데이터베이스 엔티티는 {@link EntityNotFoundException}을 사용하세요.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>파일 시스템에서 파일을 찾을 수 없는 경우</li>
 *   <li>외부 URL 리소스가 존재하지 않는 경우</li>
 *   <li>설정 파일이 누락된 경우</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 파일 조회 실패
 * if (!Files.exists(path)) {
 *     throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
 * }
 *
 * // 커스텀 메시지 포함
 * throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND, "파일 경로: " + path);
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 * @see EntityNotFoundException
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public ResourceNotFoundException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ResourceNotFoundException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
