package io.groo.core.util;

import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.ExternalServiceException;
import io.groo.core.exception.ResourceNotFoundException;
import io.groo.core.message.MessageKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 파일 처리 유틸리티
 * <p>
 * 파일 저장, 읽기, 복사, 이동, 삭제 등 파일 시스템 관련 작업을 처리합니다.
 * 업로드된 파일 처리, 파일 정보 조회, 파일명 관리 등의 기능을 제공합니다.
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>파일 작업: {@link #save}, {@link #read}, {@link #copy}, {@link #move}, {@link #delete}</li>
 *   <li>대용량 파일: {@link #withInputStream}, {@link #readChunked}, {@link #forEachLine}</li>
 *   <li>파일 정보: {@link #exists}, {@link #getSize}, {@link #getMimeType}, {@link #getLastModified}</li>
 *   <li>파일명 처리: {@link #getExtension}, {@link #getNameWithoutExtension}, {@link #sanitizeFilename}</li>
 *   <li>물리적 파일명: {@link #generatePhysicalFilename}, {@link #extractOriginalFilename}, {@link #extractUuid}</li>
 *   <li>디렉토리: {@link #createDirectory}, {@link #listFiles}</li>
 *   <li>권한 확인: {@link #isReadable}, {@link #isWritable}</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 파일 저장
 * String uuid = UUID.randomUUID().toString();
 * String physicalName = FileUtils.generatePhysicalFilename("document.pdf", uuid);
 * FileUtils.save(uploadFile, "/uploads", physicalName, messageResolver);
 *
 * // 파일 정보 조회
 * if (FileUtils.exists("/uploads/document.pdf")) {
 *     long size = FileUtils.getSize("/uploads/document.pdf");
 *     String mimeType = FileUtils.getMimeType("/uploads/document.pdf");
 * }
 *
 * // 파일명 처리
 * String ext = FileUtils.getExtension("report.xlsx");  // "xlsx"
 * String name = FileUtils.getNameWithoutExtension("report.xlsx");  // "report"
 * String safe = FileUtils.sanitizeFilename("파일<>:名.txt");  // "파일___名.txt"
 * }</pre>
 *
 * <h3>주의사항</h3>
 * <ul>
 *   <li>{@link #read} 메서드는 파일 전체를 메모리에 로드하므로 대용량 파일에는 부적합</li>
 *   <li>대용량 파일은 {@link #withInputStream}, {@link #readChunked}, {@link #forEachLine} 사용</li>
 *   <li>파일명은 {@link #sanitizeFilename}으로 경로 조작 공격 방지</li>
 *   <li>저장 경로가 없으면 자동으로 디렉토리 생성</li>
 * </ul>
 *
 * <h3>대용량 파일 처리 예시</h3>
 * <pre>{@code
 * // InputStream으로 자유롭게 처리 (HTTP 스트리밍, 해시 계산 등)
 * String hash = FileUtils.withInputStream("/files/large.zip", is -> {
 *     MessageDigest digest = MessageDigest.getInstance("SHA-256");
 *     byte[] buffer = new byte[8192];
 *     int bytesRead;
 *     while ((bytesRead = is.read(buffer)) != -1) {
 *         digest.update(buffer, 0, bytesRead);
 *     }
 *     return HexFormat.of().formatHex(digest.digest());
 * });
 *
 * // 바이너리 파일 청크 단위 처리
 * FileUtils.readChunked("/files/video.mp4", 8192, chunk -> {
 *     outputStream.write(chunk);
 * });
 *
 * // 텍스트 파일 줄 단위 처리
 * FileUtils.forEachLine("/logs/app.log", line -> {
 *     if (line.contains("ERROR")) {
 *         System.out.println(line);
 *     }
 * });
 * }</pre>
 *
 * @author groo
 * @see MultipartFile
 * @see MessageResolver
 */
public final class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    /** 파일명에서 제거할 위험 문자 패턴 */
    private static final Pattern UNSAFE_FILENAME_PATTERN = Pattern.compile("[<>:\"/\\\\|?*\\x00-\\x1F]");

    /** 경로 구분자 패턴 */
    private static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("[/\\\\]");

    private FileUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ========================================
    // 파일 존재/권한 확인
    // ========================================

    /**
     * 파일 존재 여부 확인
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * if (FileUtils.exists("/uploads/document.pdf")) {
     *     // 파일 처리
     * }
     * }</pre>
     *
     * @param path 파일 경로
     * @return 파일이 존재하면 true
     */
    public static boolean exists(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return Files.exists(Paths.get(path));
    }

    /**
     * 파일 존재 여부 확인 (Path 버전)
     *
     * @param path 파일 경로
     * @return 파일이 존재하면 true
     */
    public static boolean exists(Path path) {
        return path != null && Files.exists(path);
    }

    /**
     * 파일 읽기 가능 여부 확인
     *
     * @param path 파일 경로
     * @return 읽기 가능하면 true
     */
    public static boolean isReadable(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        Path filePath = Paths.get(path);
        return Files.exists(filePath) && Files.isReadable(filePath);
    }

    /**
     * 파일 쓰기 가능 여부 확인
     *
     * @param path 파일 경로
     * @return 쓰기 가능하면 true
     */
    public static boolean isWritable(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        Path filePath = Paths.get(path);
        return Files.exists(filePath) && Files.isWritable(filePath);
    }

    /**
     * 일반 파일인지 확인 (디렉토리 아님)
     *
     * @param path 파일 경로
     * @return 일반 파일이면 true
     */
    public static boolean isFile(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return Files.isRegularFile(Paths.get(path));
    }

    /**
     * 디렉토리인지 확인
     *
     * @param path 경로
     * @return 디렉토리이면 true
     */
    public static boolean isDirectory(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return Files.isDirectory(Paths.get(path));
    }

    // ========================================
    // 파일 정보 조회
    // ========================================

    /**
     * 파일 크기 조회 (바이트)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * long size = FileUtils.getSize("/uploads/document.pdf");
     * String readable = FileUtils.formatSize(size);  // "1.5 MB"
     * }</pre>
     *
     * @param path 파일 경로
     * @return 파일 크기 (바이트), 조회 실패 시 -1
     */
    public static long getSize(String path) {
        if (!StringUtils.hasText(path)) {
            return -1;
        }
        try {
            return Files.size(Paths.get(path));
        } catch (IOException e) {
            log.warn("Failed to get file size: {}", path, e);
            return -1;
        }
    }

    /**
     * 파일 크기를 읽기 쉬운 형식으로 변환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * FileUtils.formatSize(1024);       // "1.0 KB"
     * FileUtils.formatSize(1048576);    // "1.0 MB"
     * FileUtils.formatSize(1073741824); // "1.0 GB"
     * }</pre>
     *
     * @param bytes 바이트 크기
     * @return 읽기 쉬운 형식 (예: "1.5 MB")
     */
    public static String formatSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 파일 MIME 타입 조회
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String mimeType = FileUtils.getMimeType("/uploads/image.png");
     * // 결과: "image/png"
     * }</pre>
     *
     * @param path 파일 경로
     * @return MIME 타입 문자열, 조회 실패 시 "application/octet-stream"
     */
    public static String getMimeType(String path) {
        if (!StringUtils.hasText(path)) {
            return "application/octet-stream";
        }
        try {
            String mimeType = Files.probeContentType(Paths.get(path));
            return mimeType != null ? mimeType : "application/octet-stream";
        } catch (IOException e) {
            log.warn("Failed to get MIME type: {}", path, e);
            return "application/octet-stream";
        }
    }

    /**
     * 파일명에서 MIME 타입 추정
     *
     * @param filename 파일명
     * @return MIME 타입 문자열
     */
    public static String getMimeTypeByFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "application/octet-stream";
        }

        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            // 이미지
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            // 문서
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            // 텍스트
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "csv" -> "text/csv";
            // 압축
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "7z" -> "application/x-7z-compressed";
            case "tar" -> "application/x-tar";
            case "gz" -> "application/gzip";
            // 동영상
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "wmv" -> "video/x-ms-wmv";
            case "webm" -> "video/webm";
            // 오디오
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            default -> "application/octet-stream";
        };
    }

    /**
     * 파일 마지막 수정 시간 조회
     *
     * @param path 파일 경로
     * @return 마지막 수정 시간, 조회 실패 시 null
     */
    public static LocalDateTime getLastModified(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        try {
            BasicFileAttributes attrs = Files.readAttributes(Paths.get(path), BasicFileAttributes.class);
            return LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            log.warn("Failed to get last modified time: {}", path, e);
            return null;
        }
    }

    /**
     * 파일 생성 시간 조회
     *
     * @param path 파일 경로
     * @return 생성 시간, 조회 실패 시 null
     */
    public static LocalDateTime getCreationTime(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        try {
            BasicFileAttributes attrs = Files.readAttributes(Paths.get(path), BasicFileAttributes.class);
            return LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            log.warn("Failed to get creation time: {}", path, e);
            return null;
        }
    }

    // ========================================
    // 파일명 처리
    // ========================================

    /**
     * 파일 확장자 추출
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * FileUtils.getExtension("document.pdf");     // "pdf"
     * FileUtils.getExtension("archive.tar.gz");   // "gz"
     * FileUtils.getExtension("README");           // ""
     * FileUtils.getExtension(".gitignore");       // "gitignore"
     * }</pre>
     *
     * @param filename 파일명
     * @return 확장자 (점 제외), 없으면 빈 문자열
     */
    public static String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }

        // 경로에서 파일명만 추출
        int pathSepIndex = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (pathSepIndex >= 0) {
            filename = filename.substring(pathSepIndex + 1);
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        // .gitignore 같은 경우
        if (dotIndex == 0 && filename.length() > 1) {
            return filename.substring(1);
        }
        return "";
    }

    /**
     * 확장자 없는 파일명 추출
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * FileUtils.getNameWithoutExtension("document.pdf");   // "document"
     * FileUtils.getNameWithoutExtension("archive.tar.gz"); // "archive.tar"
     * FileUtils.getNameWithoutExtension("README");         // "README"
     * }</pre>
     *
     * @param filename 파일명
     * @return 확장자 없는 파일명
     */
    public static String getNameWithoutExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }

        // 경로에서 파일명만 추출
        int pathSepIndex = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (pathSepIndex >= 0) {
            filename = filename.substring(pathSepIndex + 1);
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    /**
     * 안전한 파일명 생성
     * <p>
     * 파일 시스템에서 문제가 될 수 있는 특수문자를 제거하거나 대체합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * FileUtils.sanitizeFilename("파일<>:\"/\\|?*名.txt");
     * // 결과: "파일_________名.txt"
     *
     * FileUtils.sanitizeFilename("../../../etc/passwd");
     * // 결과: "etc_passwd"
     * }</pre>
     *
     * @param filename 원본 파일명
     * @return 안전한 파일명
     */
    public static String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "unnamed";
        }

        // 경로 구분자 제거 (경로 조작 공격 방지)
        String safe = PATH_SEPARATOR_PATTERN.matcher(filename).replaceAll("_");

        // 위험한 문자 제거
        safe = UNSAFE_FILENAME_PATTERN.matcher(safe).replaceAll("_");

        // 연속된 언더스코어 정리
        safe = safe.replaceAll("_+", "_");

        // 앞뒤 공백 및 점 제거
        safe = safe.trim();
        while (safe.startsWith(".") || safe.startsWith("_")) {
            safe = safe.substring(1);
        }
        while (safe.endsWith(".") || safe.endsWith("_")) {
            safe = safe.substring(0, safe.length() - 1);
        }

        return safe.isEmpty() ? "unnamed" : safe;
    }

    // ========================================
    // 파일 작업 (저장, 읽기, 복사, 이동, 삭제)
    // ========================================

    /**
     * MultipartFile 저장
     * <p>
     * 업로드된 파일을 지정된 경로에 저장합니다.
     * 파일명을 지정하지 않으면 원본 파일명으로 저장합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 원본 파일명으로 저장
     * FileUtils.save(multipartFile, "/uploads/2024", null, messageResolver);
     *
     * // UUID를 붙여서 저장
     * String uuid = UUID.randomUUID().toString();
     * String physicalName = FileUtils.generatePhysicalFilename(
     *     multipartFile.getOriginalFilename(), uuid);
     * FileUtils.save(multipartFile, "/uploads", physicalName, messageResolver);
     * }</pre>
     *
     * @param multipartFile   저장할 파일
     * @param path            저장 경로 (파일명 제외)
     * @param fileName        저장할 파일명 (null이면 원본 파일명 사용)
     * @param messageResolver 메시지 리졸버
     * @return 저장 성공 시 true
     * @throws ExternalServiceException 파일 저장 실패 시
     */
    public static boolean save(MultipartFile multipartFile, String path, String fileName, MessageResolver messageResolver) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("MultipartFile is null or empty");
            return false;
        }

        // 옵션 파일명이 없다면 객체에서 원본 파일명을 가져와서 저장
        if (!StringUtils.hasText(fileName)) {
            fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        }

        if (fileName.isEmpty()) {
            log.warn("Could not save file with empty name");
            return false;
        }

        log.debug("Saving file. path: {}, fileName: {}", path, fileName);
        try {
            Path directory = Paths.get(path);
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName);
            multipartFile.transferTo(filePath.toFile());
            log.info("File saved successfully. path: {}, fileName: {}", path, fileName);
            return true;
        } catch (IOException e) {
            log.error("File save failed. path: {}, fileName: {}", path, fileName, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    messageResolver.getMessage(MessageKey.FILE_SAVE_ERROR, fileName),
                    e
            );
        }
    }

    /**
     * 파일 읽기
     * <p>
     * 지정된 경로의 파일을 byte 배열로 읽어옵니다.
     *
     * <h4>주의사항</h4>
     * <p>
     * 파일 전체를 메모리에 로드하므로 대용량 파일에는 사용하지 마세요.
     *
     * @param fullPath        읽을 파일의 전체 경로
     * @param messageResolver 메시지 리졸버
     * @return 파일 내용 (byte 배열)
     * @throws ResourceNotFoundException 파일이 존재하지 않거나 읽을 수 없는 경우
     * @throws ExternalServiceException  파일 읽기 중 IO 오류 발생 시
     */
    public static byte[] read(String fullPath, MessageResolver messageResolver) {
        log.debug("Reading file. fullPath: {}", fullPath);
        try {
            Path filePath = Paths.get(fullPath);

            if (Files.exists(filePath) && Files.isRegularFile(filePath) && Files.isReadable(filePath)) {
                byte[] fileBytes = Files.readAllBytes(filePath);
                log.info("File read successfully. fullPath: {}, size: {} bytes", fullPath, fileBytes.length);
                return fileBytes;
            } else {
                log.warn("File not found or not readable. fullPath: {}", fullPath);
                throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
            }
        } catch (IOException e) {
            log.error("File read failed. fullPath: {}", fullPath, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    messageResolver.getMessage(MessageKey.FILE_READ, fullPath),
                    e
            );
        }
    }

    // ========================================
    // 대용량 파일 처리 (스트림 기반)
    // ========================================

    /**
     * InputStream을 사용하여 파일 처리
     * <p>
     * 대용량 파일을 메모리에 전체 로드하지 않고 스트림으로 처리합니다.
     * 호출자가 InputStream을 자유롭게 사용할 수 있으며, 처리 완료 후 자동으로 close됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // HTTP Response로 스트리밍 (다운로드 API)
     * FileUtils.withInputStream("/files/video.mp4", is -> {
     *     is.transferTo(response.getOutputStream());
     *     return null;
     * });
     *
     * // 파일 해시 계산
     * String hash = FileUtils.withInputStream("/files/large.zip", is -> {
     *     MessageDigest digest = MessageDigest.getInstance("SHA-256");
     *     byte[] buffer = new byte[8192];
     *     int bytesRead;
     *     while ((bytesRead = is.read(buffer)) != -1) {
     *         digest.update(buffer, 0, bytesRead);
     *     }
     *     return HexFormat.of().formatHex(digest.digest());
     * });
     *
     * // 처음 100바이트만 읽기
     * byte[] header = FileUtils.withInputStream("/files/doc.bin", is -> {
     *     byte[] buffer = new byte[100];
     *     int read = is.read(buffer);
     *     return Arrays.copyOf(buffer, read);
     * });
     * }</pre>
     *
     * @param <T>       반환 타입
     * @param path      파일 경로
     * @param processor InputStream을 받아 처리하는 함수
     * @return processor의 반환값
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  파일 처리 중 오류 발생 시
     */
    public static <T> T withInputStream(String path, Function<InputStream, T> processor) {
        log.debug("Processing file with InputStream. path: {}", path);
        Path filePath = Paths.get(path);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.warn("File not found: {}", path);
            throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
        }

        try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath))) {
            T result = processor.apply(is);
            log.debug("File processing completed. path: {}", path);
            return result;
        } catch (IOException e) {
            log.error("File processing failed. path: {}", path, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to process file: " + path,
                    e
            );
        } catch (Exception e) {
            log.error("Error in processor function. path: {}", path, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error processing file: " + path,
                    e
            );
        }
    }

    /**
     * 바이너리 파일을 청크 단위로 처리
     * <p>
     * 대용량 바이너리 파일을 지정된 크기의 청크로 나누어 처리합니다.
     * 메모리 효율적으로 대용량 파일을 처리할 수 있습니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 파일을 8KB씩 읽어서 OutputStream으로 전송
     * FileUtils.readChunked("/files/large-video.mp4", 8192, chunk -> {
     *     outputStream.write(chunk);
     * });
     *
     * // 파일을 1MB씩 읽어서 처리
     * AtomicLong totalSize = new AtomicLong(0);
     * FileUtils.readChunked("/files/backup.tar", 1024 * 1024, chunk -> {
     *     totalSize.addAndGet(chunk.length);
     *     // 청크별 처리 로직
     * });
     * }</pre>
     *
     * @param path      파일 경로
     * @param chunkSize 청크 크기 (바이트)
     * @param handler   각 청크를 처리할 Consumer
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  파일 처리 중 오류 발생 시
     */
    public static void readChunked(String path, int chunkSize, Consumer<byte[]> handler) {
        log.debug("Reading file in chunks. path: {}, chunkSize: {}", path, chunkSize);
        Path filePath = Paths.get(path);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.warn("File not found: {}", path);
            throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
        }

        if (chunkSize <= 0) {
            chunkSize = 8192; // 기본 8KB
        }

        try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath))) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = is.read(buffer)) != -1) {
                // 마지막 청크는 실제 읽은 크기만큼만 전달
                if (bytesRead < chunkSize) {
                    handler.accept(Arrays.copyOf(buffer, bytesRead));
                } else {
                    handler.accept(buffer.clone());
                }
                totalBytes += bytesRead;
            }

            log.debug("File chunked reading completed. path: {}, totalBytes: {}", path, totalBytes);
        } catch (IOException e) {
            log.error("Chunked file reading failed. path: {}", path, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to read file in chunks: " + path,
                    e
            );
        }
    }

    /**
     * 기본 청크 크기(8KB)로 바이너리 파일 처리
     *
     * @param path    파일 경로
     * @param handler 각 청크를 처리할 Consumer
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  파일 처리 중 오류 발생 시
     */
    public static void readChunked(String path, Consumer<byte[]> handler) {
        readChunked(path, 8192, handler);
    }

    /**
     * 텍스트 파일을 줄 단위로 처리 (UTF-8)
     * <p>
     * 대용량 텍스트 파일을 한 줄씩 읽어서 처리합니다.
     * 파일 전체를 메모리에 로드하지 않으므로 대용량 로그 파일 등에 적합합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 로그 파일에서 ERROR 라인만 출력
     * FileUtils.forEachLine("/logs/application.log", line -> {
     *     if (line.contains("ERROR")) {
     *         System.out.println(line);
     *     }
     * });
     *
     * // CSV 파일 파싱
     * List<String[]> rows = new ArrayList<>();
     * FileUtils.forEachLine("/data/users.csv", line -> {
     *     rows.add(line.split(","));
     * });
     *
     * // 라인 수 카운트
     * AtomicLong lineCount = new AtomicLong(0);
     * FileUtils.forEachLine("/logs/huge.log", line -> lineCount.incrementAndGet());
     * }</pre>
     *
     * @param path        파일 경로
     * @param lineHandler 각 줄을 처리할 Consumer
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  파일 처리 중 오류 발생 시
     */
    public static void forEachLine(String path, Consumer<String> lineHandler) {
        forEachLine(path, StandardCharsets.UTF_8, lineHandler);
    }

    /**
     * 텍스트 파일을 줄 단위로 처리 (인코딩 지정)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // EUC-KR 인코딩 파일 처리
     * FileUtils.forEachLine("/data/legacy.txt", Charset.forName("EUC-KR"), line -> {
     *     System.out.println(line);
     * });
     * }</pre>
     *
     * @param path        파일 경로
     * @param charset     파일 인코딩
     * @param lineHandler 각 줄을 처리할 Consumer
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  파일 처리 중 오류 발생 시
     */
    public static void forEachLine(String path, Charset charset, Consumer<String> lineHandler) {
        log.debug("Reading file line by line. path: {}, charset: {}", path, charset);
        Path filePath = Paths.get(path);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.warn("File not found: {}", path);
            throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
        }

        try (Stream<String> lines = Files.lines(filePath, charset)) {
            lines.forEach(lineHandler);
            log.debug("File line reading completed. path: {}", path);
        } catch (IOException e) {
            log.error("Line by line file reading failed. path: {}", path, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to read file lines: " + path,
                    e
            );
        }
    }

    // ========================================
    // 파일 해시/체크섬
    // ========================================

    /**
     * 파일 해시 계산
     * <p>
     * 지정된 알고리즘으로 파일의 해시값을 계산합니다.
     * 스트림 기반으로 대용량 파일도 메모리 효율적으로 처리합니다.
     *
     * <h4>지원 알고리즘</h4>
     * <ul>
     *   <li>SHA-256 (권장)</li>
     *   <li>SHA-512</li>
     *   <li>MD5 (체크섬 용도만)</li>
     * </ul>
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // SHA-256 해시 계산
     * String hash = FileUtils.calculateHash("/files/document.pdf", "SHA-256");
     *
     * // 파일 무결성 검증
     * String expectedHash = "abc123...";
     * if (hash.equals(expectedHash)) {
     *     // 파일 무결성 확인됨
     * }
     * }</pre>
     *
     * @param path      파일 경로
     * @param algorithm 해시 알고리즘 (SHA-256, SHA-512, MD5)
     * @return 16진수 해시 문자열
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  해시 계산 중 오류 발생 시
     */
    public static String calculateHash(String path, String algorithm) {
        return withInputStream(path, is -> {
            try {
                MessageDigest digest = MessageDigest.getInstance(algorithm);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
                return bytesToHex(digest.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm, e);
            } catch (IOException e) {
                throw new ExternalServiceException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Failed to read file for hashing: " + path,
                        e
                );
            }
        });
    }

    /**
     * SHA-256 해시 계산 (편의 메서드)
     * <p>
     * 파일의 SHA-256 해시값을 계산합니다. 파일 무결성 검증에 권장됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String hash = FileUtils.calculateSha256("/files/document.pdf");
     * // 결과: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
     * }</pre>
     *
     * @param path 파일 경로
     * @return SHA-256 해시 문자열 (64자리 16진수)
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  해시 계산 중 오류 발생 시
     */
    public static String calculateSha256(String path) {
        return calculateHash(path, "SHA-256");
    }

    /**
     * MD5 해시 계산 (체크섬 용도)
     * <p>
     * 파일의 MD5 해시값을 계산합니다.
     * 보안 목적에는 적합하지 않으며, 단순 체크섬 비교 용도로만 사용하세요.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String checksum = FileUtils.calculateMd5("/files/archive.zip");
     * // 결과: "d41d8cd98f00b204e9800998ecf8427e"
     * }</pre>
     *
     * @param path 파일 경로
     * @return MD5 해시 문자열 (32자리 16진수)
     * @throws ResourceNotFoundException 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  해시 계산 중 오류 발생 시
     */
    public static String calculateMd5(String path) {
        return calculateHash(path, "MD5");
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     *
     * @param bytes 바이트 배열
     * @return 16진수 문자열 (소문자)
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 파일 복사
     * <p>
     * 원본 파일을 대상 경로로 복사합니다.
     *
     * @param sourcePath      원본 파일 전체 경로
     * @param targetPath      대상 파일 전체 경로
     * @param messageResolver 메시지 리졸버
     * @return 복사 성공 시 true
     * @throws ResourceNotFoundException 원본 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  복사 중 IO 오류 발생 시
     */
    public static boolean copy(String sourcePath, String targetPath, MessageResolver messageResolver) {
        return copy(sourcePath, targetPath, false, messageResolver);
    }

    /**
     * 파일 복사 (덮어쓰기 옵션)
     *
     * @param sourcePath      원본 파일 전체 경로
     * @param targetPath      대상 파일 전체 경로
     * @param overwrite       대상 파일이 있으면 덮어쓸지 여부
     * @param messageResolver 메시지 리졸버
     * @return 복사 성공 시 true
     * @throws ResourceNotFoundException 원본 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  복사 중 IO 오류 발생 시
     */
    public static boolean copy(String sourcePath, String targetPath, boolean overwrite, MessageResolver messageResolver) {
        log.debug("Copying file. source: {}, target: {}, overwrite: {}", sourcePath, targetPath, overwrite);
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);

        try {
            if (!Files.exists(source)) {
                log.warn("Source file not found for copy: {}", sourcePath);
                throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
            }

            Path targetDir = target.getParent();
            if (targetDir != null && !Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            if (overwrite) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(source, target);
            }
            log.info("File copied successfully. source: {}, target: {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            log.error("File copy failed. source: {}, target: {}", sourcePath, targetPath, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    messageResolver.getMessage(MessageKey.FILE_COPY, sourcePath, targetPath),
                    e
            );
        }
    }

    /**
     * 파일 이동
     * <p>
     * 원본 파일을 대상 경로로 이동합니다.
     *
     * @param sourcePath      원본 파일 전체 경로
     * @param targetPath      대상 파일 전체 경로
     * @param messageResolver 메시지 리졸버
     * @return 이동 성공 시 true
     * @throws ResourceNotFoundException 원본 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  이동 중 IO 오류 발생 시
     */
    public static boolean move(String sourcePath, String targetPath, MessageResolver messageResolver) {
        return move(sourcePath, targetPath, false, messageResolver);
    }

    /**
     * 파일 이동 (덮어쓰기 옵션)
     *
     * @param sourcePath      원본 파일 전체 경로
     * @param targetPath      대상 파일 전체 경로
     * @param overwrite       대상 파일이 있으면 덮어쓸지 여부
     * @param messageResolver 메시지 리졸버
     * @return 이동 성공 시 true
     * @throws ResourceNotFoundException 원본 파일이 존재하지 않는 경우
     * @throws ExternalServiceException  이동 중 IO 오류 발생 시
     */
    public static boolean move(String sourcePath, String targetPath, boolean overwrite, MessageResolver messageResolver) {
        log.debug("Moving file. source: {}, target: {}, overwrite: {}", sourcePath, targetPath, overwrite);
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);

        try {
            if (!Files.exists(source)) {
                log.warn("Source file not found for move: {}", sourcePath);
                throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
            }

            Path targetDir = target.getParent();
            if (targetDir != null && !Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            if (overwrite) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
            log.info("File moved successfully. source: {}, target: {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            log.error("File move failed. source: {}, target: {}", sourcePath, targetPath, e);
            throw new ExternalServiceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    messageResolver.getMessage(MessageKey.FILE_MOVE, sourcePath, targetPath),
                    e
            );
        }
    }

    /**
     * 파일 삭제
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * boolean deleted = FileUtils.delete("/temp/upload.tmp");
     * }</pre>
     *
     * @param path 삭제할 파일 경로
     * @return 삭제 성공 시 true, 파일이 없거나 실패 시 false
     */
    public static boolean delete(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }

        try {
            boolean deleted = Files.deleteIfExists(Paths.get(path));
            if (deleted) {
                log.info("File deleted successfully: {}", path);
            } else {
                log.debug("File not found for deletion: {}", path);
            }
            return deleted;
        } catch (IOException e) {
            log.error("File deletion failed: {}", path, e);
            return false;
        }
    }

    // ========================================
    // 디렉토리 작업
    // ========================================

    /**
     * 디렉토리 생성
     * <p>
     * 중간 경로가 없으면 함께 생성합니다.
     *
     * @param path 생성할 디렉토리 경로
     * @return 생성 성공 시 true
     */
    public static boolean createDirectory(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }

        try {
            Files.createDirectories(Paths.get(path));
            log.debug("Directory created: {}", path);
            return true;
        } catch (IOException e) {
            log.error("Failed to create directory: {}", path, e);
            return false;
        }
    }

    /**
     * 디렉토리 내 파일 목록 조회
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * List<Path> files = FileUtils.listFiles("/uploads/2024");
     * for (Path file : files) {
     *     System.out.println(file.getFileName());
     * }
     * }</pre>
     *
     * @param directoryPath 디렉토리 경로
     * @return 파일 목록 (디렉토리 제외)
     */
    public static List<Path> listFiles(String directoryPath) {
        List<Path> files = new ArrayList<>();
        if (!StringUtils.hasText(directoryPath)) {
            return files;
        }

        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            return files;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    files.add(entry);
                }
            }
        } catch (IOException e) {
            log.error("Failed to list files in directory: {}", directoryPath, e);
        }

        return files;
    }

    /**
     * 디렉토리 내 특정 확장자 파일 목록 조회
     *
     * @param directoryPath 디렉토리 경로
     * @param extension     확장자 (점 제외, 예: "pdf")
     * @return 해당 확장자의 파일 목록
     */
    public static List<Path> listFiles(String directoryPath, String extension) {
        List<Path> files = new ArrayList<>();
        if (!StringUtils.hasText(directoryPath) || !StringUtils.hasText(extension)) {
            return files;
        }

        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            return files;
        }

        String glob = "*." + extension;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    files.add(entry);
                }
            }
        } catch (IOException e) {
            log.error("Failed to list files in directory: {} with extension: {}", directoryPath, extension, e);
        }

        return files;
    }

    // ========================================
    // 물리적 파일명 관리
    // ========================================

    /**
     * 물리적 파일명 생성
     * <p>
     * 원본 파일명과 UUID를 조합하여 고유한 물리적 파일명을 생성합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String uuid = UUID.randomUUID().toString();
     * FileUtils.generatePhysicalFilename("document.pdf", uuid);
     * // 결과: "document_550e8400-e29b-41d4-a716-446655440000.pdf"
     * }</pre>
     *
     * @param originalFilename 원본 파일명
     * @param uuid             UUID 문자열
     * @return 물리적 파일명 (originalName_UUID.extension 형식)
     */
    public static String generatePhysicalFilename(String originalFilename, String uuid) {
        if (!StringUtils.hasText(originalFilename) || !StringUtils.hasText(uuid)) {
            return originalFilename;
        }

        if (originalFilename.contains(".")) {
            int lastDotIndex = originalFilename.lastIndexOf(".");
            String nameWithoutExt = originalFilename.substring(0, lastDotIndex);
            String extension = originalFilename.substring(lastDotIndex);
            return nameWithoutExt + "_" + uuid + extension;
        }

        return originalFilename + "_" + uuid;
    }

    /**
     * 물리적 파일명에서 원본 파일명 추출
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * String physical = "document_550e8400-e29b-41d4-a716-446655440000.pdf";
     * FileUtils.extractOriginalFilename(physical, null);
     * // 결과: "document.pdf"
     * }</pre>
     *
     * @param physicalFilename 물리적 파일명
     * @param uuid             UUID 문자열 (null이면 패턴으로 자동 추출)
     * @return 원본 파일명
     */
    public static String extractOriginalFilename(String physicalFilename, String uuid) {
        if (!StringUtils.hasText(physicalFilename)) {
            return null;
        }

        if (StringUtils.hasText(uuid)) {
            return physicalFilename.replace("_" + uuid, "");
        }

        String extractedUuid = extractUuid(physicalFilename, null);
        if (StringUtils.hasText(extractedUuid)) {
            return physicalFilename.replace("_" + extractedUuid, "");
        }

        return physicalFilename;
    }

    /**
     * 물리적 파일명에서 UUID 추출
     *
     * @param physicalFilename 물리적 파일명
     * @param uuid             UUID 문자열 (제공되면 그대로 반환)
     * @return 추출된 UUID, 추출 실패 시 null
     */
    public static String extractUuid(String physicalFilename, String uuid) {
        if (!StringUtils.hasText(physicalFilename)) {
            return null;
        }

        if (StringUtils.hasText(uuid)) {
            return uuid;
        }

        String nameWithoutExt = physicalFilename;
        if (physicalFilename.contains(".")) {
            nameWithoutExt = physicalFilename.substring(0, physicalFilename.lastIndexOf("."));
        }

        int lastUnderscoreIndex = nameWithoutExt.lastIndexOf("_");
        if (lastUnderscoreIndex != -1 && lastUnderscoreIndex < nameWithoutExt.length() - 1) {
            return nameWithoutExt.substring(lastUnderscoreIndex + 1);
        }

        return null;
    }
}
