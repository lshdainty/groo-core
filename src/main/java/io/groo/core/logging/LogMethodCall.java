package io.groo.core.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 호출 로깅 어노테이션
 * <p>
 * 메서드의 진입과 종료를 로깅합니다.
 * 디버깅 및 호출 흐름 추적에 사용합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @Service
 * public class OrderService {
 *
 *     @LogMethodCall
 *     public OrderDto createOrder(CreateOrderRequest request) {
 *         // 메서드 진입/종료 로깅
 *         return processOrder(request);
 *     }
 *
 *     // 파라미터와 반환값 포함
 *     @LogMethodCall(logArgs = true, logResult = true)
 *     public OrderDto findById(Long id) {
 *         return orderRepository.findById(id)
 *             .map(OrderDto::from)
 *             .orElseThrow();
 *     }
 *
 *     // 예외 발생 시에만 로깅
 *     @LogMethodCall(logOnExceptionOnly = true)
 *     public void deleteOrder(Long id) {
 *         orderRepository.deleteById(id);
 *     }
 * }
 * }</pre>
 *
 * <h3>로그 출력 예시</h3>
 * <pre>
 * DEBUG - [OrderService.createOrder] START
 * DEBUG - [OrderService.createOrder] END (45ms)
 *
 * DEBUG - [OrderService.findById] START args=[123]
 * DEBUG - [OrderService.findById] END (32ms) result=OrderDto(id=123, ...)
 *
 * ERROR - [OrderService.deleteOrder] EXCEPTION: EntityNotFoundException
 * </pre>
 *
 * @author groo
 * @see LoggingAspect
 * @see LogExecutionTime
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMethodCall {

    /**
     * 파라미터 로깅 여부
     * <p>
     * true로 설정하면 메서드 파라미터를 로그에 출력합니다.
     */
    boolean logArgs() default false;

    /**
     * 반환값 로깅 여부
     * <p>
     * true로 설정하면 메서드 반환값을 로그에 출력합니다.
     */
    boolean logResult() default false;

    /**
     * 예외 발생 시에만 로깅
     * <p>
     * true로 설정하면 정상 실행 시에는 로그를 남기지 않고,
     * 예외가 발생한 경우에만 로그를 남깁니다.
     */
    boolean logOnExceptionOnly() default false;

    /**
     * 로그 레벨
     * <p>
     * 기본값은 DEBUG
     */
    Level level() default Level.DEBUG;

    /**
     * 로그 레벨 enum
     */
    enum Level {
        TRACE, DEBUG, INFO, WARN
    }
}
