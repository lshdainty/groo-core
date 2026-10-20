package io.groo.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 비밀번호 인코더 설정
 * <p>
 * BCrypt 알고리즘을 사용하는 비밀번호 인코더를 빈으로 등록합니다.
 * SecurityConfig와 분리하여 순환 참조를 방지합니다.
 *
 * <h3>분리 이유</h3>
 * <p>
 * UserDetailsService 구현체에서 PasswordEncoder를 주입받고,
 * SecurityConfig에서 UserDetailsService를 주입받는 경우 순환 참조가 발생합니다.
 * 별도 Configuration으로 분리하여 이 문제를 해결합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class UserService {
 *     private final BCryptPasswordEncoder passwordEncoder;
 *
 *     public void createUser(String password) {
 *         String encoded = passwordEncoder.encode(password);
 *         // ...
 *     }
 *
 *     public boolean checkPassword(String raw, String encoded) {
 *         return passwordEncoder.matches(raw, encoded);
 *     }
 * }
 * }</pre>
 *
 * @author groo
 * @see BCryptPasswordEncoder
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
