package io.groo.core.audit;

import io.groo.core.security.AuditorPrincipal;
import io.groo.core.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link AuditAccess} 가 붙은 메서드의 접속기록을 남기는 Aspect
 * <p>
 * 메서드가 <strong>정상 종료했을 때만</strong> 기록합니다. 예외로 끝난 요청은 개인정보를
 * 처리하지 못한 것이므로 접속기록으로 남길 이유가 없습니다.
 *
 * <h3>동작 조건</h3>
 * <p>
 * {@link AuditAccessPort} 빈이 등록된 서비스에서만 실제로 기록합니다. porest 서비스들은
 * {@code scanBasePackages} 에 {@code io.groo.core} 를 포함하므로 이 Aspect 는 어디서나
 * 생성되지만, 포트가 없으면 아무 일도 하지 않습니다(기동은 정상). 포트 없이 어노테이션만
 * 붙인 경우 기록이 조용히 사라지므로 최초 1회 경고를 남깁니다.
 *
 * <h3>실패 처리</h3>
 * <p>
 * 기록 중 어떤 예외가 나도 밖으로 던지지 않습니다. 감사 기록 때문에 사용자 요청이
 * 실패하면 본말이 전도되기 때문입니다. 대신 warn 으로 남겨 누락을 추적할 수 있게 합니다.
 *
 * @author groo
 * @see AuditAccess 기록 대상 지정
 * @see AuditAccessPort 저장 담당 (서비스별 구현)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAccessAspect {

    /**
     * 저장 포트 — 서비스가 구현하지 않았을 수 있으므로 선택적으로 주입받는다.
     * 필수 의존으로 두면 포트가 없는 서비스에서 기동 자체가 실패한다.
     */
    private final ObjectProvider<AuditAccessPort> auditAccessPortProvider;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /** 포트 미등록 경고를 한 번만 남기기 위한 플래그. */
    private final AtomicBoolean missingPortWarned = new AtomicBoolean(false);

    /**
     * 대상 메서드가 정상 종료하면 접속기록을 남깁니다.
     */
    @AfterReturning("@annotation(auditAccess)")
    public void record(JoinPoint joinPoint, AuditAccess auditAccess) {
        AuditAccessPort auditAccessPort = auditAccessPortProvider.getIfAvailable();
        if (auditAccessPort == null) {
            if (missingPortWarned.compareAndSet(false, true)) {
                log.warn("@AuditAccess 가 사용됐지만 AuditAccessPort 구현이 없어 접속기록이 저장되지 않습니다. "
                        + "이 서비스에 AuditAccessPort 빈을 등록하세요.");
            }
            return;
        }

        try {
            AccessLogEntry entry = new AccessLogEntry(
                    resolveActorId(),
                    auditAccess.action(),
                    auditAccess.targetType(),
                    resolveTargetId(joinPoint, auditAccess.targetId()),
                    StringUtils.hasText(auditAccess.detail()) ? auditAccess.detail() : null,
                    HttpUtils.getClientIp(),
                    LocalDateTime.now()
            );
            auditAccessPort.record(entry);
        } catch (Exception e) {
            // 감사 기록 실패가 사용자 요청을 깨뜨리지 않게 한다
            log.warn("접속기록 남기기 실패: action={}, targetType={}",
                    auditAccess.action(), auditAccess.targetType(), e);
        }
    }

    /** Spring Security 가 미인증 요청에 넣는 익명 주체 이름. */
    private static final String ANONYMOUS_USER = "anonymousUser";

    /**
     * 수행자 계정 추출
     * <p>
     * {@link AuditorPrincipal} 을 구현한 principal 이면 그 값을 쓰고, 아니면
     * {@code Authentication.getName()} 으로 되돌아갑니다. 인증 정보가 없거나 익명이면 null 입니다.
     * <p>
     * 익명을 걸러내는 이유: Spring Security 는 미인증 요청에도 {@code isAuthenticated()==true}
     * 인 익명 토큰을 넣기 때문에, 그대로 두면 계정 칸에 {@code "anonymousUser"} 가 쌓입니다.
     */
    private String resolveActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuditorPrincipal auditorPrincipal) {
            return auditorPrincipal.getUserId();
        }

        if (ANONYMOUS_USER.equals(principal) || ANONYMOUS_USER.equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

    /**
     * SpEL 표현식으로 처리한 정보주체 식별자 추출
     * <p>
     * 표현식이 비었거나 평가에 실패하면 null 을 돌려줍니다 — 식별자를 못 뽑았다고
     * 기록 자체를 버리면 "누가 접근했다"는 사실까지 사라지기 때문입니다.
     */
    private String resolveTargetId(JoinPoint joinPoint, String expression) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();

            StandardEvaluationContext context = new StandardEvaluationContext();

            // 파라미터 이름으로 접근 (#userId) — 컴파일에 -parameters 가 없으면 못 얻을 수 있다
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 위치로도 접근 가능하게 (#p0, #a0) — 파라미터 이름이 없는 환경 대비
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }

            Expression parsed = expressionParser.parseExpression(expression);
            Object value = parsed.getValue(context);
            return value != null ? String.valueOf(value) : null;

        } catch (Exception e) {
            log.debug("접속기록 targetId 평가 실패: expression={}", expression, e);
            return null;
        }
    }
}
