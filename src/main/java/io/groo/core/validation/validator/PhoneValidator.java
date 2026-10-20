package io.groo.core.validation.validator;

import io.groo.core.util.RegexPatterns;
import io.groo.core.validation.annotation.Phone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 전화번호 검증 Validator
 * <p>
 * {@link Phone} 어노테이션의 검증 로직을 구현합니다.
 *
 * @author groo
 * @see Phone
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private boolean mobileOnly;
    private boolean allowWithoutHyphen;

    @Override
    public void initialize(Phone constraintAnnotation) {
        this.mobileOnly = constraintAnnotation.mobileOnly();
        this.allowWithoutHyphen = constraintAnnotation.allowWithoutHyphen();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null은 유효한 것으로 처리 (@NotNull로 별도 검증)
        if (value == null || value.isBlank()) {
            return true;
        }

        // 하이픈 포함 형식 검증
        if (mobileOnly) {
            if (RegexPatterns.isValidMobile(value)) {
                return true;
            }
        } else {
            if (RegexPatterns.isValidPhone(value)) {
                return true;
            }
        }

        // 하이픈 없는 형식 허용 시
        if (allowWithoutHyphen) {
            return RegexPatterns.PHONE_PLAIN.matcher(value).matches();
        }

        return false;
    }
}
