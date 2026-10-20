package io.groo.core.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 암호화/해싱 유틸리티
 * <p>
 * 문자열 해싱, Base64 인코딩/디코딩, 랜덤 문자열 생성 등의 기능을 제공합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>SHA-256, SHA-512, MD5 해싱</li>
 *   <li>Base64 인코딩/디코딩</li>
 *   <li>URL-Safe Base64 인코딩/디코딩</li>
 *   <li>랜덤 문자열/토큰 생성</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // SHA-256 해싱
 * String hashed = CryptoUtils.sha256("password");
 * // 결과: "5e884898da28047d1eb56bb..."
 *
 * // Base64 인코딩
 * String encoded = CryptoUtils.encodeBase64("Hello, World!");
 * String decoded = CryptoUtils.decodeBase64(encoded);
 *
 * // URL-Safe Base64 인코딩 (토큰 등에 사용)
 * String token = CryptoUtils.encodeBase64UrlSafe("user:123:timestamp");
 *
 * // 랜덤 토큰 생성
 * String token = CryptoUtils.generateRandomToken(32);
 * }</pre>
 *
 * <h3>보안 참고사항</h3>
 * <ul>
 *   <li>비밀번호 저장에는 이 클래스 대신 {@code PasswordEncoder}를 사용하세요.</li>
 *   <li>MD5는 보안 목적으로 사용하지 마세요 (체크섬 용도로만 권장).</li>
 *   <li>민감한 데이터 암호화에는 AES 등 대칭키 암호화를 사용하세요.</li>
 * </ul>
 *
 * @author groo
 */
@Slf4j
public final class CryptoUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private CryptoUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ========================================
    // 해싱
    // ========================================

    /**
     * SHA-256 해싱
     * <p>
     * 입력 문자열을 SHA-256 알고리즘으로 해싱합니다.
     * 256비트(64자) 16진수 문자열을 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String hashed = CryptoUtils.sha256("hello");
     * // 결과: "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
     * }</pre>
     *
     * @param input 해싱할 문자열
     * @return SHA-256 해시 값 (16진수), 실패 시 null
     */
    public static String sha256(String input) {
        return hash(input, "SHA-256");
    }

    /**
     * SHA-512 해싱
     * <p>
     * 입력 문자열을 SHA-512 알고리즘으로 해싱합니다.
     * 512비트(128자) 16진수 문자열을 반환합니다.
     *
     * @param input 해싱할 문자열
     * @return SHA-512 해시 값 (16진수), 실패 시 null
     */
    public static String sha512(String input) {
        return hash(input, "SHA-512");
    }

    /**
     * MD5 해싱
     * <p>
     * 입력 문자열을 MD5 알고리즘으로 해싱합니다.
     * 128비트(32자) 16진수 문자열을 반환합니다.
     * <p>
     * <strong>주의:</strong> MD5는 보안 목적으로 사용하지 마세요.
     * 파일 체크섬 등 무결성 확인 용도로만 사용하세요.
     *
     * @param input 해싱할 문자열
     * @return MD5 해시 값 (16진수), 실패 시 null
     */
    public static String md5(String input) {
        return hash(input, "MD5");
    }

    /**
     * 지정된 알고리즘으로 해싱
     */
    private static String hash(String input, String algorithm) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm not found: {}", algorithm, e);
            return null;
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // ========================================
    // Base64 인코딩/디코딩
    // ========================================

    /**
     * Base64 인코딩
     * <p>
     * 문자열을 Base64로 인코딩합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String encoded = CryptoUtils.encodeBase64("Hello, World!");
     * // 결과: "SGVsbG8sIFdvcmxkIQ=="
     * }</pre>
     *
     * @param input 인코딩할 문자열
     * @return Base64 인코딩된 문자열, null이면 null 반환
     */
    public static String encodeBase64(String input) {
        if (input == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 디코딩
     * <p>
     * Base64로 인코딩된 문자열을 디코딩합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String decoded = CryptoUtils.decodeBase64("SGVsbG8sIFdvcmxkIQ==");
     * // 결과: "Hello, World!"
     * }</pre>
     *
     * @param encoded Base64 인코딩된 문자열
     * @return 디코딩된 문자열, 실패 시 null
     */
    public static String decodeBase64(String encoded) {
        if (encoded == null) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encoded);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode Base64: {}", e.getMessage());
            return null;
        }
    }

    /**
     * URL-Safe Base64 인코딩
     * <p>
     * URL에서 안전하게 사용할 수 있는 Base64로 인코딩합니다.
     * '+' → '-', '/' → '_'로 대체되고, 패딩('=')이 제거됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String token = CryptoUtils.encodeBase64UrlSafe("user:123:data");
     * // URL 파라미터나 경로에 안전하게 사용 가능
     * }</pre>
     *
     * @param input 인코딩할 문자열
     * @return URL-Safe Base64 인코딩된 문자열
     */
    public static String encodeBase64UrlSafe(String input) {
        if (input == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL-Safe Base64 디코딩
     *
     * @param encoded URL-Safe Base64 인코딩된 문자열
     * @return 디코딩된 문자열, 실패 시 null
     */
    public static String decodeBase64UrlSafe(String encoded) {
        if (encoded == null) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode URL-Safe Base64: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 바이트 배열을 Base64로 인코딩
     *
     * @param bytes 인코딩할 바이트 배열
     * @return Base64 인코딩된 문자열
     */
    public static String encodeBase64(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Base64를 바이트 배열로 디코딩
     *
     * @param encoded Base64 인코딩된 문자열
     * @return 디코딩된 바이트 배열, 실패 시 null
     */
    public static byte[] decodeBase64ToBytes(String encoded) {
        if (encoded == null) {
            return null;
        }

        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode Base64 to bytes: {}", e.getMessage());
            return null;
        }
    }

    // ========================================
    // 랜덤 생성
    // ========================================

    /**
     * 랜덤 토큰 생성
     * <p>
     * 지정된 길이의 랜덤한 영숫자 문자열을 생성합니다.
     * 토큰, 인증 코드, 임시 비밀번호 등에 사용할 수 있습니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String token = CryptoUtils.generateRandomToken(32);
     * // 결과: "aB3dE5fG7hI9jK1lM2nO4pQ6rS8tU0vW"
     * }</pre>
     *
     * @param length 생성할 토큰 길이
     * @return 랜덤 영숫자 문자열
     */
    public static String generateRandomToken(int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder token = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(ALPHANUMERIC.length());
            token.append(ALPHANUMERIC.charAt(index));
        }
        return token.toString();
    }

    /**
     * 랜덤 숫자 문자열 생성
     * <p>
     * 지정된 길이의 랜덤한 숫자 문자열을 생성합니다.
     * 인증 번호, OTP 등에 사용할 수 있습니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String code = CryptoUtils.generateRandomNumber(6);
     * // 결과: "482957"
     * }</pre>
     *
     * @param length 생성할 숫자 문자열 길이
     * @return 랜덤 숫자 문자열
     */
    public static String generateRandomNumber(int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder number = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            number.append(SECURE_RANDOM.nextInt(10));
        }
        return number.toString();
    }

    /**
     * UUID 기반 토큰 생성
     * <p>
     * UUID에서 하이픈을 제거한 32자 토큰을 생성합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String uuid = CryptoUtils.generateUuidToken();
     * // 결과: "550e8400e29b41d4a716446655440000"
     * }</pre>
     *
     * @return UUID 기반 토큰 (32자)
     */
    public static String generateUuidToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 랜덤 바이트 배열 생성
     * <p>
     * 암호화 키, IV(Initialization Vector) 등에 사용할 수 있습니다.
     *
     * @param length 생성할 바이트 배열 길이
     * @return 랜덤 바이트 배열
     */
    public static byte[] generateRandomBytes(int length) {
        if (length <= 0) {
            return new byte[0];
        }

        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }
}
