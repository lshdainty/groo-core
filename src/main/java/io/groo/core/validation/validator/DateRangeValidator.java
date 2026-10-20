package io.groo.core.validation.validator;

import io.groo.core.validation.annotation.DateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Date;

/**
 * 날짜 범위 검증 Validator
 * <p>
 * {@link DateRange} 어노테이션의 검증 로직을 구현합니다.
 * 시작일이 종료일보다 이전이거나 같은지 검증합니다.
 *
 * @author groo
 * @see DateRange
 */
@Slf4j
public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

    private String startField;
    private String endField;
    private boolean allowEqual;

    @Override
    public void initialize(DateRange constraintAnnotation) {
        this.startField = constraintAnnotation.startField();
        this.endField = constraintAnnotation.endField();
        this.allowEqual = constraintAnnotation.allowEqual();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            Object startValue = getFieldValue(value, startField);
            Object endValue = getFieldValue(value, endField);

            // 둘 중 하나라도 null이면 유효한 것으로 처리
            if (startValue == null || endValue == null) {
                return true;
            }

            return compareValues(startValue, endValue);

        } catch (Exception e) {
            log.warn("DateRange validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 리플렉션으로 필드 값 가져오기
     */
    private Object getFieldValue(Object object, String fieldName) throws Exception {
        Class<?> clazz = object.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException("Field not found: " + fieldName);
    }

    /**
     * 날짜 값 비교
     */
    private boolean compareValues(Object start, Object end) {
        // LocalDate
        if (start instanceof LocalDate startDate && end instanceof LocalDate endDate) {
            if (allowEqual) {
                return !startDate.isAfter(endDate);
            } else {
                return startDate.isBefore(endDate);
            }
        }

        // LocalDateTime
        if (start instanceof LocalDateTime startDateTime && end instanceof LocalDateTime endDateTime) {
            if (allowEqual) {
                return !startDateTime.isAfter(endDateTime);
            } else {
                return startDateTime.isBefore(endDateTime);
            }
        }

        // java.util.Date
        if (start instanceof Date startDate && end instanceof Date endDate) {
            if (allowEqual) {
                return !startDate.after(endDate);
            } else {
                return startDate.before(endDate);
            }
        }

        // Comparable 인터페이스 지원
        if (start instanceof Comparable && start.getClass().equals(end.getClass())) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparableStart = (Comparable<Object>) start;
            int result = comparableStart.compareTo(end);

            if (allowEqual) {
                return result <= 0;
            } else {
                return result < 0;
            }
        }

        log.warn("Unsupported date type: {} and {}", start.getClass(), end.getClass());
        return false;
    }
}
