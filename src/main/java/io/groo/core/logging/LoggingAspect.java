package io.groo.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 로깅 AOP Aspect
 * <p>
 * {@link LogExecutionTime}과 {@link LogMethodCall} 어노테이션을 처리합니다.
 * 메서드 실행 시간 측정 및 호출 로깅을 수행합니다.
 *
 * <h3>활성화 방법</h3>
 * <p>
 * 이 Aspect를 사용하려면 다음 조건이 필요합니다:
 * <ol>
 *   <li>Spring AOP 의존성 (spring-boot-starter-aop)</li>
 *   <li>@EnableAspectJAutoProxy 또는 Spring Boot 자동 설정</li>
 *   <li>이 클래스가 Component Scan 범위에 포함</li>
 * </ol>
 *
 * <h3>사용 시 주의사항</h3>
 * <ul>
 *   <li>같은 클래스 내부 호출은 AOP가 적용되지 않습니다.</li>
 *   <li>private 메서드에는 적용되지 않습니다.</li>
 *   <li>프로덕션에서 과도한 로깅은 성능에 영향을 줄 수 있습니다.</li>
 * </ul>
 *
 * @author groo
 * @see LogExecutionTime
 * @see LogMethodCall
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * @LogExecutionTime 어노테이션이 적용된 메서드의 실행 시간 측정
     */
    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime)
            throws Throwable {

        String methodName = getMethodName(joinPoint);
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // 로그 메시지 생성
            StringBuilder message = new StringBuilder();
            message.append("[").append(methodName).append("] executed in ").append(executionTime).append("ms");

            // 파라미터 포함
            if (logExecutionTime.includeArgs()) {
                message.append(" args=").append(Arrays.toString(joinPoint.getArgs()));
            }

            // 반환값 포함
            if (logExecutionTime.includeResult()) {
                message.append(" result=").append(result);
            }

            // 임계값 초과 시 WARN, 아니면 INFO
            long threshold = logExecutionTime.warnThresholdMs();
            if (threshold > 0 && executionTime > threshold) {
                message.append(" (exceeded ").append(threshold).append("ms threshold)");
                log.warn(message.toString());
            } else {
                log.info(message.toString());
            }

            return result;

        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] failed after {}ms - {}: {}",
                    methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    /**
     * 클래스 레벨 @LogExecutionTime 처리
     */
    @Around("@within(logExecutionTime) && !@annotation(io.groo.core.logging.LogExecutionTime)")
    public Object logExecutionTimeClass(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime)
            throws Throwable {
        return logExecutionTime(joinPoint, logExecutionTime);
    }

    /**
     * @LogMethodCall 어노테이션이 적용된 메서드의 호출 로깅
     */
    @Around("@annotation(logMethodCall)")
    public Object logMethodCall(ProceedingJoinPoint joinPoint, LogMethodCall logMethodCall)
            throws Throwable {

        String methodName = getMethodName(joinPoint);
        long startTime = System.currentTimeMillis();

        // 예외 발생 시에만 로깅하는 경우 시작 로그 생략
        if (!logMethodCall.logOnExceptionOnly()) {
            StringBuilder startMessage = new StringBuilder();
            startMessage.append("[").append(methodName).append("] START");

            if (logMethodCall.logArgs()) {
                startMessage.append(" args=").append(Arrays.toString(joinPoint.getArgs()));
            }

            logByLevel(logMethodCall.level(), startMessage.toString());
        }

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // 예외 발생 시에만 로깅하는 경우 종료 로그도 생략
            if (!logMethodCall.logOnExceptionOnly()) {
                StringBuilder endMessage = new StringBuilder();
                endMessage.append("[").append(methodName).append("] END (").append(executionTime).append("ms)");

                if (logMethodCall.logResult()) {
                    endMessage.append(" result=").append(result);
                }

                logByLevel(logMethodCall.level(), endMessage.toString());
            }

            return result;

        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;

            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("[").append(methodName).append("] EXCEPTION after ")
                    .append(executionTime).append("ms");

            if (logMethodCall.logArgs()) {
                errorMessage.append(" args=").append(Arrays.toString(joinPoint.getArgs()));
            }

            errorMessage.append(" - ").append(e.getClass().getSimpleName())
                    .append(": ").append(e.getMessage());

            log.error(errorMessage.toString());
            throw e;
        }
    }

    /**
     * 클래스 레벨 @LogMethodCall 처리
     */
    @Around("@within(logMethodCall) && !@annotation(io.groo.core.logging.LogMethodCall)")
    public Object logMethodCallClass(ProceedingJoinPoint joinPoint, LogMethodCall logMethodCall)
            throws Throwable {
        return logMethodCall(joinPoint, logMethodCall);
    }

    /**
     * 메서드 이름 추출 (클래스명.메서드명)
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    /**
     * 로그 레벨에 따른 로깅
     */
    private void logByLevel(LogMethodCall.Level level, String message) {
        switch (level) {
            case TRACE -> log.trace(message);
            case DEBUG -> log.debug(message);
            case INFO -> log.info(message);
            case WARN -> log.warn(message);
        }
    }
}
