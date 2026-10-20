package io.groo.core.type;

/**
 * 실행 환경 Enum
 * <p>
 * 애플리케이션의 실행 환경을 정의합니다.
 * Spring Profile과 매핑하여 사용합니다.
 *
 * <h3>환경별 특성</h3>
 * <table border="1">
 *   <tr><th>환경</th><th>용도</th><th>특성</th></tr>
 *   <tr><td>LOCAL</td><td>로컬 개발</td><td>개발자 PC, H2 DB</td></tr>
 *   <tr><td>DEV</td><td>개발 서버</td><td>개발 팀 공유 서버</td></tr>
 *   <tr><td>STG</td><td>스테이징</td><td>운영 환경과 동일 구성</td></tr>
 *   <tr><td>PROD</td><td>운영</td><td>실제 서비스 환경</td></tr>
 * </table>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 현재 환경 확인
 * @Value("${spring.profiles.active}")
 * private String activeProfile;
 *
 * Environment env = Environment.from(activeProfile);
 *
 * if (env.isProduction()) {
 *     // 운영 환경 전용 로직
 * }
 *
 * if (env.isDevelopment()) {
 *     // 개발 환경에서만 실행
 *     log.debug("Debug info: {}", data);
 * }
 *
 * // 환경별 설정
 * String apiUrl = switch (env) {
 *     case LOCAL -> "http://localhost:8080";
 *     case DEV -> "https://dev-api.example.com";
 *     case STG -> "https://stg-api.example.com";
 *     case PROD -> "https://api.example.com";
 * };
 * }</pre>
 *
 * @author groo
 */
public enum Environment {

    /**
     * 로컬 개발 환경
     * <p>
     * 개발자 PC에서 실행되는 환경입니다.
     * 보통 H2 인메모리 DB를 사용하고, 디버그 로깅이 활성화됩니다.
     */
    LOCAL("local", "로컬"),

    /**
     * 개발 서버 환경
     * <p>
     * 개발 팀이 공유하는 개발 서버입니다.
     * 개발용 DB를 사용하고, 테스트 데이터가 존재합니다.
     */
    DEV("dev", "개발"),

    /**
     * 스테이징 환경
     * <p>
     * 운영 환경과 동일하게 구성된 테스트 환경입니다.
     * QA 테스트 및 운영 배포 전 검증에 사용됩니다.
     */
    STG("stg", "스테이징"),

    /**
     * 운영 환경
     * <p>
     * 실제 서비스가 운영되는 환경입니다.
     * 최적화된 설정과 보안 설정이 적용됩니다.
     */
    PROD("prod", "운영");

    private final String profile;
    private final String description;

    Environment(String profile, String description) {
        this.profile = profile;
        this.description = description;
    }

    /**
     * Spring Profile 이름 반환
     *
     * @return profile 이름 (local, dev, stg, prod)
     */
    public String getProfile() {
        return profile;
    }

    /**
     * 한글 설명 반환
     *
     * @return 환경 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 운영 환경 여부 확인
     *
     * @return 운영 환경이면 true
     */
    public boolean isProduction() {
        return this == PROD;
    }

    /**
     * 개발 환경 여부 확인 (LOCAL 또는 DEV)
     *
     * @return LOCAL 또는 DEV면 true
     */
    public boolean isDevelopment() {
        return this == LOCAL || this == DEV;
    }

    /**
     * 로컬 환경 여부 확인
     *
     * @return LOCAL이면 true
     */
    public boolean isLocal() {
        return this == LOCAL;
    }

    /**
     * 스테이징 환경 여부 확인
     *
     * @return STG면 true
     */
    public boolean isStaging() {
        return this == STG;
    }

    /**
     * 비운영 환경 여부 확인 (LOCAL, DEV, STG)
     *
     * @return 운영이 아니면 true
     */
    public boolean isNonProduction() {
        return this != PROD;
    }

    /**
     * Profile 문자열로부터 Environment 변환
     * <p>
     * 대소문자를 구분하지 않습니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * Environment env = Environment.from("prod");     // PROD
     * Environment env = Environment.from("LOCAL");    // LOCAL
     * Environment env = Environment.from("unknown");  // LOCAL (기본값)
     * }</pre>
     *
     * @param profile Spring Profile 이름
     * @return Environment, 매칭되지 않으면 LOCAL 반환
     */
    public static Environment from(String profile) {
        if (profile == null || profile.isBlank()) {
            return LOCAL;
        }

        for (Environment env : values()) {
            if (env.name().equalsIgnoreCase(profile) ||
                    env.profile.equalsIgnoreCase(profile)) {
                return env;
            }
        }

        // production, develop 등 축약되지 않은 이름도 지원
        String normalized = profile.toLowerCase();
        if (normalized.startsWith("prod")) {
            return PROD;
        }
        if (normalized.startsWith("stag")) {
            return STG;
        }
        if (normalized.startsWith("dev")) {
            return DEV;
        }

        return LOCAL;
    }

    /**
     * Profile 문자열로부터 Environment 변환 (기본값 지정)
     *
     * @param profile      Spring Profile 이름
     * @param defaultValue 매칭되지 않을 경우 반환할 기본값
     * @return Environment
     */
    public static Environment from(String profile, Environment defaultValue) {
        if (profile == null || profile.isBlank()) {
            return defaultValue;
        }

        for (Environment env : values()) {
            if (env.name().equalsIgnoreCase(profile) ||
                    env.profile.equalsIgnoreCase(profile)) {
                return env;
            }
        }

        return defaultValue;
    }
}
