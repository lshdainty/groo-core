package io.groo.core.logging;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그로 나갈 문자열에서 민감값을 가린다.
 *
 * <p><b>이 클래스는 로그 문자열만 다룬다.</b> 입력 문자열은 불변이고 항상 새 문자열을 돌려주므로
 * 요청·응답 본문이나 바이트 배열이 이 코드 때문에 바뀌는 일은 없다. 필터는 반드시
 * {@code ContentCaching*Wrapper} 가 들고 있는 <b>사본</b>을 넘겨야 하고, 결과는 로그에만 써야 한다.
 *
 * <h2>왜 core 에 있나</h2>
 * 같은 마스킹 코드가 desk·sso·hr 세 레포에 복사본으로 있었고 각자 다르게 늙었다.
 * sso 는 응답 마스킹 호출이 아예 없었고 키 목록이 6개뿐이라, 토큰을 가장 많이 다루는 레포가
 * 가장 약한 사본을 쓰고 있었다. 마스킹 규칙은 여기 한 벌만 둔다.
 *
 * <p>필터 자체는 각 레포에 남는다. 필터는 서블릿 스트림·{@code copyBodyToResponse()} 를 다루는
 * 위험한 코드이고 레포마다 제외 경로·등록 방식이 다르다. core 에 {@code @Component} 필터를 넣으면
 * 세 서비스가 {@code scanBasePackages} 로 core 를 통째로 스캔하므로 로컬 사본을 안 지운 서비스는
 * 필터가 두 개 돌아 이중 래핑으로 본문이 깨진다.
 *
 * <h2>세 층으로 막는다</h2>
 * <ol>
 *   <li><b>이름 정규화 일치</b> — 키를 소문자로 낮추고 {@code _ - . 공백} 을 지운 뒤 비교한다.
 *       그래서 {@code apiKey} · {@code api_key} · {@code API-KEY} 가 전부 같은 키다.
 *       리네임으로 마스킹이 조용히 풀렸던 사고(desk #253)의 재발을 줄인다.
 *       정규화 <b>후 완전일치</b>라 {@code error_code} 는 {@code code} 에 걸리지 않는다.</li>
 *   <li><b>이름 + 값 모양 AND</b> — 같은 이름이 두 가지를 뜻하는 키를 값으로 가른다.
 *       {@link #SHAPE_SCOPED_KEYS} 참고.</li>
 *   <li><b>이름 무관 값 모양</b> — JWT 와 {@code Bearer <token>}. 이름이 무엇이든 잡는 최종 방어선이라
 *       DTO 필드명을 새로 지어도 뚫리지 않는다.</li>
 * </ol>
 *
 * <h2>정규식 규칙 — 여기서 틀리면 응답이 빈 채로 나간다</h2>
 * 이 파일의 모든 수량자는 <b>소유(possessive) 수량자</b>다. 예전 판의
 * {@code (?:\\.|[^"\\])*} 는 값 2,000자에서 {@code StackOverflowError} 를 냈고,
 * {@code Error} 는 필터의 {@code catch (Exception)} 에 안 잡혀 {@code copyBodyToResponse()} 를
 * 건너뛰게 만들었다 — 클라이언트가 빈 본문을 받는다. <b>중첩 수량자를 새로 넣지 마라.</b>
 * 패턴은 전부 {@code static final} 이고 키 목록이 정규식에 들어가지 않으므로
 * 서비스별 키를 추가해도 정규식은 다시 컴파일되지 않는다.
 *
 * <p>{@link #apply(String)} 는 어떤 경우에도 예외를 던지지 않는다. 내부에서 {@code Throwable} 을
 * 잡고 {@link #MASK} 를 돌려준다 — 로깅은 부가 기능이라 실패하면 로그를 포기하고 요청은 통과시킨다.
 */
public final class SensitiveDataMasker {

    /** 가려진 값 자리에 들어가는 문자열. */
    public static final String MASK = "***";

    /**
     * 값이 무엇이든 무조건 가리는 키. 표기는 정규화되므로 스네이크·카멜·케밥·대소문자를 구분하지 않는다.
     *
     * <p><b>여기 없는 것과 그 이유</b>(dev 로그 실측 기준):
     * <ul>
     *   <li>{@code clientId}/{@code client_id} — SSO 에서는 {@code "desk"}·{@code "hr"} 라는
     *       공개 식별자다(OAuth2 스펙상 비밀이 아니다). 가리면 "누가 불렀나"가 로그에서 사라진다.
     *       desk 의 토스 {@code clientId} 는 민감하므로 desk 필터가 추가 키로 넣는다
     *       — {@link #withExtraKeys(String...)}.</li>
     *   <li>{@code state} — CSRF nonce. 가리면 OAuth 흐름을 못 따라간다.</li>
     *   <li>{@code n} · {@code kid} · {@code codeChallenge} — JWKS 공개키 모듈러스·키 ID·PKCE
     *       challenge. 값이 길고 base64url 이라 민감해 보이지만 <b>설계상 공개</b>다.
     *       실측 164건이 전부 오탐이 된다.</li>
     *   <li>이메일·전화번호 — 로그인·조회 진단의 핵심 식별자라 기본으로 가리면 로그가 못 쓰게 된다.
     *       PII 정책으로 가려야 하면 {@link #CONTACT_PII_KEYS} 를 추가 키로 넘긴다.</li>
     * </ul>
     */
    public static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
            // 비밀번호 — 정규화 후 완전일치라 변경 필드는 각각 적어야 한다
            "password", "passwd", "pwd", "user_pw",
            "current_password", "new_password", "confirm_password", "new_password_confirm",
            // 시크릿/키 계열. 증권사 원표기(나무 appkey/appsecretkey)까지 포함한다
            "secret", "secret_key", "client_secret", "api_secret", "app_secret", "app_secret_key",
            "api_key", "app_key", "private_key", "credential", "credentials",
            // 토큰 계열. "tokenType" 은 정규화하면 tokentype 이라 "token" 에 안 걸린다(의도한 동작)
            "token", "access_token", "refresh_token", "id_token", "sso_access_token",
            "authorization", "proxy_authorization", "cookie", "set_cookie",
            "ticket", "session_id", "jsessionid",
            // 계좌·고객 식별번호. 나무는 계좌목록이 acct_no, 잔고요청이 act_no 로 표기가 갈린다
            "account_no", "acct_no", "act_no", "cust_no", "rnm_cfm_no", "ssn"
    );

    /**
     * 이름만으로는 못 가르고 <b>값이 {@link #HIGH_ENTROPY_VALUE} 모양일 때만</b> 가리는 키.
     *
     * <p>{@code code} 는 dev 로그 76,908건 중 4가지를 뜻한다 — {@code ApiResponse} 봉투
     * 상태코드({@code COMMON_200}, 약 75,000건) · 도메인 에러코드({@code AUTH_002}) ·
     * HR 업무시스템 코드({@code TABLEAU}) · <b>OAuth2 인가코드(43자 base64url, 133건)</b>.
     * 이름만 보고 가리면 멀쩡한 로그 76,775건이 {@code ***} 가 되고 정밀도가 0.17% 가 된다.
     * 43자 이상 base64url 조건을 걸면 실측상 인가코드 133건을 전부 잡고 오탐이 0이다
     * — 봉투코드는 10자, 에러코드는 7~12자 대문자, 업무코드는 대문자 단어라 43자에 닿는 게 없다.
     */
    public static final Set<String> SHAPE_SCOPED_KEYS = Set.of(
            "code", "code_verifier"
    );

    /**
     * 기본에는 없는 연락처 PII 키. 정책상 가려야 하는 서비스가
     * {@code withExtraKeys(CONTACT_PII_KEYS)} 로 직접 켠다.
     *
     * <p>기본에 안 넣은 이유는 이 값들이 로그인·사용자 조회 진단에서 "누구 건인지"를 알려주는
     * 유일한 단서이기 때문이다. 가리는 순간 그 로그는 못 쓰게 된다 — 트레이드오프가 있는 결정이라
     * core 가 대신 정하지 않는다.
     */
    public static final Set<String> CONTACT_PII_KEYS = Set.of(
            "email", "user_email", "phone", "phone_no", "phone_number", "mobile", "tel", "hp_no"
    );

    /** 추가 키 없이 기본 규칙만 적용하는 공용 인스턴스. */
    public static final SensitiveDataMasker DEFAULT = new SensitiveDataMasker(Set.of());

    /**
     * JSON 의 {@code "key": value} 한 쌍. 값은 따옴표 문자열(그룹 2) 또는 숫자(그룹 3)다.
     *
     * <p>값 부분 {@code [^"\\]*+(?:\\.[^"\\]*+)*+} 은 이스케이프({@code \"})를 인식하면서도
     * 소유 수량자라 백트래킹·재귀가 없다. 종료 따옴표까지 삼키므로 {@code "a\"b"} 에서
     * {@code b} 조각이 평문으로 남지 않는다.
     *
     * <p>키를 정규식에 넣지 않고 <b>아무 키나 잡아 자바 코드로 판정</b>한다. 그래서
     * 서비스별 키를 추가해도 패턴은 그대로고, 이름+값 AND 규칙을 표현할 수 있다.
     */
    private static final Pattern JSON_FIELD = Pattern.compile(
            "\"([A-Za-z0-9_.\\-]{1,64})\"\\s*+:\\s*+"
                    + "(?:\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\"|(-?[0-9][0-9.eE+\\-]{0,63}))");

    /** 쿼리스트링·{@code application/x-www-form-urlencoded} 본문의 {@code key=value}. */
    private static final Pattern QUERY_PARAM = Pattern.compile(
            "(^|[?&])([A-Za-z0-9_.\\-]{1,64})=([^&]*+)");

    /**
     * JWT. {@code eyJ} 는 {@code {"} 의 base64 이고 뒤에 점으로 갈린 파트가 오는 모양은 JWT 말고 없다.
     * dev 로그 전수(desk 640만줄 + sso·hr)에서 이 모양에 걸린 388건의 키는
     * {@code access_token}·{@code refresh_token}·{@code accessToken} 세 개뿐이었다 — <b>오탐 0</b>.
     *
     * <p>파트를 명시적으로 3개 쓰고 {@code eyJ} 로 앵커했다. 흔히 손이 가는
     * {@code (?:[A-Za-z0-9_-]+\.)+[A-Za-z0-9_-]+} 형태는 중첩 수량자라 26자 입력에서 이미 폭주한다.
     */
    private static final Pattern JWT_VALUE = Pattern.compile(
            "eyJ[A-Za-z0-9_-]{4,}+\\.[A-Za-z0-9_-]{4,}+\\.[A-Za-z0-9_-]*+");

    /**
     * {@code Bearer <token>}. 토큰부를 20자 이상으로 잡아 {@code "Bearer Authentication"} 같은
     * 산문이 걸리지 않게 했다({@code tokenType:"Bearer"} 는 뒤에 공백+토큰이 없어 애초에 안 걸린다).
     *
     * <p>{@code Basic <base64>} 는 넣지 않았다 — {@code Basic Authentication} 이라는 흔한 문구가
     * 그대로 걸려 오탐이 난다. 지금 세 필터 모두 Authorization 헤더를 로깅하지 않으므로 실익도 없다.
     */
    private static final Pattern BEARER_VALUE = Pattern.compile(
            "(?i)\\b(Bearer)([ \\t]{1,4}+)([A-Za-z0-9._~+/=-]{20,}+)");

    /** 이름+값 AND 규칙이 쓰는 값 모양. 43자는 OAuth2 인가코드·PKCE verifier 의 길이다. */
    private static final Pattern HIGH_ENTROPY_VALUE = Pattern.compile("[A-Za-z0-9_-]{43,}");

    private final Set<String> alwaysKeys;
    private final Set<String> shapeScopedKeys;

    private SensitiveDataMasker(Collection<String> extraKeys) {
        Set<String> always = new HashSet<>(normalizeAll(DEFAULT_SENSITIVE_KEYS));
        always.addAll(normalizeAll(extraKeys));
        this.alwaysKeys = Set.copyOf(always);
        // 추가 키로 들어온 이름은 무조건 마스킹이 우선한다(서비스가 명시적으로 켠 것이므로)
        Set<String> scoped = new HashSet<>(normalizeAll(SHAPE_SCOPED_KEYS));
        scoped.removeAll(this.alwaysKeys);
        this.shapeScopedKeys = Set.copyOf(scoped);
    }

    /**
     * 서비스 고유 키를 추가한 마스커를 만든다. 기본 키는 그대로 유지되므로
     * <b>추가를 안 해도 안전</b>하고, 추가는 더 조이기만 한다.
     *
     * <p>필터에서 {@code static final} 로 한 번 만들어 재사용해라 — 정규식은 이 클래스에
     * 정적으로 하나뿐이라 인스턴스를 만들어도 다시 컴파일되지 않는다.
     */
    public static SensitiveDataMasker withExtraKeys(String... extraKeys) {
        return withExtraKeys(Arrays.asList(extraKeys));
    }

    /** @see #withExtraKeys(String...) */
    public static SensitiveDataMasker withExtraKeys(Collection<String> extraKeys) {
        if (extraKeys == null || extraKeys.isEmpty()) {
            return DEFAULT;
        }
        return new SensitiveDataMasker(extraKeys);
    }

    /** 기본 규칙만으로 마스킹한다. {@code DEFAULT.apply(text)} 와 같다. */
    public static String mask(String text) {
        return DEFAULT.apply(text);
    }

    /**
     * 로그 문자열의 민감값을 가린 <b>새 문자열</b>을 돌려준다. 입력은 변경되지 않는다.
     *
     * <p>{@code null} · 빈 문자열은 그대로 돌려준다. 그 밖에는 어떤 입력에도 예외를 던지지 않으며,
     * 내부에서 문제가 생기면 원문 대신 {@link #MASK} 를 돌려준다(원문 유출보다 로그 손실이 낫다).
     * 멱등이다 — 이미 마스킹된 문자열을 다시 넣어도 결과가 같다.
     */
    public String apply(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            String masked = maskJson(text);
            masked = maskQuery(masked);
            masked = JWT_VALUE.matcher(masked).replaceAll(MASK);
            masked = BEARER_VALUE.matcher(masked).replaceAll("$1$2" + MASK);
            return masked;
        } catch (Throwable t) {
            // 마스킹이 실패했는데 원문을 흘리면 그게 유출이다. 로그를 버린다.
            return MASK;
        }
    }

    /** 키가 이 마스커의 규칙에 걸리는지. 값 조건이 붙은 키는 값도 함께 본다. */
    private boolean isSensitive(String key, String value) {
        String normalized = normalize(key);
        if (alwaysKeys.contains(normalized)) {
            return true;
        }
        return shapeScopedKeys.contains(normalized)
                && value != null
                && HIGH_ENTROPY_VALUE.matcher(value).matches();
    }

    private String maskJson(String text) {
        Matcher m = JSON_FIELD.matcher(text);
        if (!m.find()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        do {
            String key = m.group(1);
            String value = m.group(2) != null ? m.group(2) : m.group(3);
            String replacement = isSensitive(key, value)
                    ? "\"" + key + "\":\"" + MASK + "\""
                    : m.group();
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private String maskQuery(String text) {
        Matcher m = QUERY_PARAM.matcher(text);
        if (!m.find()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        do {
            String key = m.group(2);
            String value = m.group(3);
            String replacement = isSensitive(key, value)
                    ? m.group(1) + key + "=" + MASK
                    : m.group();
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 키 표기 정규화 — 소문자로 낮추고 {@code _ - . 공백} 을 지운다.
     * {@code apiKey} · {@code api_key} · {@code API-KEY} 가 전부 {@code apikey} 가 된다.
     */
    private static String normalize(String key) {
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '_' || c == '-' || c == '.' || c == ' ') {
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static Set<String> normalizeAll(Collection<String> keys) {
        Set<String> normalized = new LinkedHashSet<>();
        if (keys == null) {
            return normalized;
        }
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                normalized.add(normalize(key));
            }
        }
        return normalized;
    }
}
