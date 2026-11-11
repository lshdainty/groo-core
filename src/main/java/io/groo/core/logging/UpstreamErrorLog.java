package io.groo.core.logging;

import io.groo.core.logging.SensitiveDataMasker;
import org.springframework.web.client.RestClientException;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * 업스트림(외부 API) 오류를 <b>로그로 옮길 때</b> 씌우는 가림막.
 *
 * <p>groo-damda-back 의 증권사(나무·토스) 연동에서 만들어 core 로 올렸다(3.1.0). 아래 예시는 그때 것이다.
 *
 * <h2>왜 필요한가</h2>
 * {@code RequestResponseLoggingFilter} 의 마스킹은 <b>우리 서버의</b> HTTP 요청·응답 본문과
 * 쿼리스트링만 지난다. 업스트림 호출이 실패해서 클라이언트가 직접 찍는 로그는 그 필터를
 * 안 거치므로 아무것도 가려지지 않는다.
 *
 * <p>그리고 그 자리에 들어오는 값이 하필 남의 응답 본문이다. Spring 의
 * {@code RestClientResponseException} 은 <b>메시지에 응답 본문을 통째로 싣는다</b>
 * ({@code 400 Bad Request: "{...}"}). 그래서 {@code log.error("...", e)} 한 줄이면
 * 나무가 돌려준 {@code cust_no}·{@code acct_no} 가 스택트레이스와 함께 그대로 남는다.
 *
 * <h2>예외를 cause 로 그대로 달면 안 되는 이유</h2>
 * 호출부의 {@code log.error} 만 고쳐도 소용이 없다. core 의 {@code GlobalExceptionHandler} 가
 * {@code ExternalServiceException} 을 예외 객체째 찍으므로({@code log.error(..., e)}),
 * 원본을 cause 로 달아 두면 {@code Caused by:} 줄에서 지우기 전 메시지가 다시 나온다.
 * 그래서 {@link #redact(RestClientException)} 로 <b>가린 예외를 새로 만들어</b> 그걸 단다.
 * 같은 판단을 {@code AbstractBrokerTokenManager.redactSecrets} 가 이미 하고 있다(#253).
 *
 * <h2>두 겹으로 가린다</h2>
 * <ol>
 *   <li><b>core {@link SensitiveDataMasker}</b> — 응답 본문이 JSON 이면 그대로 먹는다.
 *       {@code acct_no}·{@code act_no}·{@code cust_no}·{@code rnm_cfm_no} 는 core 기본 키에 있다.</li>
 *   <li><b>긴 숫자열</b> — 마스커는 이름이 붙은 값만 본다. 업무 오류 메시지({@code rsp_msg})는
 *       "계좌번호 11111111103 을 확인하세요" 같은 <b>자유 문장</b>이라 이름이 없다. 10자리 이상
 *       숫자열은 금액이 아니라 식별번호(계좌 11~14자리, 실명확인번호 13자리)이므로 뒤 4자리만 남긴다
 *       — {@code NamuQueryServiceImpl.maskAccountNo} 와 같은 표기다.</li>
 * </ol>
 */
public final class UpstreamErrorLog {

    private static final SensitiveDataMasker MASKER = SensitiveDataMasker.DEFAULT;

    /** 자유 문장에 섞여 나오는 식별번호. 금액과 겹치지 않게 10자리 이상만 잡는다. */
    private static final Pattern LONG_DIGITS = Pattern.compile("\\d{10,}");

    private static final String EMPTY = "(없음)";

    private UpstreamErrorLog() {
    }

    /**
     * 업스트림이 준 문자열을 로그에 실을 수 있는 모양으로 바꾼다.
     * {@code null}·공백은 {@code (없음)} 이 된다 — 어떤 입력에도 예외를 던지지 않는다.
     */
    public static String safe(String upstreamText) {
        if (upstreamText == null || upstreamText.isBlank()) {
            return EMPTY;
        }
        return LONG_DIGITS.matcher(MASKER.apply(upstreamText)).replaceAll(UpstreamErrorLog::keepLastFour);
    }

    /**
     * 로그와 {@code cause} 양쪽에 안전하게 쓸 대체 예외.
     *
     * <p>원본 타입은 이름으로만 남긴다({@code HttpClientErrorException$BadRequest: ...}) —
     * 무엇이 터졌는지는 진단에 필요하고, 이름에는 본문이 없다. 원인 예외(타임아웃 등)는
     * 그대로 물려 I/O 진단 정보를 잃지 않는다. <b>원본을 cause 로 달지 않는다</b> —
     * 달면 {@code Caused by:} 줄에 가리기 전 메시지가 그대로 다시 나온다.
     */
    public static RestClientException redact(RestClientException e) {
        return new RestClientException(e.getClass().getSimpleName() + ": " + safe(e.getMessage()), e.getCause());
    }

    private static String keepLastFour(MatchResult match) {
        String digits = match.group();
        return "****" + digits.substring(digits.length() - 4);
    }
}
