package io.groo.core.util;

import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.InvalidValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 날짜/시간 관련 유틸리티
 * <p>
 * 날짜 비교, 범위 계산, 포맷팅, 기간 계산 등
 * 비즈니스 로직에서 자주 사용되는 시간 관련 기능을 제공합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>날짜 비교: {@link #isAfter}, {@link #isBefore}, {@link #isSameDay}</li>
 *   <li>범위 조회: {@link #getDateRange}, {@link #filterByDayOfWeek}</li>
 *   <li>집합 연산: {@link #mergeDates}, {@link #excludeDates}</li>
 *   <li>최대/최소: {@link #latest}, {@link #earliest}</li>
 *   <li>시작/끝 시간: {@link #startOfDay}, {@link #endOfDay}, {@link #startOfMonth} 등</li>
 *   <li>기간 계산: {@link #daysBetween}, {@link #monthsBetween}, {@link #calculateAge}</li>
 *   <li>주말/평일: {@link #isWeekend}, {@link #isWeekday}</li>
 *   <li>포맷팅: {@link #format}, {@link #parse}</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 날짜 비교
 * if (TimeUtils.isAfter(startDate, endDate)) {
 *     throw new InvalidValueException("시작일이 종료일보다 늦습니다");
 * }
 *
 * // 날짜 범위 조회
 * List<LocalDate> dates = TimeUtils.getDateRange(startDate, endDate);
 *
 * // 주말 제외
 * List<LocalDate> weekdays = dates.stream()
 *     .filter(TimeUtils::isWeekday)
 *     .toList();
 *
 * // 나이 계산
 * int age = TimeUtils.calculateAge(LocalDate.of(1990, 1, 15));
 *
 * // 이번 달 시작/끝
 * LocalDate monthStart = TimeUtils.startOfMonth(LocalDate.now());
 * LocalDate monthEnd = TimeUtils.endOfMonth(LocalDate.now());
 * }</pre>
 *
 * <h3>요일 코드 (ISO-8601)</h3>
 * <table border="1">
 *   <tr><th>코드</th><th>요일</th><th>DayOfWeek</th></tr>
 *   <tr><td>1</td><td>월요일</td><td>MONDAY</td></tr>
 *   <tr><td>2</td><td>화요일</td><td>TUESDAY</td></tr>
 *   <tr><td>3</td><td>수요일</td><td>WEDNESDAY</td></tr>
 *   <tr><td>4</td><td>목요일</td><td>THURSDAY</td></tr>
 *   <tr><td>5</td><td>금요일</td><td>FRIDAY</td></tr>
 *   <tr><td>6</td><td>토요일</td><td>SATURDAY</td></tr>
 *   <tr><td>7</td><td>일요일</td><td>SUNDAY</td></tr>
 * </table>
 *
 * @author groo
 * @see LocalDate
 * @see LocalDateTime
 * @see DayOfWeek
 */
public final class TimeUtils {

    private static final Logger log = LoggerFactory.getLogger(TimeUtils.class);

    /** ISO 날짜 포맷 (yyyy-MM-dd) */
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** ISO 날짜시간 포맷 (yyyy-MM-dd'T'HH:mm:ss) */
    public static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 한국식 날짜 포맷 (yyyy년 MM월 dd일) */
    public static final DateTimeFormatter KOREAN_DATE = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /** 슬래시 날짜 포맷 (yyyy/MM/dd) */
    public static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 컴팩트 날짜 포맷 (yyyyMMdd) */
    public static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 시간 포맷 (HH:mm:ss) */
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 시간 포맷 - 초 제외 (HH:mm) */
    public static final DateTimeFormatter TIME_SHORT = DateTimeFormatter.ofPattern("HH:mm");

    private TimeUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ========================================
    // 타임존 변환
    // ========================================
    //
    // 서버(컨테이너)와 DB 는 UTC 로 돌고, 사용자에게 보이는 시각만 사용자 타임존으로 바꾼다.
    // 두 종류의 시각을 절대 섞지 말 것:
    //
    //   [UTC]        시스템이 찍는 절대 시각 (create_at·modify_at·sent_at·granted_at …).
    //                저장·비교 모두 UTC. 화면에 낼 때만 toUserZone() 으로 바꾼다.
    //   [userClock]  사용자가 의미를 정하는 벽시계 (거래일·이체일·마감일 …).
    //                이미 사용자 로컬 값이므로 변환하지 않는다 — 변환하면 자정 근처 날짜가 하루 밀린다.
    //
    // LocalDateTime 은 타임존 정보를 갖지 않으므로, 어느 쪽 기준인지는 컬럼 COMMENT 와
    // 필드 주석의 표기([UTC] / [userClock])로만 구분된다.

    /**
     * UTC 로 저장된 시각을 사용자 타임존 벽시계로 변환한다. 화면 표시용.
     *
     * <p>{@code utc} 가 null 이면 null 을 돌려준다. {@code zoneId} 가 null·빈 값·해석 불가면
     * {@code fallbackZone} 을 쓰고, 그마저 없으면 UTC 로 둔다(값을 잃지 않는 쪽으로).
     *
     * @param utc          UTC 기준 시각 ([UTC] 컬럼에서 읽은 값)
     * @param zoneId       사용자 타임존 (예: {@code "Asia/Seoul"})
     * @param fallbackZone zoneId 해석 실패 시 대체 타임존 (null 허용)
     * @return 사용자 타임존 벽시계
     */
    public static LocalDateTime toUserZone(LocalDateTime utc, String zoneId, ZoneId fallbackZone) {
        if (utc == null) {
            return null;
        }
        ZoneId target = resolveZone(zoneId, fallbackZone);
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(target).toLocalDateTime();
    }

    /**
     * UTC → 사용자 타임존 변환 (폴백 없음 — 해석 실패 시 UTC 유지).
     */
    public static LocalDateTime toUserZone(LocalDateTime utc, String zoneId) {
        return toUserZone(utc, zoneId, null);
    }

    /**
     * 사용자 타임존 벽시계를 UTC 로 되돌린다. 사용자 입력을 [UTC] 컬럼에 저장할 때 쓴다.
     *
     * <p>주의: [userClock] 컬럼(거래일 등)에는 쓰지 말 것 — 그 컬럼은 벽시계 그대로 저장한다.
     *
     * @param userLocal    사용자 타임존 기준 벽시계
     * @param zoneId       사용자 타임존
     * @param fallbackZone zoneId 해석 실패 시 대체 타임존 (null 허용)
     * @return UTC 기준 시각
     */
    public static LocalDateTime toUtc(LocalDateTime userLocal, String zoneId, ZoneId fallbackZone) {
        if (userLocal == null) {
            return null;
        }
        ZoneId source = resolveZone(zoneId, fallbackZone);
        return userLocal.atZone(source).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * 사용자 타임존 벽시계 → UTC (폴백 없음 — 해석 실패 시 UTC 로 간주해 그대로 둔다).
     */
    public static LocalDateTime toUtc(LocalDateTime userLocal, String zoneId) {
        return toUtc(userLocal, zoneId, null);
    }

    /**
     * 사용자 타임존 기준 "오늘".
     *
     * <p>{@link #today()} 는 JVM 기본 타임존을 타므로 컨테이너(UTC)에서는 한국 사용자에게
     * 오전 9시 전까지 하루 전 날짜를 준다. 사용자 화면의 "오늘"은 반드시 이 메서드를 쓴다.
     *
     * @param zoneId       사용자 타임존
     * @param fallbackZone zoneId 해석 실패 시 대체 타임존 (null 허용)
     */
    public static LocalDate todayIn(String zoneId, ZoneId fallbackZone) {
        return LocalDate.now(resolveZone(zoneId, fallbackZone));
    }

    /**
     * 사용자 타임존 기준 "지금".
     *
     * @param zoneId       사용자 타임존
     * @param fallbackZone zoneId 해석 실패 시 대체 타임존 (null 허용)
     */
    public static LocalDateTime nowIn(String zoneId, ZoneId fallbackZone) {
        return LocalDateTime.now(resolveZone(zoneId, fallbackZone));
    }

    /**
     * 타임존 문자열을 {@link ZoneId} 로 해석한다.
     *
     * <p>사용자 설정값이 깨져 있어도 화면이 죽으면 안 되므로 예외를 던지지 않고
     * {@code fallbackZone} → UTC 순으로 폴백한다.
     *
     * @param zoneId       타임존 문자열 (예: {@code "Asia/Seoul"}), null·빈 값 허용
     * @param fallbackZone 해석 실패 시 대체 타임존 (null 이면 UTC)
     */
    public static ZoneId resolveZone(String zoneId, ZoneId fallbackZone) {
        ZoneId fallback = fallbackZone != null ? fallbackZone : ZoneOffset.UTC;
        if (zoneId == null || zoneId.isBlank()) {
            return fallback;
        }
        try {
            return ZoneId.of(zoneId.trim());
        } catch (DateTimeException e) {
            log.warn("알 수 없는 타임존 '{}' — {} 로 폴백", zoneId, fallback);
            return fallback;
        }
    }

    // ========================================
    // 현재 시간
    // ========================================

    /**
     * 현재 날짜 반환
     *
     * @return 오늘 날짜
     * @deprecated JVM 기본 타임존을 타므로 컨테이너(UTC)에서 사용자 기준 "오늘"과 어긋난다.
     *             사용자 화면용은 {@link #todayIn(String, ZoneId)} 를 쓸 것.
     */
    @Deprecated(since = "2.1.0")
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 현재 날짜/시간 반환
     *
     * @return 현재 날짜/시간
     * @deprecated JVM 기본 타임존을 타므로 컨테이너(UTC)에서 사용자 기준 "지금"과 어긋난다.
     *             사용자 화면용은 {@link #nowIn(String, ZoneId)} 를 쓸 것.
     */
    @Deprecated(since = "2.1.0")
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 어제 날짜 반환
     *
     * @return 어제 날짜
     * @deprecated {@link #todayIn(String, ZoneId)}{@code .minusDays(1)} 를 쓸 것.
     */
    @Deprecated(since = "2.1.0")
    public static LocalDate yesterday() {
        return LocalDate.now().minusDays(1);
    }

    /**
     * 내일 날짜 반환
     *
     * @return 내일 날짜
     * @deprecated {@link #todayIn(String, ZoneId)}{@code .plusDays(1)} 를 쓸 것.
     */
    @Deprecated(since = "2.1.0")
    public static LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }

    // ========================================
    // 날짜 비교
    // ========================================

    /**
     * 첫 번째 날짜가 두 번째 날짜보다 이후인지 확인
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDateTime start = LocalDateTime.of(2024, 3, 1, 0, 0);
     * LocalDateTime end = LocalDateTime.of(2024, 2, 1, 0, 0);
     *
     * TimeUtils.isAfter(start, end); // true
     * }</pre>
     *
     * @param first  첫 번째 날짜/시간
     * @param second 두 번째 날짜/시간
     * @return first가 second보다 이후이면 true
     */
    public static boolean isAfter(LocalDateTime first, LocalDateTime second) {
        return first.isAfter(second);
    }

    /**
     * 첫 번째 날짜가 두 번째 날짜보다 이후인지 확인 (LocalDate 버전)
     *
     * @param first  첫 번째 날짜
     * @param second 두 번째 날짜
     * @return first가 second보다 이후이면 true
     */
    public static boolean isAfter(LocalDate first, LocalDate second) {
        return first.isAfter(second);
    }

    /**
     * 첫 번째 날짜가 두 번째 날짜보다 이전인지 확인
     *
     * @param first  첫 번째 날짜/시간
     * @param second 두 번째 날짜/시간
     * @return first가 second보다 이전이면 true
     */
    public static boolean isBefore(LocalDateTime first, LocalDateTime second) {
        return first.isBefore(second);
    }

    /**
     * 첫 번째 날짜가 두 번째 날짜보다 이전인지 확인 (LocalDate 버전)
     *
     * @param first  첫 번째 날짜
     * @param second 두 번째 날짜
     * @return first가 second보다 이전이면 true
     */
    public static boolean isBefore(LocalDate first, LocalDate second) {
        return first.isBefore(second);
    }

    /**
     * 두 날짜가 같은 날인지 확인
     * <p>
     * 시간은 무시하고 년/월/일만 비교합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDateTime dt1 = LocalDateTime.of(2024, 1, 15, 10, 30);
     * LocalDateTime dt2 = LocalDateTime.of(2024, 1, 15, 18, 45);
     *
     * TimeUtils.isSameDay(dt1, dt2); // true (시간 무시)
     * }</pre>
     *
     * @param first  첫 번째 날짜/시간
     * @param second 두 번째 날짜/시간
     * @return 같은 날이면 true
     */
    public static boolean isSameDay(LocalDateTime first, LocalDateTime second) {
        return first.toLocalDate().equals(second.toLocalDate());
    }

    /**
     * 두 날짜가 같은 날인지 확인 (LocalDate 버전)
     *
     * @param first  첫 번째 날짜
     * @param second 두 번째 날짜
     * @return 같은 날이면 true
     */
    public static boolean isSameDay(LocalDate first, LocalDate second) {
        return first.equals(second);
    }

    /**
     * 날짜가 특정 범위 내에 있는지 확인 (경계 포함)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate target = LocalDate.of(2024, 1, 15);
     * LocalDate start = LocalDate.of(2024, 1, 1);
     * LocalDate end = LocalDate.of(2024, 1, 31);
     *
     * TimeUtils.isBetween(target, start, end); // true
     * }</pre>
     *
     * @param target 확인할 날짜
     * @param start  시작 날짜 (포함)
     * @param end    종료 날짜 (포함)
     * @return 범위 내에 있으면 true
     */
    public static boolean isBetween(LocalDate target, LocalDate start, LocalDate end) {
        return !target.isBefore(start) && !target.isAfter(end);
    }

    /**
     * 날짜/시간이 특정 범위 내에 있는지 확인 (경계 포함)
     *
     * @param target 확인할 날짜/시간
     * @param start  시작 날짜/시간 (포함)
     * @param end    종료 날짜/시간 (포함)
     * @return 범위 내에 있으면 true
     */
    public static boolean isBetween(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        return !target.isBefore(start) && !target.isAfter(end);
    }

    // ========================================
    // 주말/평일 확인
    // ========================================

    /**
     * 주말(토/일)인지 확인
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 주말 제외한 날짜만 필터링
     * List<LocalDate> weekdays = dates.stream()
     *     .filter(date -> !TimeUtils.isWeekend(date))
     *     .toList();
     * }</pre>
     *
     * @param date 확인할 날짜
     * @return 토요일 또는 일요일이면 true
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * 주말(토/일)인지 확인 (LocalDateTime 버전)
     *
     * @param dateTime 확인할 날짜/시간
     * @return 토요일 또는 일요일이면 true
     */
    public static boolean isWeekend(LocalDateTime dateTime) {
        return isWeekend(dateTime.toLocalDate());
    }

    /**
     * 평일(월~금)인지 확인
     *
     * @param date 확인할 날짜
     * @return 월~금요일이면 true
     */
    public static boolean isWeekday(LocalDate date) {
        return !isWeekend(date);
    }

    /**
     * 평일(월~금)인지 확인 (LocalDateTime 버전)
     *
     * @param dateTime 확인할 날짜/시간
     * @return 월~금요일이면 true
     */
    public static boolean isWeekday(LocalDateTime dateTime) {
        return !isWeekend(dateTime);
    }

    // ========================================
    // 시작/끝 시간
    // ========================================

    /**
     * 해당 날짜의 시작 시간 (00:00:00)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate date = LocalDate.of(2024, 1, 15);
     * LocalDateTime start = TimeUtils.startOfDay(date);
     * // 결과: 2024-01-15T00:00:00
     * }</pre>
     *
     * @param date 날짜
     * @return 해당 날짜 00:00:00
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 해당 날짜의 끝 시간 (23:59:59.999999999)
     *
     * @param date 날짜
     * @return 해당 날짜 23:59:59.999999999
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 해당 주의 시작일 (월요일)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate date = LocalDate.of(2024, 1, 17); // 수요일
     * LocalDate weekStart = TimeUtils.startOfWeek(date);
     * // 결과: 2024-01-15 (월요일)
     * }</pre>
     *
     * @param date 기준 날짜
     * @return 해당 주의 월요일
     */
    public static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * 해당 주의 종료일 (일요일)
     *
     * @param date 기준 날짜
     * @return 해당 주의 일요일
     */
    public static LocalDate endOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * 해당 월의 시작일 (1일)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate date = LocalDate.of(2024, 1, 17);
     * LocalDate monthStart = TimeUtils.startOfMonth(date);
     * // 결과: 2024-01-01
     * }</pre>
     *
     * @param date 기준 날짜
     * @return 해당 월의 1일
     */
    public static LocalDate startOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * 해당 월의 마지막일
     *
     * @param date 기준 날짜
     * @return 해당 월의 마지막 날
     */
    public static LocalDate endOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * 해당 년의 시작일 (1월 1일)
     *
     * @param date 기준 날짜
     * @return 해당 년의 1월 1일
     */
    public static LocalDate startOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfYear());
    }

    /**
     * 해당 년의 마지막일 (12월 31일)
     *
     * @param date 기준 날짜
     * @return 해당 년의 12월 31일
     */
    public static LocalDate endOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    // ========================================
    // 기간 계산
    // ========================================

    /**
     * 두 날짜 사이의 일수 계산
     * <p>
     * 시작일과 종료일 모두 제외하고 계산합니다. (종료일 - 시작일)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate start = LocalDate.of(2024, 1, 1);
     * LocalDate end = LocalDate.of(2024, 1, 10);
     *
     * long days = TimeUtils.daysBetween(start, end);
     * // 결과: 9
     * }</pre>
     *
     * @param start 시작일
     * @param end   종료일
     * @return 두 날짜 사이의 일수 (음수 가능)
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 두 날짜/시간 사이의 일수 계산
     *
     * @param start 시작 날짜/시간
     * @param end   종료 날짜/시간
     * @return 두 날짜 사이의 일수 (음수 가능)
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 두 날짜 사이의 월수 계산
     *
     * @param start 시작일
     * @param end   종료일
     * @return 두 날짜 사이의 월수 (음수 가능)
     */
    public static long monthsBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.MONTHS.between(start, end);
    }

    /**
     * 두 날짜 사이의 년수 계산
     *
     * @param start 시작일
     * @param end   종료일
     * @return 두 날짜 사이의 년수 (음수 가능)
     */
    public static long yearsBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.YEARS.between(start, end);
    }

    /**
     * 생년월일로 만 나이 계산
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate birthDate = LocalDate.of(1990, 5, 15);
     * int age = TimeUtils.calculateAge(birthDate);
     * }</pre>
     *
     * @param birthDate 생년월일
     * @return 만 나이
     */
    public static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * 특정 기준일 기준으로 만 나이 계산
     *
     * @param birthDate     생년월일
     * @param referenceDate 기준일
     * @return 만 나이
     */
    public static int calculateAge(LocalDate birthDate, LocalDate referenceDate) {
        return Period.between(birthDate, referenceDate).getYears();
    }

    // ========================================
    // 날짜 범위 조회
    // ========================================

    /**
     * 두 날짜 사이의 모든 날짜 목록 조회
     * <p>
     * 시작일과 종료일을 포함한 모든 날짜를 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate start = LocalDate.of(2024, 1, 1);
     * LocalDate end = LocalDate.of(2024, 1, 5);
     *
     * List<LocalDate> dates = TimeUtils.getDateRange(start, end);
     * // 결과: [2024-01-01, 2024-01-02, 2024-01-03, 2024-01-04, 2024-01-05]
     * }</pre>
     *
     * @param start 시작일 (포함)
     * @param end   종료일 (포함)
     * @return 시작일부터 종료일까지의 모든 날짜 목록
     * @throws InvalidValueException 시작일이 종료일보다 이후인 경우
     */
    public static List<LocalDate> getDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            log.warn("Start date is after end date. start: {}, end: {}", start, end);
            throw new InvalidValueException(ErrorCode.INVALID_DATE_RANGE);
        }
        log.debug("Getting date range from {} to {}", start, end);
        return start.datesUntil(end.plusDays(1)).collect(Collectors.toList());
    }

    /**
     * 두 날짜/시간 사이의 모든 날짜 목록 조회
     * <p>
     * 시간 부분은 무시하고 날짜만 반환합니다.
     *
     * @param start 시작 날짜/시간 (포함)
     * @param end   종료 날짜/시간 (포함)
     * @return 시작일부터 종료일까지의 모든 날짜 목록
     * @throws InvalidValueException 시작일이 종료일보다 이후인 경우
     */
    public static List<LocalDate> getDateRange(LocalDateTime start, LocalDateTime end) {
        return getDateRange(start.toLocalDate(), end.toLocalDate());
    }

    /**
     * 날짜 범위에서 특정 요일에 해당하는 날짜만 필터링
     * <p>
     * 반복 일정, 정기 휴무 등을 계산할 때 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate start = LocalDate.of(2024, 1, 1);
     * LocalDate end = LocalDate.of(2024, 1, 31);
     *
     * // 월, 수, 금요일만 조회
     * int[] daysOfWeek = {1, 3, 5};
     * List<LocalDate> dates = TimeUtils.filterByDayOfWeek(start, end, daysOfWeek);
     *
     * // 주말만 조회 (토, 일)
     * int[] weekends = {6, 7};
     * List<LocalDate> weekendDates = TimeUtils.filterByDayOfWeek(start, end, weekends);
     * }</pre>
     *
     * @param start      시작일 (포함)
     * @param end        종료일 (포함)
     * @param daysOfWeek 요일 코드 배열 (1=월요일, 7=일요일)
     * @return 해당 요일에 해당하는 날짜 목록
     * @throws InvalidValueException 요일 코드가 1-7 범위를 벗어난 경우
     */
    public static List<LocalDate> filterByDayOfWeek(LocalDate start, LocalDate end, int[] daysOfWeek) {
        log.debug("Filtering dates from {} to {} by days of week", start, end);
        List<DayOfWeek> targetDays = new ArrayList<>();
        for (int day : daysOfWeek) {
            if (day < 1 || day > 7) {
                log.warn("Invalid day of week: {}", day);
                throw new InvalidValueException(ErrorCode.INVALID_PARAMETER);
            }
            targetDays.add(DayOfWeek.of(day));
        }

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (targetDays.contains(current.getDayOfWeek())) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }

        return dates;
    }

    /**
     * 날짜/시간 범위에서 특정 요일에 해당하는 날짜만 필터링
     *
     * @param start      시작 날짜/시간 (포함)
     * @param end        종료 날짜/시간 (포함)
     * @param daysOfWeek 요일 코드 배열 (1=월요일, 7=일요일)
     * @return 해당 요일에 해당하는 날짜 목록
     * @throws InvalidValueException 요일 코드가 1-7 범위를 벗어난 경우
     */
    public static List<LocalDate> filterByDayOfWeek(LocalDateTime start, LocalDateTime end, int[] daysOfWeek) {
        return filterByDayOfWeek(start.toLocalDate(), end.toLocalDate(), daysOfWeek);
    }

    // ========================================
    // 날짜 집합 연산
    // ========================================

    /**
     * 두 날짜 목록을 합집합으로 병합
     * <p>
     * 중복된 날짜는 하나만 포함됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * List<LocalDate> list1 = List.of(
     *     LocalDate.of(2024, 1, 1),
     *     LocalDate.of(2024, 1, 2)
     * );
     * List<LocalDate> list2 = List.of(
     *     LocalDate.of(2024, 1, 2),
     *     LocalDate.of(2024, 1, 3)
     * );
     *
     * List<LocalDate> merged = TimeUtils.mergeDates(list1, list2);
     * // 결과: [2024-01-01, 2024-01-02, 2024-01-03] (순서 무관)
     * }</pre>
     *
     * @param first  첫 번째 날짜 목록
     * @param second 두 번째 날짜 목록
     * @return 합집합 날짜 목록 (중복 제거됨)
     */
    public static List<LocalDate> mergeDates(List<LocalDate> first, List<LocalDate> second) {
        Set<LocalDate> merged = new HashSet<>(first);
        merged.addAll(second);
        return new ArrayList<>(merged);
    }

    /**
     * 원본 날짜 목록에서 특정 날짜들을 제외
     * <p>
     * 차집합 연산을 수행합니다. (source - exclude)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 휴가 신청일에서 공휴일 제외
     * List<LocalDate> requestedDates = List.of(
     *     LocalDate.of(2024, 1, 1),
     *     LocalDate.of(2024, 1, 2),
     *     LocalDate.of(2024, 1, 3)
     * );
     * List<LocalDate> holidays = List.of(
     *     LocalDate.of(2024, 1, 1)  // 신정
     * );
     *
     * List<LocalDate> workDates = TimeUtils.excludeDates(requestedDates, holidays);
     * // 결과: [2024-01-02, 2024-01-03]
     * }</pre>
     *
     * @param source  원본 날짜 목록
     * @param exclude 제외할 날짜 목록
     * @return 차집합 날짜 목록
     */
    public static List<LocalDate> excludeDates(List<LocalDate> source, List<LocalDate> exclude) {
        Set<LocalDate> result = new HashSet<>(source);
        result.removeAll(new HashSet<>(exclude));
        return new ArrayList<>(result);
    }

    // ========================================
    // 최대/최소 찾기
    // ========================================

    /**
     * 날짜/시간 목록에서 가장 늦은(최대) 값 조회
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * List<LocalDateTime> loginTimes = List.of(
     *     LocalDateTime.of(2024, 1, 1, 10, 0),
     *     LocalDateTime.of(2024, 1, 2, 15, 30),
     *     LocalDateTime.of(2024, 1, 1, 20, 0)
     * );
     *
     * LocalDateTime lastLogin = TimeUtils.latest(loginTimes);
     * // 결과: 2024-01-02T15:30
     * }</pre>
     *
     * @param dateTimes 날짜/시간 목록
     * @return 가장 늦은 날짜/시간
     * @throws InvalidValueException 목록이 null이거나 비어있는 경우
     */
    public static LocalDateTime latest(List<LocalDateTime> dateTimes) {
        if (dateTimes == null || dateTimes.isEmpty()) {
            log.warn("DateTime list is null or empty");
            throw new InvalidValueException(ErrorCode.INVALID_PARAMETER);
        }

        return dateTimes.stream()
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new InvalidValueException(ErrorCode.INVALID_PARAMETER));
    }

    /**
     * 날짜 목록에서 가장 늦은(최대) 값 조회
     *
     * @param dates 날짜 목록
     * @return 가장 늦은 날짜
     * @throws InvalidValueException 목록이 null이거나 비어있는 경우
     */
    public static LocalDate latestDate(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            log.warn("Date list is null or empty");
            throw new InvalidValueException(ErrorCode.INVALID_PARAMETER);
        }

        return dates.stream()
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new InvalidValueException(ErrorCode.INVALID_PARAMETER));
    }

    /**
     * 날짜/시간 목록에서 가장 이른(최소) 값 조회
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * List<LocalDateTime> createdTimes = List.of(
     *     LocalDateTime.of(2024, 1, 15, 10, 0),
     *     LocalDateTime.of(2024, 1, 1, 9, 0),
     *     LocalDateTime.of(2024, 1, 10, 14, 0)
     * );
     *
     * LocalDateTime firstCreated = TimeUtils.earliest(createdTimes);
     * // 결과: 2024-01-01T09:00
     * }</pre>
     *
     * @param dateTimes 날짜/시간 목록
     * @return 가장 이른 날짜/시간
     * @throws InvalidValueException 목록이 null이거나 비어있는 경우
     */
    public static LocalDateTime earliest(List<LocalDateTime> dateTimes) {
        if (dateTimes == null || dateTimes.isEmpty()) {
            log.warn("DateTime list is null or empty");
            throw new InvalidValueException(ErrorCode.INVALID_PARAMETER);
        }

        return dateTimes.stream()
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new InvalidValueException(ErrorCode.INVALID_PARAMETER));
    }

    /**
     * 날짜 목록에서 가장 이른(최소) 값 조회
     *
     * @param dates 날짜 목록
     * @return 가장 이른 날짜
     * @throws InvalidValueException 목록이 null이거나 비어있는 경우
     */
    public static LocalDate earliestDate(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            log.warn("Date list is null or empty");
            throw new InvalidValueException(ErrorCode.INVALID_PARAMETER);
        }

        return dates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new InvalidValueException(ErrorCode.INVALID_PARAMETER));
    }

    // ========================================
    // 포맷팅 / 파싱
    // ========================================

    /**
     * 날짜를 지정된 포맷으로 변환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate date = LocalDate.of(2024, 1, 15);
     *
     * TimeUtils.format(date, TimeUtils.ISO_DATE);      // "2024-01-15"
     * TimeUtils.format(date, TimeUtils.KOREAN_DATE);   // "2024년 01월 15일"
     * TimeUtils.format(date, TimeUtils.COMPACT_DATE);  // "20240115"
     * }</pre>
     *
     * @param date      날짜
     * @param formatter 포맷터
     * @return 포맷된 문자열
     */
    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return date.format(formatter);
    }

    /**
     * 날짜/시간을 지정된 포맷으로 변환
     *
     * @param dateTime  날짜/시간
     * @param formatter 포맷터
     * @return 포맷된 문자열
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime.format(formatter);
    }

    /**
     * 날짜를 ISO 포맷(yyyy-MM-dd)으로 변환
     *
     * @param date 날짜
     * @return ISO 포맷 문자열
     */
    public static String formatIso(LocalDate date) {
        return date.format(ISO_DATE);
    }

    /**
     * 날짜/시간을 ISO 포맷(yyyy-MM-dd'T'HH:mm:ss)으로 변환
     *
     * @param dateTime 날짜/시간
     * @return ISO 포맷 문자열
     */
    public static String formatIso(LocalDateTime dateTime) {
        return dateTime.format(ISO_DATE_TIME);
    }

    /**
     * 문자열을 날짜로 파싱
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate date = TimeUtils.parseDate("2024-01-15", TimeUtils.ISO_DATE);
     * LocalDate date2 = TimeUtils.parseDate("20240115", TimeUtils.COMPACT_DATE);
     * }</pre>
     *
     * @param text      날짜 문자열
     * @param formatter 포맷터
     * @return 파싱된 날짜
     * @throws InvalidValueException 파싱 실패 시
     */
    public static LocalDate parseDate(String text, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(text, formatter);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {} with formatter: {}", text, formatter);
            throw new InvalidValueException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    /**
     * 문자열을 날짜/시간으로 파싱
     *
     * @param text      날짜/시간 문자열
     * @param formatter 포맷터
     * @return 파싱된 날짜/시간
     * @throws InvalidValueException 파싱 실패 시
     */
    public static LocalDateTime parseDateTime(String text, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(text, formatter);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse datetime: {} with formatter: {}", text, formatter);
            throw new InvalidValueException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    /**
     * ISO 포맷(yyyy-MM-dd) 문자열을 날짜로 파싱
     *
     * @param text ISO 포맷 날짜 문자열
     * @return 파싱된 날짜
     * @throws InvalidValueException 파싱 실패 시
     */
    public static LocalDate parseIsoDate(String text) {
        return parseDate(text, ISO_DATE);
    }

    /**
     * ISO 포맷(yyyy-MM-dd'T'HH:mm:ss) 문자열을 날짜/시간으로 파싱
     *
     * @param text ISO 포맷 날짜/시간 문자열
     * @return 파싱된 날짜/시간
     * @throws InvalidValueException 파싱 실패 시
     */
    public static LocalDateTime parseIsoDateTime(String text) {
        return parseDateTime(text, ISO_DATE_TIME);
    }

    // ========================================
    // 기존 메소드 호환성 유지 (Deprecated)
    // ========================================

    /**
     * @deprecated {@link #isAfter(LocalDateTime, LocalDateTime)} 사용
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static boolean isAfterThanEndDate(LocalDateTime start, LocalDateTime end) {
        return isAfter(start, end);
    }

    /**
     * @deprecated {@link #getDateRange(LocalDateTime, LocalDateTime)} 사용
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static List<LocalDate> getBetweenDates(LocalDateTime start, LocalDateTime end) {
        return getDateRange(start, end);
    }

    /**
     * @deprecated {@link #filterByDayOfWeek(LocalDateTime, LocalDateTime, int[])} 사용
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static List<LocalDate> getBetweenDatesByDayOfWeek(LocalDateTime start, LocalDateTime end, int[] daysOfWeek) {
        return filterByDayOfWeek(start, end, daysOfWeek);
    }

    /**
     * @deprecated {@link #mergeDates(List, List)} 사용
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static List<LocalDate> addAllDates(List<LocalDate> sourceDates, List<LocalDate> targetDates) {
        return mergeDates(sourceDates, targetDates);
    }

    /**
     * @deprecated {@link #excludeDates(List, List)} 사용
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static List<LocalDate> removeAllDates(List<LocalDate> sourceDates, List<LocalDate> targetDates) {
        return excludeDates(sourceDates, targetDates);
    }

    /**
     * @deprecated {@link #latest(List)} 사용
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static LocalDateTime findMaxDateTime(List<LocalDateTime> dateTimes) {
        return latest(dateTimes);
    }

    /**
     * @deprecated {@link #earliest(List)} 사용
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static LocalDateTime findMinDateTime(List<LocalDateTime> dateTimes) {
        return earliest(dateTimes);
    }
}
