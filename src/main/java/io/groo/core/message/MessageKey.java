package io.groo.core.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Core 공통 메시지 키
 * <p>
 * {@code messages.properties}의 키를 enum으로 관리하여 타입 안전성을 보장합니다.
 * 도메인별 메시지 키는 각 모듈의 MessageKey enum에서 별도로 정의합니다.
 *
 * <h3>메시지 파일 구조</h3>
 * <pre>
 * src/main/resources/message/
 * ├── messages.properties      # 기본 (영어)
 * ├── messages_ko.properties   # 한국어
 * └── messages_en.properties   # 영어
 * </pre>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // MessageResolver를 통해 메시지 조회
 * String message = messageResolver.getMessage(MessageKey.COMMON_SUCCESS);
 *
 * // 파라미터 포함
 * String message = messageResolver.getMessage(MessageKey.FILE_NOT_FOUND, "test.txt");
 * }</pre>
 *
 * <h3>메시지 파일 예시</h3>
 * <pre>
 * # messages_ko.properties
 * error.common.success=성공적으로 처리되었습니다.
 * error.file.notfound=파일을 찾을 수 없습니다: {0}
 * </pre>
 *
 * @author groo
 * @see io.groo.core.util.MessageResolver
 */
@Getter
@RequiredArgsConstructor
public enum MessageKey {

    // ========================================
    // FILE (파일 관련)
    // ========================================
    FILE_NOT_FOUND("error.file.notfound"),
    FILE_READ("error.file.read"),
    FILE_COPY("error.file.copy"),
    FILE_MOVE("error.file.move"),
    FILE_SAVE_ERROR("error.file.save"),
    FILE_INVALID_TYPE("error.file.invalid.type"),
    FILE_TOO_LARGE("error.file.too.large"),

    // ========================================
    // COMMON (공통)
    // ========================================
    COMMON_SUCCESS("error.common.success"),
    COMMON_INVALID_INPUT("error.common.invalid.input"),
    COMMON_UNAUTHORIZED("error.common.unauthorized"),
    COMMON_FORBIDDEN("error.common.forbidden"),
    COMMON_NOT_FOUND("error.common.not.found"),
    COMMON_404("error.common.404"),
    COMMON_MISSING_PARAMETER("error.common.missing.parameter"),
    COMMON_INTERNAL_SERVER("error.common.internal.server"),

    ;

    private final String key;
}
