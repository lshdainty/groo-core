package io.groo.core.exception;

/**
 * 외부 서비스 연동 실패 예외 (5xx Server Error)
 * <p>
 * 외부 API, 메시징 서비스, 파일 시스템 등 외부 시스템 연동 실패 시 발생시킵니다.
 * 내부 비즈니스 로직 오류는 다른 예외를 사용하세요.
 *
 * <h3>사용 시점</h3>
 * <ul>
 *   <li>OAuth2 인증 서버 연동 실패</li>
 *   <li>이메일 발송 서버 응답 없음</li>
 *   <li>외부 API 호출 타임아웃</li>
 *   <li>파일 시스템 I/O 오류</li>
 *   <li>메시지 큐 연결 실패</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 외부 API 호출 실패
 * try {
 *     restTemplate.getForObject(url, Response.class);
 * } catch (RestClientException e) {
 *     throw new ExternalServiceException(ErrorCode.INTERNAL_SERVER_ERROR,
 *         "외부 API 호출 실패: " + url, e);
 * }
 *
 * // 이메일 발송 실패
 * try {
 *     mailSender.send(message);
 * } catch (MailException e) {
 *     throw new ExternalServiceException(ErrorCode.INTERNAL_SERVER_ERROR,
 *         "이메일 발송 실패", e);
 * }
 * }</pre>
 *
 * @author groo
 * @see BusinessException
 */
public class ExternalServiceException extends BusinessException {

    public ExternalServiceException(ErrorCodeProvider errorCode) {
        super(errorCode);
    }

    public ExternalServiceException(ErrorCodeProvider errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    public ExternalServiceException(ErrorCodeProvider errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ExternalServiceException(ErrorCodeProvider errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
}
