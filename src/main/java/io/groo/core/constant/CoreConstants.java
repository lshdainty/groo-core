package io.groo.core.constant;

/**
 * 공통 상수 정의
 * <p>
 * 프로젝트 전반에서 사용되는 상수들을 중앙 관리합니다.
 * 하드코딩을 방지하고 일관성을 유지하기 위해 사용합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 날짜 포맷 사용
 * String formatted = localDate.format(
 *     DateTimeFormatter.ofPattern(CoreConstants.DATE_FORMAT)
 * );
 *
 * // 페이지네이션 기본값 사용
 * int size = request.getSize() > 0 ? request.getSize() : CoreConstants.DEFAULT_PAGE_SIZE;
 *
 * // 정규식 패턴 사용
 * if (!email.matches(CoreConstants.EMAIL_PATTERN)) {
 *     throw new InvalidValueException(ErrorCode.INVALID_EMAIL_FORMAT);
 * }
 * }</pre>
 *
 * @author groo
 * @see io.groo.core.util.RegexPatterns
 */
public final class CoreConstants {

    private CoreConstants() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ========================================
    // 날짜/시간 포맷
    // ========================================

    /**
     * 날짜 포맷 (yyyy-MM-dd)
     * <p>
     * ISO 8601 표준 날짜 형식입니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDate date = LocalDate.now();
     * String formatted = date.format(DateTimeFormatter.ofPattern(CoreConstants.DATE_FORMAT));
     * // 결과: "2024-01-15"
     * }</pre>
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 날짜/시간 포맷 (yyyy-MM-dd HH:mm:ss)
     * <p>
     * 초 단위까지 포함하는 일반적인 날짜/시간 형식입니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * LocalDateTime dateTime = LocalDateTime.now();
     * String formatted = dateTime.format(DateTimeFormatter.ofPattern(CoreConstants.DATETIME_FORMAT));
     * // 결과: "2024-01-15 14:30:00"
     * }</pre>
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 날짜/시간 포맷 (밀리초 포함, yyyy-MM-dd HH:mm:ss.SSS)
     * <p>
     * 밀리초 단위까지 포함하는 정밀한 날짜/시간 형식입니다.
     * 로깅이나 정밀한 시간 기록에 사용합니다.
     */
    public static final String DATETIME_MILLIS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 시간 포맷 (HH:mm:ss)
     * <p>
     * 24시간제 시간 형식입니다.
     */
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 시간 포맷 (시:분만, HH:mm)
     * <p>
     * 초를 제외한 시간 형식입니다.
     */
    public static final String TIME_SHORT_FORMAT = "HH:mm";

    /**
     * ISO 8601 날짜/시간 포맷 (yyyy-MM-dd'T'HH:mm:ss)
     * <p>
     * API 통신에서 주로 사용되는 ISO 8601 형식입니다.
     */
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    // ========================================
    // 페이지네이션
    // ========================================

    /**
     * 기본 페이지 크기
     * <p>
     * 페이지네이션 요청 시 size가 지정되지 않았을 때 사용하는 기본값입니다.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 최대 페이지 크기
     * <p>
     * 성능 보호를 위해 한 번에 조회 가능한 최대 항목 수입니다.
     * 이 값을 초과하는 요청은 이 값으로 제한됩니다.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 기본 시작 페이지 번호
     * <p>
     * Spring Data의 페이지는 0부터 시작합니다.
     */
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // ========================================
    // 정규식 패턴
    // ========================================

    /**
     * 이메일 정규식 패턴
     * <p>
     * 기본적인 이메일 형식을 검증합니다.
     *
     * <h4>유효한 예시</h4>
     * <ul>
     *   <li>user@example.com</li>
     *   <li>user.name@example.co.kr</li>
     *   <li>user+tag@example.com</li>
     * </ul>
     */
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /**
     * 전화번호 정규식 패턴 (한국)
     * <p>
     * 하이픈(-)을 포함한 한국 전화번호 형식을 검증합니다.
     *
     * <h4>유효한 예시</h4>
     * <ul>
     *   <li>010-1234-5678</li>
     *   <li>02-123-4567</li>
     *   <li>031-1234-5678</li>
     * </ul>
     */
    public static final String PHONE_PATTERN = "^\\d{2,3}-\\d{3,4}-\\d{4}$";

    /**
     * 휴대전화 정규식 패턴 (한국)
     * <p>
     * 010, 011, 016, 017, 018, 019로 시작하는 휴대전화 번호를 검증합니다.
     */
    public static final String MOBILE_PATTERN = "^01[016789]-\\d{3,4}-\\d{4}$";

    /**
     * 비밀번호 정규식 패턴 (강력)
     * <p>
     * 최소 8자, 영문 대/소문자, 숫자, 특수문자를 모두 포함해야 합니다.
     *
     * <h4>유효한 예시</h4>
     * <ul>
     *   <li>Password1!</li>
     *   <li>Abcd1234@</li>
     * </ul>
     */
    public static final String PASSWORD_STRONG_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    /**
     * 비밀번호 정규식 패턴 (기본)
     * <p>
     * 최소 8자, 영문과 숫자를 포함해야 합니다.
     */
    public static final String PASSWORD_BASIC_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";

    /**
     * 숫자만 허용하는 정규식 패턴
     */
    public static final String NUMERIC_PATTERN = "^\\d+$";

    /**
     * 영문자만 허용하는 정규식 패턴
     */
    public static final String ALPHA_PATTERN = "^[A-Za-z]+$";

    /**
     * 영문자와 숫자만 허용하는 정규식 패턴
     */
    public static final String ALPHANUMERIC_PATTERN = "^[A-Za-z0-9]+$";

    // ========================================
    // HTTP 관련
    // ========================================

    /**
     * Authorization 헤더 이름
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Bearer 토큰 접두사
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Content-Type 헤더 이름
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * JSON Content-Type
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * Accept-Language 헤더 이름
     */
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

    // ========================================
    // 기타
    // ========================================

    /**
     * 빈 문자열
     */
    public static final String EMPTY = "";

    /**
     * 콤마 구분자
     */
    public static final String COMMA = ",";

    /**
     * 세미콜론 구분자
     */
    public static final String SEMICOLON = ";";

    /**
     * 파이프 구분자
     */
    public static final String PIPE = "|";

    /**
     * 줄바꿈 문자
     */
    public static final String NEW_LINE = "\n";

    /**
     * 시스템 사용자 이름
     * <p>
     * 배치 작업이나 시스템에서 자동으로 처리하는 작업의 생성자/수정자로 사용합니다.
     */
    public static final String SYSTEM_USER = "SYSTEM";
}
