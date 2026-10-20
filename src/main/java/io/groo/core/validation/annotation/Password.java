package io.groo.core.validation.annotation;

import io.groo.core.validation.validator.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호 규칙 검증 어노테이션
 * <p>
 * 비밀번호가 지정된 보안 규칙을 만족하는지 검증합니다.
 * 강도(BASIC, STRONG)에 따라 다른 규칙을 적용합니다.
 *
 * <h3>강도별 규칙</h3>
 * <table border="1">
 *   <tr><th>강도</th><th>규칙</th></tr>
 *   <tr><td>BASIC</td><td>최소 8자, 영문+숫자 포함</td></tr>
 *   <tr><td>STRONG</td><td>최소 8자, 영문 대/소문자+숫자+특수문자 포함</td></tr>
 * </table>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * public class SignUpRequest {
 *     // 기본 강도 (영문+숫자)
 *     @Password(message = "비밀번호는 8자 이상, 영문과 숫자를 포함해야 합니다.")
 *     private String password;
 *
 *     // 강력한 강도 (영문 대/소문자+숫자+특수문자)
 *     @Password(strength = Password.Strength.STRONG,
 *               message = "비밀번호는 8자 이상, 대소문자, 숫자, 특수문자를 포함해야 합니다.")
 *     private String adminPassword;
 *
 *     // 최소 길이 변경
 *     @Password(minLength = 12, strength = Password.Strength.STRONG)
 *     private String securePassword;
 * }
 * }</pre>
 *
 * @author groo
 * @see PasswordValidator
 * @see io.groo.core.util.RegexPatterns#PASSWORD_BASIC
 * @see io.groo.core.util.RegexPatterns#PASSWORD_STRONG
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {

    /**
     * 검증 실패 시 메시지
     */
    String message() default "비밀번호 형식이 올바르지 않습니다.";

    /**
     * 검증 그룹
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 비밀번호 강도
     * <p>
     * 기본값은 BASIC
     */
    Strength strength() default Strength.BASIC;

    /**
     * 최소 길이
     * <p>
     * 기본값은 8
     */
    int minLength() default 8;

    /**
     * 최대 길이
     * <p>
     * 기본값은 100
     */
    int maxLength() default 100;

    /**
     * 비밀번호 강도 enum
     */
    enum Strength {
        /**
         * 기본 강도: 최소 길이, 영문+숫자 포함
         */
        BASIC,

        /**
         * 강력한 강도: 최소 길이, 영문 대/소문자+숫자+특수문자 포함
         */
        STRONG
    }
}
