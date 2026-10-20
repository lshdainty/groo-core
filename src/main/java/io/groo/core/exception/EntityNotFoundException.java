package io.groo.core.exception;

/**
 * 엔티티 조회 실패 예외 (404 Not Found)
 * <p>
 * 데이터베이스에서 엔티티를 찾을 수 없을 때 발생시킵니다.
 * JPA Repository의 조회 결과가 없거나 Optional이 비어있을 때 사용합니다.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>ID로 엔티티 조회 시 결과가 없는 경우</li>
 *   <li>특정 조건으로 조회 시 결과가 없는 경우</li>
 *   <li>연관 엔티티가 존재하지 않는 경우</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 기본 사용
 * User user = userRepository.findById(userId)
 *     .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NOT_FOUND));
 *
 * // 도메인별 ErrorCode 사용
 * throw new EntityNotFoundException(HrErrorCode.USER_NOT_FOUND);
 *
 * // 커스텀 메시지 포함 (로깅용)
 * throw new EntityNotFoundException(HrErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId);
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 * @see ResourceNotFoundException
 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public EntityNotFoundException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public EntityNotFoundException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EntityNotFoundException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
