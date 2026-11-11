package io.groo.core.security.web;

/**
 * 차단 IP 목록 — 저장 방식(DB·Redis·메모리)은 서비스가 구현한다(core 3.2.0).
 * {@link IpBlockFilter} 가 요청마다 부르므로 빠르게(캐시) 답해야 한다.
 */
public interface IpBlocklist {

    boolean isBlocked(String ip);
}
