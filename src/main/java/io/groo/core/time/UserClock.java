package io.groo.core.time;

import io.groo.core.util.TimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 사용자 기준 시각.
 *
 * <p>서버·DB 는 UTC 로 두고(로그·시점 비교의 표준), 사용자에게 보이는 "오늘"·"지금" 만
 * 사용자 타임존으로 판단한다. 타임존 조회는 서비스마다 다르므로 {@link UserZoneProvider} 에 위임한다.
 *
 * <p><b>왜 필요한가</b> — 컨테이너에 TZ 가 없으면 JVM 기본이 UTC 다. 그대로
 * {@code LocalDate.now()} 를 쓰면 한국 사용자에게는 오전 9시 전까지 "오늘" 이 하루 전으로 잡힌다.
 *
 * <p><b>벽시계 컬럼과의 정합</b> — 거래일·이체일·잔액 효력 시각처럼 사용자가 의미를 정하는
 * 컬럼([userClock])은 타임존 없는 naive 값이고, 클라이언트가 보내는 값은 사용자 로컬 벽시계다.
 * 서버가 같은 컬럼에 UTC 로 찍으면 한 컬럼에 두 기준이 섞여 정렬이 깨진다
 * (실제로 잔액 앵커가 거래보다 뒤로 밀려 이체가 사라진 사고가 있었다).
 * 그래서 그 컬럼들에 넣을 값은 반드시 이 클래스로 만든다.
 *
 * <p>시스템이 발생 사실을 기록하는 컬럼([UTC] — create_at·sent_at 등)은 이 클래스가 아니라
 * UTC 로 찍고, 화면에 낼 때만 {@link TimeUtils#toUserZone} 으로 바꾼다.
 *
 * <p>배치처럼 특정 사용자에 속하지 않는 처리는 {@link ServiceClock} 을 쓴다.
 */
public class UserClock {

    private final UserZoneProvider zoneProvider;
    private final ServiceClock serviceClock;

    public UserClock(UserZoneProvider zoneProvider, ServiceClock serviceClock) {
        this.zoneProvider = zoneProvider;
        this.serviceClock = serviceClock;
    }

    /** 사용자 타임존. 알 수 없거나 조회 실패면 서비스 기준으로 폴백. */
    public ZoneId zoneOf(Long userRowId) {
        if (userRowId == null) {
            return serviceClock.zone();
        }
        ZoneId zone = zoneProvider.zoneOf(userRowId);
        return zone != null ? zone : serviceClock.zone();
    }

    /**
     * 타임존 문자열을 이미 손에 쥔 경우(엔티티를 조회해 둔 경우) 재조회 없이 해석한다.
     * 값이 없거나 깨졌으면 서비스 기준으로 폴백.
     */
    public ZoneId zoneOfTimezone(String timezone) {
        return TimeUtils.resolveZone(timezone, serviceClock.zone());
    }

    /** 사용자 기준 오늘 날짜. */
    public LocalDate today(Long userRowId) {
        return LocalDate.now(zoneOf(userRowId));
    }

    /** 사용자 기준 현재 일시. */
    public LocalDateTime now(Long userRowId) {
        return LocalDateTime.now(zoneOf(userRowId));
    }

    /** 타임존 문자열 기준 오늘 날짜 (사용자 엔티티를 이미 가진 호출부용 — 추가 조회 없음). */
    public LocalDate todayIn(String timezone) {
        return LocalDate.now(zoneOfTimezone(timezone));
    }

    /** 타임존 문자열 기준 현재 일시 (사용자 엔티티를 이미 가진 호출부용 — 추가 조회 없음). */
    public LocalDateTime nowIn(String timezone) {
        return LocalDateTime.now(zoneOfTimezone(timezone));
    }
}
