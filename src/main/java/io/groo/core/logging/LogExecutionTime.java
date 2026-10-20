package io.groo.core.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 시간 로깅 어노테이션
 * <p>
 * 메서드의 실행 시간을 측정하여 로깅합니다.
 * 성능 모니터링 및 병목 지점 파악에 사용합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @Service
 * public class UserService {
 *
 *     @LogExecutionTime
 *     public UserDto findById(Long id) {
 *         // 실행 시간이 로깅됨
 *         return userRepository.findById(id)
 *             .map(UserDto::from)
 *             .orElseThrow();
 *     }
 *
 *     // 경고 임계값 지정
 *     @LogExecutionTime(warnThresholdMs = 500)
 *     public List<UserDto> findAll() {
 *         // 500ms 초과 시 WARN 레벨로 로깅
 *         return userRepository.findAll().stream()
 *             .map(UserDto::from)
 *             .toList();
 *     }
 *
 *     // 클래스 레벨에 적용하면 모든 public 메서드에 적용
 * }
 * }</pre>
 *
 * <h3>로그 출력 예시</h3>
 * <pre>
 * INFO  - [UserService.findById] executed in 45ms
 * WARN  - [UserService.findAll] executed in 523ms (exceeded 500ms threshold)
 * </pre>
 *
 * <h3>사용 시 주의사항</h3>
 * <ul>
 *   <li>프로덕션 환경에서 과도한 사용은 성능에 영향을 줄 수 있습니다.</li>
 *   <li>AOP 프록시를 통해 동작하므로 같은 클래스 내부 호출은 적용되지 않습니다.</li>
 *   <li>private 메서드에는 적용되지 않습니다.</li>
 * </ul>
 *
 * @author groo
 * @see LoggingAspect
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {

    /**
     * 경고 임계값 (밀리초)
     * <p>
     * 실행 시간이 이 값을 초과하면 WARN 레벨로 로깅합니다.
     * 0 이하이면 임계값 검사를 하지 않습니다.
     * <p>
     * 기본값: 1000ms (1초)
     */
    long warnThresholdMs() default 1000;

    /**
     * 로그 메시지에 파라미터 포함 여부
     * <p>
     * true로 설정하면 메서드 파라미터도 로그에 출력합니다.
     * 민감한 정보가 파라미터에 포함된 경우 false로 설정하세요.
     */
    boolean includeArgs() default false;

    /**
     * 로그 메시지에 반환값 포함 여부
     * <p>
     * true로 설정하면 메서드 반환값도 로그에 출력합니다.
     * 대용량 반환값이나 민감한 정보가 포함된 경우 false로 설정하세요.
     */
    boolean includeResult() default false;
}
