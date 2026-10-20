package io.groo.core.type;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 국가 코드 Enum
 * <p>
 * ISO 3166-1 표준에 따른 국가 코드를 정의합니다.
 * Alpha-2 (2자리), Alpha-3 (3자리), Numeric (숫자) 코드를 모두 지원합니다.
 *
 * <h3>코드 형식</h3>
 * <table border="1">
 *   <tr><th>형식</th><th>예시 (한국)</th><th>설명</th></tr>
 *   <tr><td>Alpha-2</td><td>KR</td><td>2자리 알파벳 (Enum 이름)</td></tr>
 *   <tr><td>Alpha-3</td><td>KOR</td><td>3자리 알파벳</td></tr>
 *   <tr><td>Numeric</td><td>410</td><td>3자리 숫자</td></tr>
 * </table>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 기본 사용
 * CountryCode korea = CountryCode.KR;
 * String alpha2 = korea.getAlpha2();     // "KR"
 * String alpha3 = korea.getAlpha3();     // "KOR"
 * String numeric = korea.getNumeric();   // "410"
 *
 * // 문자열에서 변환
 * CountryCode code = CountryCode.from("KR");      // KR
 * CountryCode code = CountryCode.from("KOR");     // KR
 * CountryCode code = CountryCode.from("410");     // KR
 *
 * // 대륙별 필터링
 * List<CountryCode> asianCountries = CountryCode.byContinent("Asia");
 *
 * // 아시아 국가인지 확인
 * if (country.isAsia()) {
 *     // 아시아 국가 처리
 * }
 * }</pre>
 *
 * @author groo
 * @see DisplayType
 */
public enum CountryCode implements DisplayType {

    /**
     * 대한민국 (Republic of Korea)
     */
    KR("KOR", "410", "Asia", "대한민국"),

    /**
     * 미국 (United States of America)
     */
    US("USA", "840", "America", "미국"),

    /**
     * 일본 (Japan)
     */
    JP("JPN", "392", "Asia", "일본"),

    /**
     * 중국 (China)
     */
    CN("CHN", "156", "Asia", "중국"),

    /**
     * 베트남 (Vietnam)
     */
    VN("VNM", "704", "Asia", "베트남"),

    /**
     * 말레이시아 (Malaysia)
     */
    MY("MYS", "458", "Asia", "말레이시아"),

    /**
     * 폴란드 (Poland)
     */
    PL("POL", "616", "Europe", "폴란드");

    private static final String MESSAGE_KEY_PREFIX = "type.country.code.";

    /** Alpha-2/Alpha-3/Numeric 코드로 빠른 조회를 위한 캐시 */
    private static final Map<String, CountryCode> CODE_MAP;

    static {
        CODE_MAP = Arrays.stream(values())
                .flatMap(c -> java.util.stream.Stream.of(
                        Map.entry(c.name(), c),
                        Map.entry(c.alpha3, c),
                        Map.entry(c.numeric, c)
                ))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a  // 중복 시 첫 번째 유지
                ));
    }

    private final String alpha3;
    private final String numeric;
    private final String continent;
    private final String koreanName;

    CountryCode(String alpha3, String numeric, String continent, String koreanName) {
        this.alpha3 = alpha3;
        this.numeric = numeric;
        this.continent = continent;
        this.koreanName = koreanName;
    }

    // ========================================
    // DisplayType 구현
    // ========================================

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY_PREFIX + this.name().toLowerCase();
    }

    @Override
    public Long getOrderSeq() {
        return (long) this.ordinal();
    }

    // ========================================
    // Getter 메서드
    // ========================================

    /**
     * Alpha-2 코드 반환 (2자리)
     * <p>
     * ISO 3166-1 alpha-2 코드입니다. Enum 이름과 동일합니다.
     *
     * @return Alpha-2 코드 (예: "KR", "US", "JP")
     */
    public String getAlpha2() {
        return this.name();
    }

    /**
     * Alpha-3 코드 반환 (3자리)
     * <p>
     * ISO 3166-1 alpha-3 코드입니다.
     *
     * @return Alpha-3 코드 (예: "KOR", "USA", "JPN")
     */
    public String getAlpha3() {
        return alpha3;
    }

    /**
     * Numeric 코드 반환 (숫자)
     * <p>
     * ISO 3166-1 numeric 코드입니다.
     *
     * @return Numeric 코드 (예: "410", "840", "392")
     */
    public String getNumeric() {
        return numeric;
    }

    /**
     * 대륙 반환
     *
     * @return 대륙 (예: "Asia", "America", "Europe")
     */
    public String getContinent() {
        return continent;
    }

    /**
     * 한글 국가명 반환
     *
     * @return 한글 국가명 (예: "대한민국", "미국", "일본")
     */
    public String getKoreanName() {
        return koreanName;
    }

    // ========================================
    // 대륙별 판별 메서드
    // ========================================

    /**
     * 아시아 국가 여부 확인
     *
     * @return 아시아 국가이면 true
     */
    public boolean isAsia() {
        return "Asia".equals(continent);
    }

    /**
     * 아메리카 국가 여부 확인
     *
     * @return 아메리카 국가이면 true
     */
    public boolean isAmerica() {
        return "America".equals(continent);
    }

    /**
     * 유럽 국가 여부 확인
     *
     * @return 유럽 국가이면 true
     */
    public boolean isEurope() {
        return "Europe".equals(continent);
    }

    // ========================================
    // 특정 국가 판별 메서드
    // ========================================

    /**
     * 한국 여부 확인
     *
     * @return 한국이면 true
     */
    public boolean isKorea() {
        return this == KR;
    }

    /**
     * 미국 여부 확인
     *
     * @return 미국이면 true
     */
    public boolean isUnitedStates() {
        return this == US;
    }

    /**
     * 일본 여부 확인
     *
     * @return 일본이면 true
     */
    public boolean isJapan() {
        return this == JP;
    }

    /**
     * 중국 여부 확인
     *
     * @return 중국이면 true
     */
    public boolean isChina() {
        return this == CN;
    }

    // ========================================
    // 정적 변환 메서드
    // ========================================

    /**
     * 문자열로부터 CountryCode 변환
     * <p>
     * Alpha-2, Alpha-3, Numeric 코드 모두 지원합니다.
     * 대소문자를 구분하지 않습니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * CountryCode code = CountryCode.from("KR");    // KR
     * CountryCode code = CountryCode.from("kr");    // KR
     * CountryCode code = CountryCode.from("KOR");   // KR
     * CountryCode code = CountryCode.from("410");   // KR
     * CountryCode code = CountryCode.from("XXX");   // null
     * }</pre>
     *
     * @param code Alpha-2, Alpha-3, 또는 Numeric 코드
     * @return CountryCode, 매칭되지 않으면 null
     */
    public static CountryCode from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return CODE_MAP.get(code.toUpperCase().trim());
    }

    /**
     * 문자열로부터 CountryCode 변환 (기본값 지정)
     *
     * @param code         Alpha-2, Alpha-3, 또는 Numeric 코드
     * @param defaultValue 매칭되지 않을 경우 반환할 기본값
     * @return CountryCode
     */
    public static CountryCode from(String code, CountryCode defaultValue) {
        CountryCode result = from(code);
        return result != null ? result : defaultValue;
    }

    /**
     * 대륙별 국가 목록 반환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * List<CountryCode> asianCountries = CountryCode.byContinent("Asia");
     * // [KR, JP, CN, VN, MY]
     * }</pre>
     *
     * @param continent 대륙 (Asia, America, Europe)
     * @return 해당 대륙의 국가 목록
     */
    public static List<CountryCode> byContinent(String continent) {
        return Arrays.stream(values())
                .filter(c -> c.continent.equalsIgnoreCase(continent))
                .toList();
    }

    /**
     * 모든 아시아 국가 반환
     *
     * @return 아시아 국가 목록
     */
    public static List<CountryCode> asianCountries() {
        return byContinent("Asia");
    }

    /**
     * 모든 유럽 국가 반환
     *
     * @return 유럽 국가 목록
     */
    public static List<CountryCode> europeanCountries() {
        return byContinent("Europe");
    }

    /**
     * 모든 아메리카 국가 반환
     *
     * @return 아메리카 국가 목록
     */
    public static List<CountryCode> americanCountries() {
        return byContinent("America");
    }

    /**
     * 코드 존재 여부 확인
     *
     * @param code Alpha-2, Alpha-3, 또는 Numeric 코드
     * @return 존재하면 true
     */
    public static boolean exists(String code) {
        return from(code) != null;
    }
}
