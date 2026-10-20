package io.groo.core.audit;

/**
 * 접속기록 저장 포트
 * <p>
 * core 는 "무엇을 언제 기록할지"만 정하고, <strong>어디에 어떻게 저장할지는 각 서비스가 정합니다</strong>.
 * 서비스마다 테이블명·컬럼·보관 정책이 다르고, SSO 처럼 이미 자체 감사 테이블을 쓰는 곳도 있기 때문입니다.
 *
 * <h3>구현 시 지켜야 할 것</h3>
 * <ul>
 *   <li><strong>예외를 밖으로 던지지 마세요.</strong> 기록 실패로 사용자 요청이 깨지면 안 됩니다.
 *       (Aspect 에서도 한 번 더 막지만, 구현체에서 먼저 처리하는 편이 원인 파악에 낫습니다)</li>
 *   <li>본 트랜잭션에 얹히지 않게 하세요. 본 작업이 롤백돼도 "접근했다"는 사실은 남아야 합니다
 *       — {@code REQUIRES_NEW} 또는 비동기 처리를 권장합니다.</li>
 *   <li>보관 기간은 고시 제8조상 <strong>1년 이상</strong>입니다. 5만명 이상의 정보주체 개인정보나
 *       고유식별정보·민감정보를 다루면 2년 이상입니다.</li>
 * </ul>
 *
 * <h3>구현 예시</h3>
 * <pre>{@code
 * @Component
 * @RequiredArgsConstructor
 * public class AccessLogPortImpl implements AuditAccessPort {
 *
 *     private final AccessLogRepository repository;
 *
 *     @Override
 *     @Async
 *     @Transactional(propagation = Propagation.REQUIRES_NEW)
 *     public void record(AccessLogEntry entry) {
 *         try {
 *             repository.save(AccessLog.from(entry));
 *         } catch (Exception e) {
 *             log.warn("접속기록 저장 실패: action={}, target={}", entry.action(), entry.targetType(), e);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author groo
 * @see AccessLogEntry 기록 한 건의 형태
 * @see AuditAccessAspect 이 포트를 호출하는 쪽
 */
public interface AuditAccessPort {

    /**
     * 접속기록 한 건을 저장합니다.
     *
     * @param entry 기록 내용
     */
    void record(AccessLogEntry entry);
}
