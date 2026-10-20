package io.groo.core.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * 마스킹 규칙 고정.
 *
 * <p>이 테스트가 지키는 것은 두 가지다 — <b>민감값이 평문으로 남지 않는다</b>,
 * 그리고 <b>멀쩡한 로그가 망가지지 않는다</b>. 두 번째가 더 자주 깨지고 더 늦게 발견된다
 * (ex. {@code code} 를 무조건 가리면 응답 상태코드 7만여 건이 통째로 {@code ***} 가 된다).
 */
class SensitiveDataMaskerTest {

    private final SensitiveDataMasker sut = SensitiveDataMasker.DEFAULT;

    @Nested
    @DisplayName("① 이름 기반 — JSON · 중첩 · 배열 · 쿼리스트링")
    class NameBased {

        @Test
        @DisplayName("비밀번호와 변경 필드가 마스킹된다")
        void masks_passwords() {
            String body = "{\"id\":\"alice\",\"password\":\"p@ssw0rd!\",\"user_pw\":\"secret123\","
                    + "\"currentPassword\":\"1q2w3e$R\",\"newPassword\":\"qwer!@#$\","
                    + "\"confirmPassword\":\"qwer!@#$\",\"newPasswordConfirm\":\"qwer!@#$\"}";

            String masked = SensitiveDataMasker.mask(body);

            assertThat(masked)
                    .doesNotContain("p@ssw0rd!", "secret123", "1q2w3e$R", "qwer!@#$")
                    .contains("\"password\":\"***\"", "\"user_pw\":\"***\"",
                            "\"currentPassword\":\"***\"", "\"newPasswordConfirm\":\"***\"")
                    .contains("\"id\":\"alice\"");
        }

        @Test
        @DisplayName("증권사 크리덴셜(apiKey·apiSecret·appkey·appsecretkey)이 마스킹된다 — 리네임으로 뚫렸던 자리")
        void masks_broker_credentials() {
            String body = "{\"apiKey\":\"PS8fk2Lm9QwErTyUiOp\",\"apiSecret\":\"Zx9kCvBnMqWeRtYuIoP1234\","
                    + "\"appkey\":\"NH-APP-KEY-0001\",\"appsecretkey\":\"NH-APP-SECRET-0001\","
                    + "\"api_key\":\"AK-1\",\"api_secret\":\"AS-1\"}";

            String masked = SensitiveDataMasker.mask(body);

            assertThat(masked).doesNotContain("PS8fk2Lm9QwErTyUiOp", "Zx9kCvBnMqWeRtYuIoP1234",
                    "NH-APP-KEY-0001", "NH-APP-SECRET-0001", "AK-1", "AS-1");
        }

        @Test
        @DisplayName("OAuth 토큰류와 client_secret 이 마스킹된다 — sso 유출 실측의 핵심 키")
        void masks_oauth_tokens() {
            String body = "{\"access_token\":\"abc.def.ghi\",\"refreshToken\":\"rrr-111\","
                    + "\"client_secret\":\"cs-2222\",\"ssoAccessToken\":\"sso-3333\","
                    + "\"ticket\":\"tk-4444\",\"token\":\"t-5555\"}";

            String masked = SensitiveDataMasker.mask(body);

            assertThat(masked).doesNotContain("abc.def.ghi", "rrr-111", "cs-2222",
                    "sso-3333", "tk-4444", "t-5555");
        }

        @Test
        @DisplayName("계좌·고객 식별번호가 마스킹된다 (숫자 값 포함)")
        void masks_account_numbers() {
            String body = "{\"accountNo\":\"12345678901\",\"acct_no\":\"999-88-7777\","
                    + "\"act_no\":12345678,\"cust_no\":\"C0001\",\"rnm_cfm_no\":\"9001011234567\"}";

            String masked = SensitiveDataMasker.mask(body);

            assertThat(masked).doesNotContain("12345678901", "999-88-7777", "12345678",
                    "C0001", "9001011234567");
            // 숫자 값도 가려진다 — 예전 판은 따옴표 값만 봤다
            assertThat(masked).contains("\"act_no\":\"***\"");
        }

        @Test
        @DisplayName("중첩 객체와 배열 안에서도 마스킹된다")
        void masks_nested_and_array() {
            String body = "{\"data\":{\"user\":{\"password\":\"deep\"},"
                    + "\"list\":[{\"accessToken\":\"in-array-1\"},{\"accessToken\":\"in-array-2\"}]},"
                    + "\"count\":2}";

            String masked = SensitiveDataMasker.mask(body);

            assertThat(masked).doesNotContain("deep", "in-array-1", "in-array-2");
            assertThat(masked).contains("\"count\":2");
        }

        @Test
        @DisplayName("쿼리스트링·폼 본문의 민감값이 마스킹된다 (선두 파라미터 포함)")
        void masks_query_string() {
            assertThat(SensitiveDataMasker.mask("password=abc&client_secret=xyz&page=1"))
                    .isEqualTo("password=***&client_secret=***&page=1");
        }

        @Test
        @DisplayName("URI 안에 박힌 쿼리스트링도 마스킹된다 — sso 리다이렉트 유출 16건의 모양")
        void masks_query_inside_uri() {
            String uri = "/api/v1/oauth2/redirect?redirect_uri=https://a.b/cb"
                    + "&code=Zx9kCvBnMqWeRtYuIoP1234AbCdEfGhIjKlMnOpQrST"
                    + "&state=abcdefghijklmnopqrstuv";

            String masked = SensitiveDataMasker.mask(uri);

            assertThat(masked).contains("code=***");
            // state 는 CSRF nonce — 가리면 OAuth 흐름을 못 따라간다
            assertThat(masked).contains("state=abcdefghijklmnopqrstuv");
        }

        @Test
        @DisplayName("값에 이스케이프된 따옴표가 있어도 전체 값을 가린다(부분 노출 방지)")
        void masks_value_with_escaped_quote() {
            String masked = SensitiveDataMasker.mask("{\"password\":\"a\\\"b\",\"keep\":\"x\"}");

            assertThat(masked).isEqualTo("{\"password\":\"***\",\"keep\":\"x\"}");
        }

        @Test
        @DisplayName("유사 키가 서로를 오염시키지 않는다 — 정규화 후 완전일치다")
        void does_not_corrupt_on_substring_key() {
            assertThat(SensitiveDataMasker.mask("{\"clientSecret\":\"AAA\",\"secret\":\"BBB\"}"))
                    .isEqualTo("{\"clientSecret\":\"***\",\"secret\":\"***\"}");
            // errorCode 는 code 가 아니고, tokenType 은 token 이 아니다
            assertThat(SensitiveDataMasker.mask("{\"error_code\":\"AUTH_002\",\"tokenType\":\"Bearer\"}"))
                    .isEqualTo("{\"error_code\":\"AUTH_002\",\"tokenType\":\"Bearer\"}");
        }

        @Test
        @DisplayName("민감 필드가 없으면 본문을 글자 하나 안 바꾼다")
        void keeps_non_sensitive_body() {
            String body = "{\"name\":\"홍길동\",\"amount\":1000,\"icon\":\"circle-dollar-sign\"}";

            assertThat(SensitiveDataMasker.mask(body)).isEqualTo(body);
        }
    }

    @Nested
    @DisplayName("② 표기 변형 — 스네이크 · 카멜 · 케밥 · 대소문자")
    class KeyNormalization {

        @ParameterizedTest(name = "{0} 는 마스킹된다")
        @ValueSource(strings = {
                "apiKey", "api_key", "API_KEY", "Api-Key", "api.key", "APIKEY",
                "accessToken", "access_token", "ACCESS-TOKEN", "AccessToken",
                "clientSecret", "client_secret", "CLIENT_SECRET", "Client-Secret",
                "refreshToken", "refresh_token", "ssoAccessToken", "sso_access_token"
        })
        void masks_all_spellings(String key) {
            String masked = SensitiveDataMasker.mask("{\"" + key + "\":\"LEAK-VALUE\"}");

            assertThat(masked)
                    .doesNotContain("LEAK-VALUE")
                    .isEqualTo("{\"" + key + "\":\"***\"}");
        }

        @Test
        @DisplayName("키의 대소문자와 표기는 보존된다 — 값만 바뀐다")
        void preserves_key_spelling() {
            assertThat(SensitiveDataMasker.mask("{\"ClientSecret\":\"LEAK\",\"PASSWORD\":\"LEAK2\"}"))
                    .isEqualTo("{\"ClientSecret\":\"***\",\"PASSWORD\":\"***\"}");
        }
    }

    @Nested
    @DisplayName("③ 이름 충돌 — 같은 이름이 두 가지를 뜻할 때 값으로 가른다")
    class NameCollision {

        @Test
        @DisplayName("code=COMMON_200(응답 봉투 상태코드)은 가려지지 않는다")
        void keeps_envelope_code() {
            String body = "{\"success\":true,\"code\":\"COMMON_200\",\"message\":\"OK\"}";

            assertThat(SensitiveDataMasker.mask(body)).isEqualTo(body);
        }

        @ParameterizedTest(name = "code={0} 는 가려지지 않는다")
        @ValueSource(strings = {
                "COMMON_200", "COMMON_500", "AUTH_002", "TOSS_003", "SEC_003", "AST_030",
                "EXP_022", "UNAUTHORIZED", "rate-limit-exceeded", "TABLEAU", "SAP", "MYDATA",
                "LIMS", "desk", "hr", "200"
        })
        void keeps_non_sensitive_code_values(String value) {
            String body = "{\"code\":\"" + value + "\"}";

            assertThat(SensitiveDataMasker.mask(body)).isEqualTo(body);
        }

        @Test
        @DisplayName("code=43자 base64url(OAuth2 인가코드)은 가려진다")
        void masks_authorization_code() {
            String authCode = "Zx9kCvBnMqWeRtYuIoP1234AbCdEfGhIjKlMnOpQrST";
            assertThat(authCode).hasSize(43);

            assertThat(SensitiveDataMasker.mask("{\"code\":\"" + authCode + "\"}"))
                    .isEqualTo("{\"code\":\"***\"}");
            assertThat(SensitiveDataMasker.mask("code=" + authCode + "&grant_type=authorization_code"))
                    .isEqualTo("code=***&grant_type=authorization_code");
        }

        @Test
        @DisplayName("code 의 임계값은 43자다 — 42자는 남고 43자는 가려진다")
        void code_threshold_is_43() {
            // 43자 아래로 내리면 operationId(20~31자)·state(22자)·icon(16자 이상) 같은 정상 필드가
            // 무더기로 걸린다. 실측상 32~42자 구간은 비어 있어 43이 안전한 최소값이다.
            assertThat(SensitiveDataMasker.mask("{\"code\":\"" + "a".repeat(42) + "\"}"))
                    .contains("a".repeat(42));
            assertThat(SensitiveDataMasker.mask("{\"code\":\"" + "a".repeat(43) + "\"}"))
                    .isEqualTo("{\"code\":\"***\"}");
        }

        @Test
        @DisplayName("code_verifier(PKCE 소유증명)는 가려지고 codeChallenge(설계상 공개)는 남는다")
        void masks_verifier_but_not_challenge() {
            String v = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 43자
            String body = "{\"code_verifier\":\"" + v + "\",\"codeChallenge\":\"" + v + "\"}";

            String masked = SensitiveDataMasker.mask(body);

            assertThat(masked).contains("\"code_verifier\":\"***\"");
            assertThat(masked).contains("\"codeChallenge\":\"" + v + "\"");
        }

        @Test
        @DisplayName("JWKS 공개키(n·kid)는 43자 이상이어도 가려지지 않는다 — 공개 정보다")
        void keeps_public_jwks_fields() {
            String modulus = "x".repeat(342);
            String body = "{\"kty\":\"RSA\",\"kid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                    + "\"n\":\"" + modulus + "\",\"e\":\"AQAB\"}";

            assertThat(SensitiveDataMasker.mask(body)).isEqualTo(body);
        }

        @Test
        @DisplayName("client_id 는 가려지지 않는다 — SSO 에서는 공개 식별자(desk·hr)다")
        void keeps_client_id_by_default() {
            String body = "{\"client_id\":\"desk\",\"clientId\":\"hr\"}";

            assertThat(SensitiveDataMasker.mask(body)).isEqualTo(body);
        }

        @Test
        @DisplayName("이메일·전화번호는 기본으로 가리지 않는다 — 진단 식별자라 정책은 서비스가 정한다")
        void keeps_contact_pii_by_default() {
            String body = "{\"email\":\"a@b.com\",\"phone\":\"010-1234-5678\"}";

            assertThat(SensitiveDataMasker.mask(body)).isEqualTo(body);
            assertThat(SensitiveDataMasker.withExtraKeys(SensitiveDataMasker.CONTACT_PII_KEYS).apply(body))
                    .isEqualTo("{\"email\":\"***\",\"phone\":\"***\"}");
        }
    }

    @Nested
    @DisplayName("④ 값 모양 기반 — 이름을 몰라도 잡는 최종 방어선")
    class ValueShape {

        @Test
        @DisplayName("JWT 는 키 이름이 무엇이든 가려진다 — DTO 필드명을 새로 지어도 안 뚫린다")
        void masks_jwt_regardless_of_key() {
            String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                    + ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4ifQ"
                    + ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

            // 목록에 없는 이름들 — 이게 요점이다
            String body = "{\"someBrandNewFieldName\":\"" + jwt + "\",\"x\":\"" + jwt + "\"}";

            assertThat(SensitiveDataMasker.mask(body)).doesNotContain("eyJ").doesNotContain(jwt);
            // 배열·평문·쿼리 어디에 있어도 마찬가지
            assertThat(SensitiveDataMasker.mask("raw log line " + jwt + " tail")).doesNotContain("eyJ");
            assertThat(SensitiveDataMasker.mask("?assertion=" + jwt)).doesNotContain("eyJ");
        }

        @Test
        @DisplayName("Bearer 뒤 토큰이 가려지고, 산문 'Bearer Authentication' 은 안 걸린다")
        void masks_bearer_token_only() {
            assertThat(SensitiveDataMasker.mask("Authorization: Bearer abcdefghijklmnopqrstuvwxyz012345"))
                    .isEqualTo("Authorization: Bearer ***");
            assertThat(SensitiveDataMasker.mask("uses Bearer Authentication scheme"))
                    .isEqualTo("uses Bearer Authentication scheme");
            assertThat(SensitiveDataMasker.mask("{\"tokenType\":\"Bearer\"}"))
                    .isEqualTo("{\"tokenType\":\"Bearer\"}");
        }

        @Test
        @DisplayName("긴 정상 값(아이콘명·operationId·state)은 값 모양 규칙에 안 걸린다")
        void keeps_long_but_harmless_values() {
            String body = "{\"icon\":\"circle-dollar-sign\",\"operationId\":\"getUserScheduleList\","
                    + "\"excludeInvestmentCaution\":\"false\",\"expenseCategoryRowId\":\"abcdefghijklmnopqrst\"}";

            assertThat(SensitiveDataMasker.mask(body)).isEqualTo(body);
        }
    }

    @Nested
    @DisplayName("⑤ 망가진 입력에도 예외를 던지지 않는다")
    class NeverThrows {

        @Test
        @DisplayName("null·빈 문자열은 그대로 돌려준다")
        void handles_null_and_empty() {
            assertThat(SensitiveDataMasker.mask(null)).isNull();
            assertThat(SensitiveDataMasker.mask("")).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "not json at all",
                "{\"password\":",                       // 잘린 JSON
                "{\"password\":\"unterminated",         // 닫히지 않은 값
                "{\"password\":\"a\\",                  // 끝이 이스케이프
                "��� binary garbage �",  // 비-UTF8 바이트가 디코딩된 모양
                "&&&===&&&", "?=", "{}", "[]", "null",
                "{\"password\":null}", "{\"password\":true}", "{\"password\":\"\"}"
        })
        void never_throws(String input) {
            assertThatCode(() -> SensitiveDataMasker.mask(input)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("잘린 JSON 안의 민감값도 최대한 가린다")
        void masks_inside_truncated_json() {
            assertThat(SensitiveDataMasker.mask("{\"a\":1,\"password\":\"leak\",\"b\":"))
                    .doesNotContain("leak");
        }

        @Test
        @DisplayName("비-UTF8 디코딩 결과(치환문자 섞임)에서도 민감값을 가린다")
        void masks_with_replacement_chars() {
            String body = "�{\"password\":\"leak\"}�";

            assertThat(SensitiveDataMasker.mask(body)).doesNotContain("leak");
        }
    }

    @Nested
    @DisplayName("⑥ 폭주하지 않는다 — 스택·시간 상한")
    class NoBlowUp {

        /**
         * 예전 판의 {@code (?:\\.|[^"\\])*} 는 값 2,000자에서 {@code StackOverflowError} 를 냈고,
         * 그 {@code Error} 가 필터의 {@code catch (Exception)} 을 뚫고 나가
         * {@code copyBodyToResponse()} 를 건너뛰게 만들었다 — <b>클라이언트가 빈 응답을 받는다.</b>
         * 이 테스트가 그 회귀를 잡는 그물이다.
         */
        @ParameterizedTest(name = "민감값 {0}자에서 StackOverflowError 가 나지 않는다")
        @ValueSource(ints = {2_000, 100_000, 1_000_000})
        void does_not_overflow_stack(int length) {
            String body = "{\"password\":\"" + "a".repeat(length) + "\",\"keep\":\"x\"}";

            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                String masked = SensitiveDataMasker.mask(body);
                assertThat(masked).isEqualTo("{\"password\":\"***\",\"keep\":\"x\"}");
            });
        }

        @Test
        @DisplayName("이스케이프가 촘촘한 긴 값에서도 즉시 끝난다")
        void handles_dense_escapes() {
            String body = "{\"password\":\"" + "a\\\"".repeat(100_000) + "\"}";

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> assertThat(SensitiveDataMasker.mask(body)).isEqualTo("{\"password\":\"***\"}"));
        }

        @Test
        @DisplayName("JWT 패턴이 미완성 입력에서 폭주하지 않는다 — 중첩 수량자 금지의 회귀 테스트")
        void jwt_pattern_does_not_backtrack() {
            String bait = "eyJ" + "a".repeat(200_000) + "!";

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> assertThat(SensitiveDataMasker.mask(bait)).isEqualTo(bait));
        }

        @Test
        @DisplayName("깊게 중첩된 거대 본문도 시간 안에 끝난다")
        void handles_deeply_nested_large_body() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 20_000; i++) {
                sb.append("{\"lvl\":").append(i).append(",\"password\":\"leak").append(i).append("\",");
            }

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> assertThat(SensitiveDataMasker.mask(sb.toString())).doesNotContain("leak"));
        }

        @Test
        @DisplayName("쿼리 파라미터가 아주 많아도 시간 안에 끝난다")
        void handles_many_query_params() {
            StringBuilder sb = new StringBuilder("a=1");
            for (int i = 0; i < 50_000; i++) {
                sb.append("&password=leak").append(i);
            }

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> assertThat(SensitiveDataMasker.mask(sb.toString())).doesNotContain("leak"));
        }
    }

    @Nested
    @DisplayName("⑦ 부작용이 없다 — 입력 불변 · 멱등")
    class NoSideEffects {

        @Test
        @DisplayName("입력 문자열 자체는 변하지 않는다")
        void does_not_mutate_input() {
            String original = "{\"password\":\"leak\",\"id\":1}";
            String copy = new String(original.toCharArray());

            String masked = SensitiveDataMasker.mask(original);

            assertThat(original).isEqualTo(copy);
            assertThat(masked).isNotSameAs(original).isEqualTo("{\"password\":\"***\",\"id\":1}");
        }

        @Test
        @DisplayName("이미 마스킹된 문자열을 다시 넣어도 결과가 같다(멱등)")
        void is_idempotent() {
            String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abcdefghijklmnopqrstuvwxyz";
            String once = SensitiveDataMasker.mask(
                    "{\"clientSecret\":\"LEAK\",\"id\":1,\"tk\":\"" + jwt + "\"}"
                            + "?password=x&code=COMMON_200");

            assertThat(SensitiveDataMasker.mask(once)).isEqualTo(once);
        }

        @Test
        @DisplayName("추가 키는 기본 키를 지우지 않는다 — 추가는 조이기만 한다")
        void extra_keys_only_tighten() {
            SensitiveDataMasker desk = SensitiveDataMasker.withExtraKeys("clientId", "client_id");
            String body = "{\"clientId\":\"tsck_live_ArfgoFbLDafecJFMjelTAeB\","
                    + "\"clientSecret\":\"tssk_live_ebkuEpOmb5T6XtZ538o1xZtrDs10Zu3acztY8n5yF3a\","
                    + "\"password\":\"still-masked\",\"code\":\"COMMON_200\"}";

            String masked = desk.apply(body);

            assertThat(masked).doesNotContain("tsck_live_", "tssk_live_", "still-masked");
            assertThat(masked).contains("\"clientId\":\"***\"", "\"clientSecret\":\"***\"",
                    "\"password\":\"***\"");
            // 추가 키를 넣어도 이름 충돌 처리는 그대로다
            assertThat(masked).contains("\"code\":\"COMMON_200\"");
        }

        @Test
        @DisplayName("추가 키를 안 넣어도 기본만으로 안전하다 — 추가는 선택이다")
        void default_is_safe_without_extras() {
            String body = "{\"password\":\"a\",\"client_secret\":\"b\",\"refresh_token\":\"c\"}";

            assertThat(SensitiveDataMasker.mask(body))
                    .isEqualTo("{\"password\":\"***\",\"client_secret\":\"***\",\"refresh_token\":\"***\"}");
        }

        @Test
        @DisplayName("빈 추가 키 목록·null 은 기본 인스턴스를 그대로 준다")
        void empty_extras_reuse_default() {
            assertThat(SensitiveDataMasker.withExtraKeys()).isSameAs(SensitiveDataMasker.DEFAULT);
            assertThat(SensitiveDataMasker.withExtraKeys((java.util.Collection<String>) null))
                    .isSameAs(SensitiveDataMasker.DEFAULT);
        }
    }
}
