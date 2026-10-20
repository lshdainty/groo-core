package io.groo.core.validation.annotation;

import io.groo.core.validation.validator.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 전화번호 형식 검증 어노테이션
 * <p>
 * 필드 값이 유효한 전화번호 형식인지 검증합니다.
 * null 값은 유효한 것으로 처리됩니다 (null 검증은 @NotNull 사용).
 *
 * <h3>지원 형식</h3>
 * <ul>
 *   <li>010-1234-5678 (휴대전화)</li>
 *   <li>02-123-4567 (서울 지역번호)</li>
 *   <li>031-1234-5678 (경기 지역번호)</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * public class UserRequest {
 *     @NotBlank
 *     @Phone(message = "올바른 전화번호 형식이 아닙니다.")
 *     private String phone;
 *
 *     @Phone(mobileOnly = true, message = "휴대전화 번호만 입력 가능합니다.")
 *     private String mobile;
 * }
 * }</pre>
 *
 * @author groo
 * @see PhoneValidator
 * @see io.groo.core.util.RegexPatterns#PHONE
 */
@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {

    /**
     * 검증 실패 시 메시지
     */
    String message() default "올바른 전화번호 형식이 아닙니다.";

    /**
     * 검증 그룹
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 휴대전화만 허용할지 여부
     * <p>
     * true로 설정하면 010, 011, 016, 017, 018, 019로 시작하는 번호만 허용합니다.
     */
    boolean mobileOnly() default false;

    /**
     * 하이픈 없는 형식도 허용할지 여부
     * <p>
     * true로 설정하면 01012345678 형식도 허용합니다.
     */
    boolean allowWithoutHyphen() default false;
}
