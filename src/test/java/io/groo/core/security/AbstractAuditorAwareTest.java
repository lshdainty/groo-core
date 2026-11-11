package io.groo.core.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractAuditorAwareTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void login(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES));
    }

    @Test
    void 기본_구현_AuditorPrincipal_이면_그_ID() {
        login((AuditorPrincipal) () -> "u1");

        assertThat(new AbstractAuditorAware() {}.getCurrentAuditor()).contains("u1");
    }

    @Test
    void 기본_구현_로그인_없음과_낯선_주체는_비움() {
        AbstractAuditorAware sut = new AbstractAuditorAware() {};
        assertThat(sut.getCurrentAuditor()).isEmpty();

        login("plain-string-principal");
        assertThat(sut.getCurrentAuditor()).isEmpty();
    }

    @Test
    void 서비스가_연_자리를_덮으면_그_값을_쓴다() {
        AbstractAuditorAware damdaLike = new AbstractAuditorAware() {
            @Override
            protected Optional<String> anonymousAuditor() {
                return Optional.of("system");
            }

            @Override
            protected Optional<String> fallbackAuditor(Authentication authentication) {
                return Optional.of(authentication.getName());
            }
        };
        assertThat(damdaLike.getCurrentAuditor()).contains("system");

        login("plain-string-principal");
        assertThat(damdaLike.getCurrentAuditor()).contains("plain-string-principal");
    }
}
