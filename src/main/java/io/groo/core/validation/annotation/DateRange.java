package io.groo.core.validation.annotation;

import io.groo.core.validation.validator.DateRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 날짜 범위 검증 어노테이션
 * <p>
 * 시작일이 종료일보다 이전이거나 같은지 검증합니다.
 * 클래스 레벨에 적용하여 두 날짜 필드를 비교합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @DateRange(startField = "startDate", endField = "endDate",
 *            message = "시작일은 종료일보다 이전이어야 합니다.")
 * public class SearchRequest {
 *     private LocalDate startDate;
 *     private LocalDate endDate;
 * }
 *
 * // LocalDateTime도 지원
 * @DateRange(startField = "startDateTime", endField = "endDateTime")
 * public class ReservationRequest {
 *     private LocalDateTime startDateTime;
 *     private LocalDateTime endDateTime;
 * }
 * }</pre>
 *
 * <h3>지원 타입</h3>
 * <ul>
 *   <li>{@link java.time.LocalDate}</li>
 *   <li>{@link java.time.LocalDateTime}</li>
 *   <li>{@link java.util.Date}</li>
 * </ul>
 *
 * @author groo
 * @see DateRangeValidator
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateRange {

    /**
     * 검증 실패 시 메시지
     */
    String message() default "시작일은 종료일보다 이전이어야 합니다.";

    /**
     * 검증 그룹
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 시작일 필드명
     */
    String startField();

    /**
     * 종료일 필드명
     */
    String endField();

    /**
     * 시작일과 종료일이 같은 것을 허용할지 여부
     * <p>
     * 기본값은 true (같은 날짜 허용)
     */
    boolean allowEqual() default true;

    /**
     * 여러 DateRange 어노테이션 적용을 위한 컨테이너
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        DateRange[] value();
    }
}
