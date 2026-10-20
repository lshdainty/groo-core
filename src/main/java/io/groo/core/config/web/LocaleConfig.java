package io.groo.core.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.List;
import java.util.Locale;

/**
 * 다국어 지원을 위한 Locale 설정
 * <p>
 * Accept-Language 헤더 기반으로 Locale을 결정하며, URL 파라미터로 변경할 수 있습니다.
 *
 * <h3>Locale 결정 우선순위</h3>
 * <ol>
 *   <li>URL 파라미터 {@code lang} (예: {@code ?lang=en})</li>
 *   <li>HTTP Accept-Language 헤더</li>
 *   <li>기본값: 한국어 (ko)</li>
 * </ol>
 *
 * <h3>지원 언어</h3>
 * <ul>
 *   <li>한국어 (ko) - 기본</li>
 *   <li>영어 (en)</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // URL 파라미터로 언어 변경
 * GET /api/v1/users?lang=en
 *
 * // Accept-Language 헤더로 언어 설정
 * Accept-Language: ko-KR,ko;q=0.9,en;q=0.8
 * }</pre>
 *
 * @author groo
 * @see LocaleResolver
 * @see LocaleChangeInterceptor
 */
@Configuration
public class LocaleConfig {

    /**
     * LocaleResolver 설정
     * Accept-Language 헤더를 기반으로 Locale을 결정
     * 기본 Locale: 한국어 (ko)
     * 지원 Locale: 한국어 (ko), 영어 (en)
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setSupportedLocales(List.of(Locale.KOREAN, Locale.ENGLISH));
        return resolver;
    }

    /**
     * LocaleChangeInterceptor 설정
     * URL 파라미터 'lang'으로 Locale 변경 가능
     * 예: /api/v1/users?lang=en
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
}
