package io.groo.core.util;

import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.ErrorCodeProvider;
import io.groo.core.message.MessageKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * 다국어 메시지 처리를 위한 통합 유틸리티
 * <p>
 * Spring의 {@link MessageSource}를 래핑하여 타입 안전한 메시지 조회를 제공합니다.
 * {@link MessageKey} enum 또는 {@link ErrorCodeProvider}를 사용하여
 * 컴파일 타임에 메시지 키 오류를 방지합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>MessageKey 기반 메시지 조회 (권장)</li>
 *   <li>ErrorCodeProvider 기반 메시지 조회 (예외 처리용)</li>
 *   <li>문자열 키 기반 메시지 조회 (하위 호환용)</li>
 *   <li>파라미터 치환 지원</li>
 *   <li>현재 Locale 자동 적용</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class UserService {
 *     private final MessageResolver messageResolver;
 *
 *     // MessageKey 사용 (권장)
 *     public String getWelcomeMessage() {
 *         return messageResolver.getMessage(MessageKey.COMMON_SUCCESS);
 *     }
 *
 *     // 파라미터 포함
 *     public String getFileNotFoundMessage(String filename) {
 *         return messageResolver.getMessage(MessageKey.FILE_NOT_FOUND, filename);
 *     }
 *
 *     // ErrorCode 사용 (예외 처리)
 *     public void validateUser(User user) {
 *         if (user == null) {
 *             String msg = messageResolver.getMessage(ErrorCode.USER_NOT_FOUND);
 *             throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND, msg);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>메시지 파일 구조</h3>
 * <pre>
 * src/main/resources/message/
 * ├── messages.properties      # 기본 (영어)
 * ├── messages_en.properties   # 영어
 * └── messages_ko.properties   # 한국어
 * </pre>
 *
 * <h3>Locale 결정</h3>
 * <p>
 * {@link LocaleContextHolder}를 통해 현재 요청의 Locale을 자동으로 적용합니다.
 * Accept-Language 헤더 또는 세션/쿠키 기반으로 Locale이 결정됩니다.
 *
 * @author groo
 * @see MessageKey
 * @see ErrorCodeProvider
 * @see MessageSource
 * @see LocaleContextHolder
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    // ========================================
    // MessageKey 기반 메시지 조회 (권장)
    // ========================================

    /**
     * MessageKey로 메시지 조회
     * <p>
     * 지정된 MessageKey에 해당하는 다국어 메시지를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String message = messageResolver.getMessage(MessageKey.COMMON_SUCCESS);
     * // 결과: "성공적으로 처리되었습니다." (한국어) 또는 "Success" (영어)
     * }</pre>
     *
     * @param messageKey 메시지 키 enum
     * @return 현재 Locale에 맞는 다국어 메시지
     */
    public String getMessage(MessageKey messageKey) {
        return messageSource.getMessage(
                messageKey.getKey(),
                null,
                messageKey.getKey(),
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * MessageKey로 메시지 조회 (파라미터 치환)
     * <p>
     * 지정된 MessageKey에 해당하는 메시지를 조회하고, 파라미터를 치환합니다.
     * 메시지 내 {0}, {1}, {2}... 플레이스홀더가 args 순서대로 치환됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // messages_ko.properties: error.file.notfound=파일을 찾을 수 없습니다: {0}
     * String message = messageResolver.getMessage(MessageKey.FILE_NOT_FOUND, "test.txt");
     * // 결과: "파일을 찾을 수 없습니다: test.txt"
     *
     * // 여러 파라미터
     * // messages_ko.properties: file.copy=파일을 {0}에서 {1}로 복사했습니다.
     * String message = messageResolver.getMessage(MessageKey.FILE_COPY, "/src/a.txt", "/dest/a.txt");
     * // 결과: "파일을 /src/a.txt에서 /dest/a.txt로 복사했습니다."
     * }</pre>
     *
     * @param messageKey 메시지 키 enum
     * @param args       메시지 파라미터 ({0}, {1}... 순서로 치환)
     * @return 현재 Locale에 맞는 다국어 메시지 (파라미터 치환됨)
     */
    public String getMessage(MessageKey messageKey, Object... args) {
        return messageSource.getMessage(
                messageKey.getKey(),
                args,
                messageKey.getKey(),
                LocaleContextHolder.getLocale()
        );
    }

    // ========================================
    // ErrorCodeProvider 기반 메시지 조회 (예외 처리용)
    // ========================================

    /**
     * ErrorCodeProvider에서 메시지 조회
     * <p>
     * Core의 ErrorCode와 각 모듈의 ErrorCode enum 등
     * 모든 ErrorCodeProvider 구현체를 지원합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 예외 처리 시
     * throw new EntityNotFoundException(
     *     ErrorCode.USER_NOT_FOUND,
     *     messageResolver.getMessage(ErrorCode.USER_NOT_FOUND)
     * );
     * }</pre>
     *
     * @param errorCode 에러 코드 (ErrorCodeProvider 구현체)
     * @return 현재 Locale에 맞는 다국어 에러 메시지
     */
    public String getMessage(ErrorCodeProvider errorCode) {
        return messageSource.getMessage(
                errorCode.getMessageKey(),
                null,
                errorCode.getCode(),
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * ErrorCodeProvider에서 메시지 조회 (파라미터 치환)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // messages_ko.properties: error.file.size.exceeded=파일 크기가 {0}MB를 초과했습니다.
     * String message = messageResolver.getMessage(ErrorCode.FILE_SIZE_EXCEEDED, 10);
     * // 결과: "파일 크기가 10MB를 초과했습니다."
     * }</pre>
     *
     * @param errorCode 에러 코드 (ErrorCodeProvider 구현체)
     * @param args      메시지 파라미터 ({0}, {1}... 순서로 치환)
     * @return 현재 Locale에 맞는 다국어 에러 메시지 (파라미터 치환됨)
     */
    public String getMessage(ErrorCodeProvider errorCode, Object... args) {
        return messageSource.getMessage(
                errorCode.getMessageKey(),
                args,
                errorCode.getCode(),
                LocaleContextHolder.getLocale()
        );
    }

    // ========================================
    // ErrorCode 기반 메시지 조회 (하위 호환용)
    // ========================================

    /**
     * ErrorCode에서 메시지 가져오기 (현재 Locale 사용)
     *
     * @param errorCode 에러 코드 enum
     * @return 다국어 메시지
     * @deprecated ErrorCodeProvider를 사용하는 getMessage(ErrorCodeProvider) 사용 권장
     */
    @Deprecated
    public String getMessage(ErrorCode errorCode) {
        return getMessage((ErrorCodeProvider) errorCode);
    }

    /**
     * ErrorCode에서 메시지 가져오기 (파라미터 포함)
     *
     * @param errorCode 에러 코드 enum
     * @param args 메시지 파라미터
     * @return 다국어 메시지
     * @deprecated ErrorCodeProvider를 사용하는 getMessage(ErrorCodeProvider, Object...) 사용 권장
     */
    @Deprecated
    public String getMessage(ErrorCode errorCode, Object... args) {
        return getMessage((ErrorCodeProvider) errorCode, args);
    }

    // ========================================
    // 문자열 키 기반 메시지 조회 (하위 호환용)
    // ========================================

    /**
     * 메시지 키 문자열로 직접 메시지 가져오기
     * 가능하면 MessageKey enum 사용을 권장
     *
     * @param messageKey 메시지 키 문자열
     * @return 다국어 메시지
     */
    public String getMessage(String messageKey) {
        return messageSource.getMessage(
                messageKey,
                null,
                messageKey,
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * 메시지 키 문자열로 직접 메시지 가져오기 (파라미터 포함)
     * 가능하면 MessageKey enum 사용을 권장
     *
     * @param messageKey 메시지 키 문자열
     * @param args 메시지 파라미터
     * @return 다국어 메시지
     */
    public String getMessage(String messageKey, Object... args) {
        return messageSource.getMessage(
                messageKey,
                args,
                messageKey,
                LocaleContextHolder.getLocale()
        );
    }
}
