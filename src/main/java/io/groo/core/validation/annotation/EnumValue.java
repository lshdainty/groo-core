package io.groo.core.validation.annotation;

import io.groo.core.validation.validator.EnumValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enum 값 검증 어노테이션
 * <p>
 * 문자열 값이 지정된 Enum의 유효한 값인지 검증합니다.
 * API 요청에서 문자열로 받은 값이 Enum으로 변환 가능한지 확인할 때 사용합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * public enum Status {
 *     ACTIVE, INACTIVE, PENDING
 * }
 *
 * public class UserRequest {
 *     @EnumValue(enumClass = Status.class, message = "유효하지 않은 상태입니다.")
 *     private String status;
 *
 *     // 대소문자 무시
 *     @EnumValue(enumClass = Status.class, ignoreCase = true)
 *     private String status2;
 * }
 *
 * // 유효한 요청
 * { "status": "ACTIVE" }
 *
 * // 유효하지 않은 요청 (검증 실패)
 * { "status": "UNKNOWN" }
 * }</pre>
 *
 * @author groo
 * @see EnumValueValidator
 */
@Documented
@Constraint(validatedBy = EnumValueValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValue {

    /**
     * 검증 실패 시 메시지
     */
    String message() default "유효하지 않은 값입니다.";

    /**
     * 검증 그룹
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 검증할 Enum 클래스
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 대소문자 무시 여부
     * <p>
     * true로 설정하면 "active", "ACTIVE", "Active" 모두 유효합니다.
     */
    boolean ignoreCase() default false;

    /**
     * null 허용 여부
     * <p>
     * false로 설정하면 null 값도 검증 실패로 처리합니다.
     * 기본값은 true (null 허용, @NotNull로 별도 검증)
     */
    boolean nullable() default true;
}
