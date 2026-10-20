package io.groo.core.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 접속기록 엔티티 공통 필드
 * <p>
 * 「개인정보의 안전성 확보조치 기준」 제8조 제1항의 다섯 항목을 컬럼으로 고정합니다.
 * 테이블명·인덱스·추가 컬럼은 서비스마다 다르므로 실제 {@code @Entity} 는 각 서비스에서
 * 이 클래스를 상속해 정의합니다.
 *
 * <h3>감사 필드를 상속하지 않는 이유</h3>
 * <p>
 * {@code AuditingFields}(create_by/modify_by 등)를 상속하지 않습니다. 이 테이블 자체가
 * 감사 기록이라 수정 이력이 있을 수 없고, 오히려 <strong>수정·삭제가 불가능해야</strong> 합니다.
 *
 * <h3>위·변조 방지</h3>
 * <p>
 * 고시 제8조 제2항은 접속기록을 안전하게 보관하도록 요구합니다. 애플리케이션 DB 계정에서
 * 이 테이블의 {@code UPDATE}·{@code DELETE} 권한을 회수하고 {@code INSERT}·{@code SELECT}
 * 만 남기는 것을 권장합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @Entity
 * @Table(name = "access_logs")
 * @Getter
 * @NoArgsConstructor(access = AccessLevel.PROTECTED)
 * public class AccessLog extends AbstractAccessLog {
 *
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     @Column(name = "row_id")
 *     private Long rowId;
 *
 *     public static AccessLog from(AccessLogEntry entry) {
 *         AccessLog log = new AccessLog();
 *         log.apply(entry);
 *         return log;
 *     }
 * }
 * }</pre>
 *
 * @author groo
 * @see AccessLogEntry
 */
@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractAccessLog {

    /** 수행자(개인정보취급자) 계정 — 고시 제8조 "계정" */
    @Column(name = "actor_id", length = 20)
    private String actorId;

    /** 수행업무 — 고시 제8조 "수행업무" */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    /** 대상 유형 (USER, DUES, …) */
    @Column(name = "target_type", length = 20)
    private String targetType;

    /** 처리한 정보주체 식별자 — 고시 제8조 "처리한 정보주체 정보" */
    @Column(name = "target_id", length = 50)
    private String targetId;

    /** 맥락 메모 (개인정보 금지) */
    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    /** 접속지 정보 — 고시 제8조 "접속지 정보" */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** 접속일시 — 고시 제8조 "접속일시". [UTC] 시스템 기록 시각 */
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    /**
     * 기록 값을 채웁니다. 상속받은 엔티티의 팩토리 메소드에서 호출하세요.
     *
     * @param entry 기록 내용
     */
    protected void apply(AccessLogEntry entry) {
        this.actorId = entry.actorId();
        this.action = entry.action() != null ? entry.action().name() : null;
        this.targetType = entry.targetType();
        this.targetId = entry.targetId();
        this.detail = entry.detail();
        this.ipAddress = entry.ipAddress();
        this.createAt = entry.occurredAt() != null ? entry.occurredAt() : LocalDateTime.now();
    }
}
