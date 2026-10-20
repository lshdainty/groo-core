package io.groo.core.util;

import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.InvalidValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * 파일 업로드 보안 검증 유틸리티
 * <p>
 * 파일 업로드 시 확장자, 크기, MIME 타입 등을 검증하여 보안 위협을 방지합니다.
 * 각 프로젝트에서 허용 확장자와 최대 크기를 지정하여 사용합니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 프로필 이미지 업로드 검증
 * Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "gif", "webp");
 * long maxSize = 5 * 1024 * 1024L; // 5MB
 * FileUploadValidator.validate(file, allowedExtensions, maxSize);
 *
 * // 문서 업로드 검증
 * Set<String> docExtensions = Set.of("pdf", "doc", "docx", "xls", "xlsx");
 * FileUploadValidator.validate(file, docExtensions, 10 * 1024 * 1024L);
 * }</pre>
 *
 * @author groo
 * @see FileUtils
 */
public final class FileUploadValidator {

    private static final Logger log = LoggerFactory.getLogger(FileUploadValidator.class);

    private FileUploadValidator() {
    }

    /**
     * 파일 업로드 통합 검증 (확장자 + 크기)
     *
     * @param file              업로드 파일
     * @param allowedExtensions 허용 확장자 (점 없이, 소문자 권장: "jpg", "png")
     * @param maxSizeBytes      최대 파일 크기 (bytes)
     * @throws InvalidValueException 검증 실패 시
     */
    public static void validate(MultipartFile file, Set<String> allowedExtensions, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            log.warn("파일이 null이거나 비어있습니다.");
            throw new InvalidValueException(ErrorCode.INVALID_INPUT);
        }

        validateFileSize(file, maxSizeBytes);
        validateExtension(file.getOriginalFilename(), allowedExtensions);
    }

    /**
     * 파일 확장자 화이트리스트 검증
     *
     * @param filename          파일명
     * @param allowedExtensions 허용 확장자 (점 없이: "jpg", "png", "pdf")
     * @throws InvalidValueException 허용되지 않은 확장자일 경우
     */
    public static void validateExtension(String filename, Set<String> allowedExtensions) {
        if (!StringUtils.hasText(filename)) {
            log.warn("파일명이 비어있습니다.");
            throw new InvalidValueException(ErrorCode.FILE_INVALID_TYPE);
        }

        String extension = FileUtils.getExtension(filename).toLowerCase();
        if (extension.isEmpty() || !allowedExtensions.contains(extension)) {
            log.warn("허용되지 않는 파일 확장자: extension={}, filename={}, allowed={}",
                    extension, filename, allowedExtensions);
            throw new InvalidValueException(ErrorCode.FILE_INVALID_TYPE);
        }
    }

    /**
     * 파일 크기 검증
     *
     * @param file         업로드 파일
     * @param maxSizeBytes 최대 파일 크기 (bytes)
     * @throws InvalidValueException 크기 초과 시
     */
    public static void validateFileSize(MultipartFile file, long maxSizeBytes) {
        if (file.getSize() > maxSizeBytes) {
            log.warn("파일 크기 초과: size={}, maxSize={}", file.getSize(), maxSizeBytes);
            throw new InvalidValueException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    /**
     * 저장된 파일의 MIME 타입 감지
     * <p>
     * OS 기반 {@code Files.probeContentType()}을 우선 사용하고,
     * 실패 시 확장자 기반으로 폴백합니다.
     *
     * @param filePath 저장된 파일 경로
     * @return 감지된 MIME 타입
     */
    public static String detectMimeType(Path filePath) {
        try {
            String mimeType = Files.probeContentType(filePath);
            if (mimeType != null) {
                return mimeType;
            }
        } catch (IOException e) {
            log.warn("MIME 타입 감지 실패, 확장자 기반으로 폴백: path={}", filePath, e);
        }

        return FileUtils.getMimeTypeByFilename(filePath.getFileName().toString());
    }

    /**
     * Content-Disposition 헤더 인젝션 방지용 파일명 정제
     * <p>
     * CR(\r), LF(\n), 큰따옴표("), 역슬래시(\) 를 제거하여
     * HTTP 응답 헤더 분할 공격을 방지합니다.
     *
     * @param filename 원본 파일명
     * @return 안전한 파일명
     */
    public static String sanitizeForContentDisposition(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "download";
        }
        return filename.replaceAll("[\\r\\n\"\\\\]", "_");
    }
}
