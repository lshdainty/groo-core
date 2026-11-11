package io.groo.core.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing 의 작성자·수정자({@code create_by}·{@code modify_by})를 정하는 공통 뼈대(3.1.0).
 *
 * <p>세 서비스가 같은 코드를 복사해 쓰던 것을 올렸다. 공통 판단은 여기서 하고, 서비스마다 다른 두 자리만
 * 열어 둔다.
 * <ul>
 *   <li>{@link #anonymousAuditor()} — 로그인 없이 일어난 쓰기(스케줄러·배치·공개 API). 기본은 비움.
 *       담다는 {@code "system"} 으로 남긴다.</li>
 *   <li>{@link #fallbackAuditor(Authentication)} — 인증은 됐는데 주체가 {@link AuditorPrincipal} 이 아닐 때.
 *       기본은 비움.</li>
 * </ul>
 *
 * <p>서비스는 이걸 상속한 {@code @Component("auditorAware")} 를 두고 {@code @EnableJpaAuditing(auditorAwareRef =
 * "auditorAware")} 로 연결한다. core 는 빈을 직접 등록하지 않는다(서비스 3곳이 core 패키지를 통째로 스캔하므로,
 * 여기서 빈을 만들면 서비스 쪽 빈과 충돌한다).
 */
public abstract class AbstractAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return anonymousAuditor();
        }
        if (authentication.getPrincipal() instanceof AuditorPrincipal principal) {
            return Optional.ofNullable(principal.getUserId());
        }
        return fallbackAuditor(authentication);
    }

    /** 로그인 없이 일어난 쓰기의 작성자. 기본은 비움(컬럼이 null 로 남는다). */
    protected Optional<String> anonymousAuditor() {
        return Optional.empty();
    }

    /** 인증된 주체가 {@link AuditorPrincipal} 이 아닐 때의 작성자. 기본은 비움. */
    protected Optional<String> fallbackAuditor(Authentication authentication) {
        return Optional.empty();
    }
}
