package io.groo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA Auditing 기본 필드
 * <p>
 * 엔티티의 생성/수정 시간과 사용자 정보를 자동으로 관리합니다.
 * 모든 엔티티에서 이 클래스를 상속받아 사용합니다.
 *
 * <h3>기본 필드</h3>
 * <ul>
 *   <li>{@code createAt} (create_at) - 생성 일시</li>
 *   <li>{@code createBy} (create_by) - 생성자 ID</li>
 *   <li>{@code modifyAt} (modify_at) - 수정 일시</li>
 *   <li>{@code modifyBy} (modify_by) - 수정자 ID</li>
 * </ul>
 *
 * <h3>IP 필드 확장</h3>
 * <p>
 * IP 주소 등 추가 필드가 필요한 경우 각 서비스에서 이 클래스를 상속받아 확장합니다.
 * <pre>{@code
 * @MappedSuperclass
 * public abstract class AuditingFieldsWithIp extends AuditingFields {
 *     @Column(length = 45)
 *     private String createdIp;
 *
 *     @Column(length = 45)
 *     private String updatedIp;
 *
 *     @PrePersist
 *     public void prePersist() {
 *         this.createdIp = HttpUtils.getClientIp();
 *         this.updatedIp = this.createdIp;
 *     }
 *
 *     @PreUpdate
 *     public void preUpdate() {
 *         this.updatedIp = HttpUtils.getClientIp();
 *     }
 * }
 * }</pre>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @Entity
 * @Table(name = "users")
 * public class User extends AuditingFields {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *
 *     private String name;
 *     private String email;
 * }
 * }</pre>
 *
 * <h3>설정 요구사항</h3>
 * <p>
 * JPA Auditing 활성화를 위해 다음 설정이 필요합니다.
 * <pre>{@code
 * @Configuration
 * @EnableJpaAuditing
 * public class JpaConfig {
 * }
 * }</pre>
 *
 * <h3>AuditorAware 구현</h3>
 * <p>
 * {@code createdBy}, {@code updatedBy} 필드 자동 설정을 위해
 * {@code AuditorAware<String>} 구현이 필요합니다.
 * <pre>{@code
 * @Component
 * public class LoginUserAuditorAware implements AuditorAware<String> {
 *     @Override
 *     public Optional<String> getCurrentAuditor() {
 *         return Optional.ofNullable(SecurityContextHolder.getContext())
 *             .map(SecurityContext::getAuthentication)
 *             .filter(Authentication::isAuthenticated)
 *             .map(Authentication::getPrincipal)
 *             .filter(AuditorPrincipal.class::isInstance)
 *             .map(AuditorPrincipal.class::cast)
 *             .map(AuditorPrincipal::getUserId);
 *     }
 * }
 * }</pre>
 *
 * @author groo
 * @see io.groo.core.security.AuditorPrincipal
 * @see org.springframework.data.jpa.domain.support.AuditingEntityListener
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditingFields {

    /**
     * 생성 일시
     * <p>
     * 엔티티가 처음 저장될 때 자동으로 설정됩니다.
     * 이후 수정되지 않습니다.
     */
    @CreatedDate
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    /**
     * 생성자 ID
     * <p>
     * 엔티티를 생성한 사용자의 ID입니다.
     * {@code AuditorAware} 구현체에서 현재 사용자 ID를 가져옵니다.
     */
    @CreatedBy
    @Column(name = "create_by", length = 50, updatable = false)
    private String createBy;

    /**
     * 수정 일시
     * <p>
     * 엔티티가 수정될 때마다 자동으로 갱신됩니다.
     */
    @LastModifiedDate
    @Column(name = "modify_at", nullable = false)
    private LocalDateTime modifyAt;

    /**
     * 수정자 ID
     * <p>
     * 엔티티를 마지막으로 수정한 사용자의 ID입니다.
     * {@code AuditorAware} 구현체에서 현재 사용자 ID를 가져옵니다.
     */
    @LastModifiedBy
    @Column(name = "modify_by", length = 50)
    private String modifyBy;
}
