package io.groo.core.security;

/**
 * JPA Auditing을 위한 Principal 인터페이스
 * <p>
 * Spring Security의 Principal 구현체가 이 인터페이스를 구현하면
 * AuditorAware에서 현재 사용자 ID를 가져올 수 있습니다.
 *
 * <h3>구현 예시</h3>
 * <pre>{@code
 * public class LoginUser implements UserDetails, AuditorPrincipal {
 *     private final User user;
 *
 *     @Override
 *     public String getUserId() {
 *         return user.getId().toString();
 *     }
 *
 *     // ... UserDetails 구현
 * }
 * }</pre>
 *
 * <h3>AuditorAware 구현 예시</h3>
 * <pre>{@code
 * @Component
 * public class LoginUserAuditorAware implements AuditorAware<String> {
 *
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
 * @see org.springframework.data.domain.AuditorAware
 */
public interface AuditorPrincipal {
    /**
     * 사용자 ID를 반환합니다.
     * @return 사용자 ID (문자열)
     */
    String getUserId();
}
