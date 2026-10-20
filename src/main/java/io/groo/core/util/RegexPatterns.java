package io.groo.core.util;

import java.util.regex.Pattern;

/**
 * 정규식 패턴 유틸리티
 * <p>
 * 자주 사용되는 정규식 패턴을 미리 컴파일하여 제공합니다.
 * Pattern 객체를 재사용하여 성능을 최적화합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 이메일 검증
 * if (RegexPatterns.isValidEmail("hong@example.com")) {
 *     // 유효한 이메일
 * }
 *
 * // 전화번호 검증
 * if (RegexPatterns.isValidPhone("010-1234-5678")) {
 *     // 유효한 전화번호
 * }
 *
 * // 패턴 직접 사용
 * Matcher matcher = RegexPatterns.EMAIL.matcher(email);
 * if (matcher.matches()) {
 *     // 매칭됨
 * }
 * }</pre>
 *
 * <h3>패턴 상수 vs 검증 메서드</h3>
 * <ul>
 *   <li>단순 검증: {@code isValidXxx()} 메서드 사용</li>
 *   <li>그룹 추출 필요: {@code Pattern} 상수로 Matcher 생성</li>
 * </ul>
 *
 * @author groo
 * @see io.groo.core.constant.CoreConstants
 */
public final class RegexPatterns {

    private RegexPatterns() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ========================================
    // 이메일
    // ========================================

    /**
     * 이메일 패턴
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
    public static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * 이메일 유효성 검사
     *
     * @param email 검사할 이메일
     * @return 유효하면 true
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    // ========================================
    // 전화번호
    // ========================================

    /**
     * 전화번호 패턴 (한국, 하이픈 포함)
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
    public static final Pattern PHONE = Pattern.compile(
            "^\\d{2,3}-\\d{3,4}-\\d{4}$"
    );

    /**
     * 휴대전화 패턴 (한국)
     * <p>
     * 010, 011, 016, 017, 018, 019로 시작하는 휴대전화를 검증합니다.
     */
    public static final Pattern MOBILE = Pattern.compile(
            "^01[016789]-\\d{3,4}-\\d{4}$"
    );

    /**
     * 전화번호 패턴 (하이픈 없음)
     * <p>
     * 하이픈 없이 숫자만으로 이루어진 전화번호를 검증합니다.
     */
    public static final Pattern PHONE_PLAIN = Pattern.compile(
            "^\\d{9,11}$"
    );

    /**
     * 전화번호 유효성 검사 (하이픈 포함)
     *
     * @param phone 검사할 전화번호
     * @return 유효하면 true
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    /**
     * 휴대전화 유효성 검사
     *
     * @param mobile 검사할 휴대전화 번호
     * @return 유효하면 true
     */
    public static boolean isValidMobile(String mobile) {
        return mobile != null && MOBILE.matcher(mobile).matches();
    }

    // ========================================
    // 비밀번호
    // ========================================

    /**
     * 비밀번호 패턴 (강력)
     * <p>
     * 최소 8자, 영문 대/소문자, 숫자, 특수문자를 모두 포함해야 합니다.
     *
     * <h4>유효한 예시</h4>
     * <ul>
     *   <li>Password1!</li>
     *   <li>Abcd1234@</li>
     * </ul>
     */
    public static final Pattern PASSWORD_STRONG = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$"
    );

    /**
     * 비밀번호 패턴 (기본)
     * <p>
     * 최소 8자, 영문과 숫자를 포함해야 합니다.
     */
    public static final Pattern PASSWORD_BASIC = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&#]{8,}$"
    );

    /**
     * 강력한 비밀번호 유효성 검사
     *
     * @param password 검사할 비밀번호
     * @return 유효하면 true
     */
    public static boolean isValidStrongPassword(String password) {
        return password != null && PASSWORD_STRONG.matcher(password).matches();
    }

    /**
     * 기본 비밀번호 유효성 검사
     *
     * @param password 검사할 비밀번호
     * @return 유효하면 true
     */
    public static boolean isValidBasicPassword(String password) {
        return password != null && PASSWORD_BASIC.matcher(password).matches();
    }

    // ========================================
    // 숫자/문자
    // ========================================

    /**
     * 숫자만 패턴
     */
    public static final Pattern NUMERIC = Pattern.compile("^\\d+$");

    /**
     * 영문자만 패턴
     */
    public static final Pattern ALPHA = Pattern.compile("^[A-Za-z]+$");

    /**
     * 영문자와 숫자만 패턴
     */
    public static final Pattern ALPHANUMERIC = Pattern.compile("^[A-Za-z0-9]+$");

    /**
     * 한글만 패턴
     */
    public static final Pattern KOREAN = Pattern.compile("^[가-힣]+$");

    /**
     * 한글과 영문만 패턴
     */
    public static final Pattern KOREAN_OR_ALPHA = Pattern.compile("^[가-힣A-Za-z]+$");

    /**
     * 숫자만 포함되어 있는지 검사
     *
     * @param value 검사할 문자열
     * @return 숫자만 포함되어 있으면 true
     */
    public static boolean isNumeric(String value) {
        return value != null && NUMERIC.matcher(value).matches();
    }

    /**
     * 영문자만 포함되어 있는지 검사
     *
     * @param value 검사할 문자열
     * @return 영문자만 포함되어 있으면 true
     */
    public static boolean isAlpha(String value) {
        return value != null && ALPHA.matcher(value).matches();
    }

    /**
     * 영문자와 숫자만 포함되어 있는지 검사
     *
     * @param value 검사할 문자열
     * @return 영숫자만 포함되어 있으면 true
     */
    public static boolean isAlphanumeric(String value) {
        return value != null && ALPHANUMERIC.matcher(value).matches();
    }

    /**
     * 한글만 포함되어 있는지 검사
     *
     * @param value 검사할 문자열
     * @return 한글만 포함되어 있으면 true
     */
    public static boolean isKorean(String value) {
        return value != null && KOREAN.matcher(value).matches();
    }

    // ========================================
    // 신분증/계좌
    // ========================================

    /**
     * 주민등록번호 패턴 (하이픈 포함)
     * <p>
     * 000000-0000000 형식을 검증합니다.
     */
    public static final Pattern SSN = Pattern.compile(
            "^\\d{6}-[1-4]\\d{6}$"
    );

    /**
     * 사업자등록번호 패턴
     * <p>
     * 000-00-00000 형식을 검증합니다.
     */
    public static final Pattern BUSINESS_NUMBER = Pattern.compile(
            "^\\d{3}-\\d{2}-\\d{5}$"
    );

    /**
     * 신용카드번호 패턴 (하이픈 포함)
     * <p>
     * 0000-0000-0000-0000 형식을 검증합니다.
     */
    public static final Pattern CARD_NUMBER = Pattern.compile(
            "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$"
    );

    /**
     * 주민등록번호 유효성 검사
     *
     * @param ssn 검사할 주민등록번호
     * @return 형식이 유효하면 true (체크섬 검증 아님)
     */
    public static boolean isValidSsn(String ssn) {
        return ssn != null && SSN.matcher(ssn).matches();
    }

    /**
     * 사업자등록번호 유효성 검사
     *
     * @param number 검사할 사업자등록번호
     * @return 형식이 유효하면 true
     */
    public static boolean isValidBusinessNumber(String number) {
        return number != null && BUSINESS_NUMBER.matcher(number).matches();
    }

    // ========================================
    // 기타
    // ========================================

    /**
     * URL 패턴
     * <p>
     * HTTP/HTTPS URL 형식을 검증합니다.
     */
    public static final Pattern URL = Pattern.compile(
            "^https?://[A-Za-z0-9.-]+(?:/[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=-]*)?$"
    );

    /**
     * IPv4 주소 패턴
     */
    public static final Pattern IPV4 = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );

    /**
     * 우편번호 패턴 (한국, 5자리)
     */
    public static final Pattern POSTAL_CODE = Pattern.compile("^\\d{5}$");

    /**
     * URL 유효성 검사
     *
     * @param url 검사할 URL
     * @return 유효하면 true
     */
    public static boolean isValidUrl(String url) {
        return url != null && URL.matcher(url).matches();
    }

    /**
     * IPv4 주소 유효성 검사
     *
     * @param ip 검사할 IP 주소
     * @return 유효하면 true
     */
    public static boolean isValidIpv4(String ip) {
        return ip != null && IPV4.matcher(ip).matches();
    }

    /**
     * 우편번호 유효성 검사 (한국)
     *
     * @param postalCode 검사할 우편번호
     * @return 유효하면 true
     */
    public static boolean isValidPostalCode(String postalCode) {
        return postalCode != null && POSTAL_CODE.matcher(postalCode).matches();
    }
}
