package io.groo.core.time;

import java.time.ZoneId;

/**
 * 사용자 타임존 조회 포트.
 *
 * <p>{@link UserClock} 이 "이 사용자의 오늘"을 판단하려면 사용자 타임존을 알아야 하는데,
 * 그 값이 어디에 있는지는 서비스마다 다르다. core 가 각 서비스의 User 엔티티·Repository 에
 * 의존하지 않도록 조회만 인터페이스로 뽑는다.
 *
 * <p>구현 예시
 * <ul>
 *   <li><b>desk</b> — {@code users.timezone} (사용자가 직접 고른 표시 기준 지역)</li>
 *   <li><b>hr</b> — {@code users.user_origin_company} → {@code company.timezone}
 *       (인력관리라 소속 회사 소재지를 따른다)</li>
 *   <li><b>sso</b> — {@code users.timezone} (가입 지역)</li>
 * </ul>
 *
 * <p>구현체는 <b>예외를 던지지 않는다</b>. 알 수 없는 사용자·깨진 설정값이면 {@code null} 을
 * 돌려주고, 폴백 판단은 {@link UserClock} 에 맡긴다 — 타임존 하나 때문에 화면이 죽으면 안 된다.
 */
@FunctionalInterface
public interface UserZoneProvider {

    /**
     * 사용자 타임존. 알 수 없으면 {@code null}(호출부가 서비스 기준으로 폴백).
     *
     * @param userRowId 사용자 행 아이디 (null 허용 — 이 경우 null 반환)
     */
    ZoneId zoneOf(Long userRowId);
}
