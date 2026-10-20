package io.groo.core.time;

import io.groo.core.util.TimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 서비스 운영 기준 시각.
 *
 * <p>서버·DB 는 UTC 로 두고(로그·시점 비교의 표준), <b>날짜 판단만</b> 이 기준으로 한다.
 * 컨테이너에 TZ 가 없으면 JVM 기본이 UTC 라서, 그대로 {@code LocalDate.now()} 를 쓰면
 * 한국 시각 오전 9시 전까지 "오늘" 이 하루 전으로 잡힌다.
 *
 * <p>여기서 다루는 건 <b>사용자별</b> 타임존이 아니라 <b>서비스 운영</b> 타임존이다.
 * 배치(스케줄러)는 특정 사용자의 것이 아니라 서비스 전체 기준으로 돌아야 하므로 이 값을 쓴다.
 * 사용자 화면에 보이는 "오늘" 은 {@link UserClock} 을 쓴다.
 *
 * <p>{@code @Scheduled(zone = ...)} 는 <b>발화 시각</b>만 옮길 뿐, 메서드 안의
 * {@code LocalDate.now()} 는 여전히 JVM 기본(UTC)이다. 둘을 같은 기준으로 맞추기 위해
 * 스케줄러 본문에서도 이 클래스를 쓴다.
 *
 * <p>Spring 어노테이션을 붙이지 않는다 — 컴포넌트 스캔 범위(서비스별 basePackage)에
 * core 가 들어오는지에 의존하지 않도록, 각 서비스가 {@code @Bean} 으로 명시 등록한다.
 */
public class ServiceClock {

    private final ZoneId zone;

    /**
     * @param zone IANA 타임존 ID (예: {@code "Asia/Seoul"}).
     *             null·빈 값·해석 불가면 UTC 로 폴백한다(값을 잃지 않는 쪽).
     */
    public ServiceClock(String zone) {
        this.zone = TimeUtils.resolveZone(zone, ZoneId.of("UTC"));
    }

    public ServiceClock(ZoneId zone) {
        this.zone = zone != null ? zone : ZoneId.of("UTC");
    }

    public ZoneId zone() {
        return zone;
    }

    /** 서비스 기준 오늘 날짜. */
    public LocalDate today() {
        return LocalDate.now(zone);
    }

    /** 서비스 기준 현재 일시. */
    public LocalDateTime now() {
        return LocalDateTime.now(zone);
    }
}
