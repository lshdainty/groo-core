package io.groo.core.security.event;

/**
 * SSO Redis 이벤트 서명 약속 — 발행({@link SsoEventSigner}, groo-sso-back)과 검증({@link SsoEventVerifier},
 * 담다 · 이음)이 같은 이름을 쓰도록 한곳에 둔다(core 3.4.0, 보안 점검 IEU-6).
 *
 * <h3>모양</h3>
 * 기존 이벤트 JSON 에 {@value #SIG_FIELD} 필드 하나를 덧붙인다. 값은 SSO 서명 키(JWT 와 같은 RS256 키, JWKS 로
 * 공개)로 서명한 compact JWS 이고, 클레임은 다음과 같다.
 * <ul>
 *   <li>{@code typ} = {@value #TYPE} — SSO 가 같은 키로 서명하는 access 토큰 등을 이벤트로 끼워 넣지 못하게</li>
 *   <li>{@code chn} — 발행 채널. 한 채널의 이벤트를 다른 채널로 다시 쏘지 못하게</li>
 *   <li>{@code evt} — 이벤트 JSON 원문(문자열). 검증에 통과하면 받는 쪽은 바깥 JSON 이 아니라 <b>이 값</b>을 쓴다
 *       — 바깥 필드를 바꿔치기해도 효과가 없다</li>
 *   <li>{@code iat} · {@code exp}(짧게, 기본 5분) · {@code jti}(받는 쪽이 짧게 기억해 재전송을 막는다)</li>
 * </ul>
 * 바깥 필드를 그대로 두는 이유는 하위 호환이다 — 서명을 모르는 옛 구독자는 모르는 필드 하나를 무시하고 예전처럼 읽는다.
 */
public final class SsoEventSignature {

    /** 이벤트 JSON 에 덧붙이는 서명 필드 */
    public static final String SIG_FIELD = "sig";

    /** {@code typ} 클레임 값 */
    public static final String TYPE = "sso-event";

    /** SSO 사용자 이벤트 채널(생성 · 수정 · 삭제 · 합침) */
    public static final String USER_EVENTS_CHANNEL = "porest:sso:user-events";

    /** SSO 세션 이벤트 채널(전체 폐기) */
    public static final String SESSION_EVENTS_CHANNEL = "porest:sso:session-events";

    static final String CLAIM_TYPE = "typ";
    static final String CLAIM_CHANNEL = "chn";
    static final String CLAIM_EVENT = "evt";

    private SsoEventSignature() {
    }
}
