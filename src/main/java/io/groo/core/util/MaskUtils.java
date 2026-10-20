package io.groo.core.util;

/**
 * 문자열 마스킹 유틸리티
 * <p>
 * 개인정보 보호를 위해 민감한 정보를 마스킹 처리합니다.
 * 로깅, API 응답, 화면 표시 등에서 개인정보 노출을 방지합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 이메일 마스킹
 * String masked = MaskUtils.email("hong@example.com");
 * // 결과: "hon***@example.com"
 *
 * // 전화번호 마스킹
 * String masked = MaskUtils.phone("010-1234-5678");
 * // 결과: "010-****-5678"
 *
 * // 이름 마스킹
 * String masked = MaskUtils.name("홍길동");
 * // 결과: "홍*동"
 *
 * // 주민등록번호 마스킹
 * String masked = MaskUtils.ssn("900101-1234567");
 * // 결과: "900101-1******"
 *
 * // 카드번호 마스킹
 * String masked = MaskUtils.cardNumber("1234-5678-9012-3456");
 * // 결과: "1234-****-****-3456"
 * }</pre>
 *
 * <h3>로깅 활용 예시</h3>
 * <pre>{@code
 * log.info("사용자 정보 조회 - 이름: {}, 이메일: {}, IP: {}",
 *     MaskUtils.name(user.getName()),
 *     MaskUtils.email(user.getEmail()),
 *     MaskUtils.ip(clientIp));
 * }</pre>
 *
 * <h3>지원 마스킹 유형</h3>
 * <table border="1">
 *   <tr><th>유형</th><th>메서드</th><th>보존 범위</th></tr>
 *   <tr><td>이메일</td><td>email()</td><td>앞 3자 + 도메인</td></tr>
 *   <tr><td>전화번호</td><td>phone()</td><td>앞 3자리 + 뒤 4자리</td></tr>
 *   <tr><td>이름</td><td>name()</td><td>첫 글자 + 마지막 글자</td></tr>
 *   <tr><td>카드번호</td><td>cardNumber()</td><td>앞 4자리 + 뒤 4자리</td></tr>
 *   <tr><td>주민번호</td><td>ssn()</td><td>생년월일 + 성별코드</td></tr>
 *   <tr><td>계좌번호</td><td>accountNumber()</td><td>앞 3자리 + 뒤 4자리</td></tr>
 *   <tr><td>주소</td><td>address()</td><td>시/도/구 까지</td></tr>
 *   <tr><td>IP 주소</td><td>ip()</td><td>앞 3옥텟</td></tr>
 * </table>
 *
 * @author groo
 */
public final class MaskUtils {

    private static final String MASK_CHAR = "*";

    private MaskUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 이메일 마스킹
     * <p>
     * 이메일의 로컬 파트(@ 앞부분)를 마스킹합니다.
     * 앞 3자는 보존하고 나머지를 마스킹합니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"hong@example.com" → "hon***@example.com"</li>
     *   <li>"ab@example.com" → "ab***@example.com"</li>
     *   <li>"a@example.com" → "a***@example.com"</li>
     *   <li>null → null</li>
     *   <li>"invalid" → "invalid" (@ 없으면 원본 반환)</li>
     * </ul>
     *
     * @param email 원본 이메일
     * @return 마스킹된 이메일, null이나 유효하지 않은 형식이면 원본 반환
     */
    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf("@");
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (local.length() <= 3) {
            return local + "***" + domain;
        }

        return local.substring(0, 3) + "***" + domain;
    }

    /**
     * 전화번호 마스킹
     * <p>
     * 전화번호의 중간 부분을 마스킹합니다.
     * 하이픈 유무에 관계없이 처리합니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"010-1234-5678" → "010-****-5678"</li>
     *   <li>"02-123-4567" → "02-***-4567"</li>
     *   <li>"01012345678" → "010****5678"</li>
     *   <li>null → null</li>
     *   <li>"1234567" → "1234567" (8자 미만은 원본 반환)</li>
     * </ul>
     *
     * @param phone 원본 전화번호
     * @return 마스킹된 전화번호, null이거나 8자 미만이면 원본 반환
     */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }

        // 하이픈이 있는 경우
        if (phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length >= 3) {
                String middle = MASK_CHAR.repeat(parts[1].length());
                return parts[0] + "-" + middle + "-" + parts[2];
            } else if (parts.length == 2) {
                String middle = MASK_CHAR.repeat(Math.min(parts[0].length(), 4));
                return middle + "-" + parts[1];
            }
        }

        // 하이픈이 없는 경우 (중간 4자리 마스킹)
        int length = phone.length();
        int maskStart = length > 7 ? 3 : 2;
        int maskEnd = length - 4;

        if (maskEnd <= maskStart) {
            return phone;
        }

        return phone.substring(0, maskStart) +
                MASK_CHAR.repeat(maskEnd - maskStart) +
                phone.substring(maskEnd);
    }

    /**
     * 이름 마스킹
     * <p>
     * 이름의 중간 글자를 마스킹합니다.
     * 첫 글자와 마지막 글자만 보존합니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"홍길동" → "홍*동"</li>
     *   <li>"김철수" → "김*수"</li>
     *   <li>"남궁민수" → "남**수"</li>
     *   <li>"이" → "이" (1글자는 마스킹 안함)</li>
     *   <li>"이순" → "이*"</li>
     *   <li>null → null</li>
     * </ul>
     *
     * @param name 원본 이름
     * @return 마스킹된 이름, null이거나 1글자면 원본 반환
     */
    public static String name(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }

        if (name.length() == 2) {
            return name.charAt(0) + MASK_CHAR;
        }

        // 3글자 이상: 첫 글자와 마지막 글자만 보존
        return name.charAt(0) +
                MASK_CHAR.repeat(name.length() - 2) +
                name.charAt(name.length() - 1);
    }

    /**
     * 카드번호 마스킹
     * <p>
     * 카드번호의 중간 8자리를 마스킹합니다.
     * 앞 4자리와 뒤 4자리만 보존합니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"1234-5678-9012-3456" → "1234-****-****-3456"</li>
     *   <li>"1234567890123456" → "1234********3456"</li>
     *   <li>null → null</li>
     *   <li>"12345678" → "12345678" (12자 미만은 원본 반환)</li>
     * </ul>
     *
     * @param cardNumber 원본 카드번호
     * @return 마스킹된 카드번호, null이거나 12자 미만이면 원본 반환
     */
    public static String cardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }

        // 하이픈 제거
        String cleanNumber = cardNumber.replaceAll("-", "");

        if (cleanNumber.length() < 12) {
            return cardNumber;
        }

        // 원본 형식 유지 (하이픈 포함 여부)
        if (cardNumber.contains("-")) {
            String[] parts = cardNumber.split("-");
            if (parts.length == 4) {
                return parts[0] + "-****-****-" + parts[3];
            }
        }

        // 하이픈 없는 경우
        return cleanNumber.substring(0, 4) +
                MASK_CHAR.repeat(cleanNumber.length() - 8) +
                cleanNumber.substring(cleanNumber.length() - 4);
    }

    /**
     * 주민등록번호 마스킹
     * <p>
     * 주민등록번호의 뒷자리를 마스킹합니다.
     * 생년월일(앞 6자리)과 성별 코드(뒷자리 첫 1자리)만 보존합니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"900101-1234567" → "900101-1******"</li>
     *   <li>"9001011234567" → "900101-1******"</li>
     *   <li>null → null</li>
     *   <li>"90010112345" → "90010112345" (13자리 아니면 원본 반환)</li>
     * </ul>
     *
     * @param ssn 원본 주민등록번호
     * @return 마스킹된 주민등록번호, null이거나 형식이 맞지 않으면 원본 반환
     */
    public static String ssn(String ssn) {
        if (ssn == null) {
            return null;
        }

        // 하이픈 제거
        String cleanSsn = ssn.replaceAll("-", "");

        if (cleanSsn.length() != 13) {
            return ssn;
        }

        return cleanSsn.substring(0, 6) + "-" + cleanSsn.charAt(6) + "******";
    }

    /**
     * 계좌번호 마스킹
     * <p>
     * 계좌번호의 중간 부분을 마스킹합니다.
     * 앞 3자리와 뒤 4자리만 보존합니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"110-123-456789" → "110*******6789"</li>
     *   <li>"1101234567890" → "110*******7890"</li>
     *   <li>null → null</li>
     *   <li>"1234567" → "1234567" (8자 미만은 원본 반환)</li>
     * </ul>
     *
     * @param accountNumber 원본 계좌번호
     * @return 마스킹된 계좌번호, null이거나 8자 미만이면 원본 반환
     */
    public static String accountNumber(String accountNumber) {
        if (accountNumber == null) {
            return null;
        }

        // 하이픈 제거한 길이
        String cleanNumber = accountNumber.replaceAll("-", "");

        if (cleanNumber.length() < 8) {
            return accountNumber;
        }

        // 앞 3자리, 뒤 4자리 보존
        int maskLength = cleanNumber.length() - 7;

        return cleanNumber.substring(0, 3) +
                MASK_CHAR.repeat(maskLength) +
                cleanNumber.substring(cleanNumber.length() - 4);
    }

    /**
     * 주소 마스킹
     * <p>
     * 주소의 상세 부분을 마스킹합니다.
     * 시/도, 구/군까지만 보존하고 나머지는 마스킹합니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"서울시 강남구 역삼동 123-45" → "서울시 강남구 역삼동 *****"</li>
     *   <li>"경기도 성남시 분당구 정자동" → "경기도 성남시 분당구 ***"</li>
     *   <li>null → null</li>
     *   <li>"서울시" → "서울시" (2단어 이하면 원본 반환)</li>
     * </ul>
     *
     * @param address 원본 주소
     * @return 마스킹된 주소, null이거나 공백이면 원본 반환
     */
    public static String address(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }

        String[] parts = address.split(" ");

        if (parts.length <= 2) {
            return address;
        }

        // 시/도, 구/군 까지만 보존 (보통 2~3단어)
        StringBuilder result = new StringBuilder();
        int preserveCount = Math.min(3, parts.length - 1);

        for (int i = 0; i < preserveCount; i++) {
            result.append(parts[i]).append(" ");
        }

        result.append("*".repeat(5));

        return result.toString().trim();
    }

    /**
     * IP 주소 마스킹
     * <p>
     * IP 주소의 마지막 옥텟을 마스킹합니다.
     * 네트워크 대역은 보존하고 호스트 부분만 숨깁니다.
     *
     * <h4>예시</h4>
     * <ul>
     *   <li>"192.168.1.100" → "192.168.1.***"</li>
     *   <li>"10.0.0.1" → "10.0.0.*"</li>
     *   <li>null → null</li>
     *   <li>"invalid" → "invalid" (. 없으면 원본 반환)</li>
     * </ul>
     *
     * @param ip 원본 IP 주소
     * @return 마스킹된 IP 주소, null이거나 유효하지 않으면 원본 반환
     */
    public static String ip(String ip) {
        if (ip == null || !ip.contains(".")) {
            return ip;
        }

        int lastDot = ip.lastIndexOf(".");
        if (lastDot < 0) {
            return ip;
        }

        String lastOctet = ip.substring(lastDot + 1);
        return ip.substring(0, lastDot + 1) + MASK_CHAR.repeat(lastOctet.length());
    }

    /**
     * 커스텀 마스킹
     * <p>
     * 지정된 위치부터 지정된 길이만큼 마스킹합니다.
     * 다른 마스킹 메서드로 처리할 수 없는 경우 사용합니다.
     *
     * <h4>예시</h4>
     * <pre>{@code
     * MaskUtils.custom("ABCDEFGH", 2, 4);
     * // 결과: "AB****GH"
     *
     * MaskUtils.custom("LICENSE-12345", 8, 5);
     * // 결과: "LICENSE-*****"
     * }</pre>
     *
     * @param value      원본 문자열
     * @param startIndex 마스킹 시작 위치 (0부터 시작)
     * @param length     마스킹할 문자 개수
     * @return 마스킹된 문자열, null이거나 범위 초과 시 원본 반환
     */
    public static String custom(String value, int startIndex, int length) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (startIndex < 0 || startIndex >= value.length()) {
            return value;
        }

        int endIndex = Math.min(startIndex + length, value.length());

        return value.substring(0, startIndex) +
                MASK_CHAR.repeat(endIndex - startIndex) +
                value.substring(endIndex);
    }
}
