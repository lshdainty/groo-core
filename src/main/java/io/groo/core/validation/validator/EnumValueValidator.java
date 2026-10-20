package io.groo.core.validation.validator;

import io.groo.core.validation.annotation.EnumValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enum 값 검증 Validator
 * <p>
 * {@link EnumValue} 어노테이션의 검증 로직을 구현합니다.
 * 문자열 값이 지정된 Enum의 유효한 값인지 검증합니다.
 *
 * @author groo
 * @see EnumValue
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Set<String> enumValues;
    private boolean ignoreCase;
    private boolean nullable;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        Class<? extends Enum<?>> enumClass = constraintAnnotation.enumClass();
        this.ignoreCase = constraintAnnotation.ignoreCase();
        this.nullable = constraintAnnotation.nullable();

        // Enum 값들을 Set으로 저장
        this.enumValues = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .map(name -> ignoreCase ? name.toUpperCase() : name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 처리
        if (value == null) {
            return nullable;
        }

        // 빈 문자열 처리
        if (value.isBlank()) {
            return nullable;
        }

        // Enum 값 포함 여부 확인
        String checkValue = ignoreCase ? value.toUpperCase() : value;
        return enumValues.contains(checkValue);
    }
}
