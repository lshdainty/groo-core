package io.groo.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 보안 관련 설정 프로퍼티
 * <p>
 * {@code application.yml}의 {@code security} 프리픽스로 설정합니다.
 *
 * <h3>설정 예시</h3>
 * <pre>{@code
 * security:
 *   ip-blacklist:
 *     enabled: true
 *     file-path: config/ip-blacklist.txt
 *     log-level: WARN
 * }</pre>
 *
 * <h3>IP 블랙리스트 파일 형식</h3>
 * <pre>
 * # 차단할 IP 목록 (한 줄에 하나씩)
 * 192.168.1.100
 * 10.0.0.0/24
 * </pre>
 *
 * @author groo
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * IP 블랙리스트 설정
     */
    private IpBlacklist ipBlacklist = new IpBlacklist();

    @Getter
    @Setter
    public static class IpBlacklist {
        /**
         * IP 블랙리스트 활성화 여부
         */
        private boolean enabled = true;

        /**
         * IP 블랙리스트 파일 경로
         * 파일이 존재하지 않으면 경고만 로그에 남기고 계속 진행
         * 예: "config/ip-blacklist.txt", "/etc/porest/ip-blacklist.txt"
         */
        private String filePath;

        /**
         * 차단 시 로그 레벨 (WARN 또는 ERROR)
         */
        private String logLevel = "WARN";
    }
}
