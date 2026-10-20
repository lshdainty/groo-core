package io.groo.core.validation.validator;

import io.groo.core.validation.annotation.Password;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 비밀번호 검증 Validator
 * <p>
 * {@link Password} 어노테이션의 검증 로직을 구현합니다.
 * 비밀번호 강도에 따라 다른 규칙을 적용합니다.
 *
 * @author groo
 * @see Password
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

    private Password.Strength strength;
    private int minLength;
    private int maxLength;

    // 기본 강도: 영문+숫자
    private static final Pattern BASIC_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d).+$"
    );

    // 강력한 강도: 영문 대문자+소문자+숫자+특수문자
    private static final Pattern STRONG_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).+$"
    );

    @Override
    public void initialize(Password constraintAnnotation) {
        this.strength = constraintAnnotation.strength();
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null은 유효한 것으로 처리 (@NotNull로 별도 검증)
        if (value == null) {
            return true;
        }

        // 빈 문자열 검증
        if (value.isBlank()) {
            return true;
        }

        // 길이 검증
        if (value.length() < minLength || value.length() > maxLength) {
            return false;
        }

        // 강도별 패턴 검증
        return switch (strength) {
            case BASIC -> BASIC_PATTERN.matcher(value).find();
            case STRONG -> STRONG_PATTERN.matcher(value).find();
        };
    }
}
