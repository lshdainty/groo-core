package io.groo.core.security.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * 클라이언트 IP 판정 — IP 차단·레이트리밋·감사 기록·요청 로그가 같이 쓴다(core 3.2.0, groo-ieum-back 에서 올림).
 *
 * <p>{@code HttpUtils.getClientIp} 도 이걸 쓴다. 예전 core 는 {@code X-Forwarded-For} 의 <b>맨 앞</b> 값을 믿어서
 * 요청자가 헤더를 넣는 것만으로 IP 를 바꿀 수 있었다 — sso 의 비밀번호 찾기·초대·핸드오프 레이트리밋이 그 값으로
 * 사람을 갈라 요청마다 헤더를 바꾸면 무한히 우회됐다(2026-11-11 점검).
 *
 * <p><b>전제: 서비스는 {@code server.forward-headers-strategy: native} 여야 한다.</b> {@code framework}(Spring
 * ForwardedHeaderFilter)는 이 클래스가 보기 전에 {@code getRemoteAddr()} 를 X-Forwarded-For 맨 왼쪽(위조 가능) 값으로
 * 바꿔 버려, 여기서 아무리 조심해도 위조값이 '직접 연결된 공인 IP' 로 보인다. native(Tomcat RemoteIpValve)는 사설 대역
 * 프록시만 믿고 오른쪽부터 읽으므로 getRemoteAddr() 가 실제 사용자 IP 가 되고, 이 클래스는 그 값을 그대로 쓴다.
 *
 * <p>규칙: 믿는 프록시에서 온 요청일 때만 전달 헤더를 쓴다. 믿는 프록시는 직접 연결된 상대(remoteAddr)가
 * 사설 대역(10/8 · 172.16/12 · 192.168/16 · 127/8 · ::1 · fc00::/7)인 경우다(Tomcat RemoteIpValve 의
 * internalProxies 기본과 같은 뜻). 그 경우 X-Real-IP(프록시가 덮어씀)를 먼저 보고, 없으면
 * X-Forwarded-For 를 오른쪽부터 읽어 믿는 프록시(사설 주소)를 걷어 낸 첫 값을 쓴다.
 * 믿는 프록시가 아니면 헤더는 위조일 수 있으므로 remoteAddr 를 그대로 쓴다.
 */
public final class ClientIpResolver {

    private static final Pattern LITERAL = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isTrustedProxy(remote)) {
            return remote;
        }

        String realIp = clean(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String ip = clean(parts[i]);
                if (ip == null) {
                    // 형식이 깨진 값 왼쪽은 믿을 수 없다 — 더 읽지 않는다
                    break;
                }
                if (!isTrustedProxy(ip)) {
                    return ip;
                }
            }
        }
        return remote;
    }

    public static boolean isTrustedProxy(String ip) {
        InetAddress addr = parse(ip);
        if (addr == null) {
            return false;
        }
        if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC; // fc00::/7
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return parse(v) != null ? v : null;
    }

    /** IP 리터럴만 파싱한다(문자 검사를 먼저 해서 DNS 조회가 일어나지 않는다). */
    private static InetAddress parse(String ip) {
        if (ip == null || !LITERAL.matcher(ip).matches()) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            return null;
        }
    }
}
