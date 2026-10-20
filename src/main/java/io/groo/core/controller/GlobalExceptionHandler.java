package io.groo.core.controller;

import io.groo.core.exception.BusinessException;
import io.groo.core.exception.EntityNotFoundException;
import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.ErrorCodeProvider;
import io.groo.core.exception.ExternalServiceException;
import io.groo.core.exception.UnauthorizedException;
import io.groo.core.message.MessageKey;
import io.groo.core.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * <p>
 * 애플리케이션에서 발생하는 모든 예외를 일관된 {@link ApiResponse} 형식으로 변환합니다.
 * {@link MessageResolver}를 통해 다국어 에러 메시지를 지원합니다.
 *
 * <h3>예외 처리 우선순위</h3>
 * <p>
 * 예외 핸들러는 구체적인 예외부터 처리됩니다:
 * <ol>
 *   <li>{@link EntityNotFoundException} - 엔티티 조회 실패 (404)</li>
 *   <li>{@link UnauthorizedException} - 인증 실패 (401)</li>
 *   <li>{@link ExternalServiceException} - 외부 서비스 연동 실패 (502/503)</li>
 *   <li>{@link BusinessException} - 비즈니스 예외 (상위 클래스, 400/409 등)</li>
 *   <li>{@link AccessDeniedException} - Spring Security 권한 없음 (403)</li>
 *   <li>{@link MethodArgumentNotValidException} - @Valid 검증 실패 (400)</li>
 *   <li>{@link BindException} - 요청 바인딩 실패 (400)</li>
 *   <li>{@link IllegalArgumentException} - 잘못된 인자 (400)</li>
 *   <li>{@link Exception} - 그 외 모든 예외 (500)</li>
 * </ol>
 *
 * <h3>예외 클래스 계층 구조</h3>
 * <pre>
 * RuntimeException
 *   └── BusinessException (비즈니스 예외 기본 클래스)
 *       ├── EntityNotFoundException      # DB 엔티티 조회 실패 (404)
 *       ├── InvalidValueException        # 입력값 검증 실패 (400)
 *       ├── DuplicateException           # 중복 데이터 (409)
 *       ├── BusinessRuleViolationException # 비즈니스 규칙 위반 (400)
 *       ├── ForbiddenException           # 권한 없음 (403)
 *       ├── UnauthorizedException        # 인증 실패 (401)
 *       ├── ExternalServiceException     # 외부 서비스 연동 실패 (502/503)
 *       └── ResourceNotFoundException    # 파일 등 리소스 조회 실패 (404)
 * </pre>
 *
 * <h3>Service에서 예외 발생 예시</h3>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class UserServiceImpl implements UserService {
 *
 *     private final UserRepository userRepository;
 *
 *     // 엔티티 조회 실패
 *     public UserDto findById(Long id) {
 *         return userRepository.findById(id)
 *             .map(UserDto::from)
 *             .orElseThrow(() -> new EntityNotFoundException(HrErrorCode.USER_NOT_FOUND));
 *     }
 *
 *     // 입력값 검증 실패
 *     public void updateEmail(Long id, String email) {
 *         if (!isValidEmail(email)) {
 *             throw new InvalidValueException(HrErrorCode.INVALID_EMAIL_FORMAT);
 *         }
 *         // ...
 *     }
 *
 *     // 중복 데이터
 *     public UserDto create(CreateUserRequest request) {
 *         if (userRepository.existsByEmail(request.getEmail())) {
 *             throw new DuplicateException(HrErrorCode.USER_EMAIL_DUPLICATE);
 *         }
 *         // ...
 *     }
 *
 *     // 커스텀 메시지와 함께
 *     public void validateAge(int age) {
 *         if (age < 0) {
 *             throw new InvalidValueException(
 *                 HrErrorCode.INVALID_PARAMETER,
 *                 "나이는 0 이상이어야 합니다."
 *             );
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>에러 응답 형식</h3>
 * <pre>{@code
 * HTTP/1.1 404 Not Found
 * Content-Type: application/json
 *
 * {
 *   "success": false,
 *   "code": "USER_001",
 *   "message": "사용자를 찾을 수 없습니다.",
 *   "data": null
 * }
 * }</pre>
 *
 * <h3>다국어 메시지 처리</h3>
 * <p>
 * 에러 메시지는 {@link MessageResolver}를 통해 다국어로 처리됩니다:
 * <ul>
 *   <li>Accept-Language: ko → 한국어 메시지</li>
 *   <li>Accept-Language: en → 영어 메시지</li>
 * </ul>
 *
 * <pre>{@code
 * // messages_ko.properties
 * error.notfound.user=사용자를 찾을 수 없습니다.
 *
 * // messages_en.properties
 * error.notfound.user=User not found.
 * }</pre>
 *
 * <h3>로깅 레벨</h3>
 * <table border="1">
 *   <tr><th>예외 유형</th><th>로깅 레벨</th><th>스택 트레이스</th></tr>
 *   <tr><td>BusinessException 계열</td><td>WARN</td><td>X</td></tr>
 *   <tr><td>ExternalServiceException</td><td>ERROR</td><td>O</td></tr>
 *   <tr><td>NoResourceFoundException</td><td>WARN</td><td>X (보안)</td></tr>
 *   <tr><td>Exception (예상치 못한)</td><td>ERROR</td><td>O</td></tr>
 * </table>
 *
 * @author groo
 * @see ApiResponse
 * @see BusinessException
 * @see ErrorCode
 * @see MessageResolver
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageResolver messageResolver;

    /**
     * EntityNotFoundException 처리 (엔티티 조회 실패)
     * <p>
     * DB에서 엔티티를 찾지 못한 경우 발생합니다.
     * HTTP 404 (Not Found) 상태 코드를 반환합니다.
     *
     * <h4>발생 예시</h4>
     * <pre>{@code
     * public UserDto findById(Long id) {
     *     return userRepository.findById(id)
     *         .map(UserDto::from)
     *         .orElseThrow(() -> new EntityNotFoundException(HrErrorCode.USER_NOT_FOUND));
     * }
     * }</pre>
     *
     * <h4>응답 예시</h4>
     * <pre>{@code
     * HTTP/1.1 404 Not Found
     * {
     *   "success": false,
     *   "code": "USER_001",
     *   "message": "사용자를 찾을 수 없습니다.",
     *   "data": null
     * }
     * }</pre>
     *
     * @param e EntityNotFoundException 예외
     * @return 404 에러 응답
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("EntityNotFoundException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());

        ErrorCodeProvider errorCode = e.getErrorCode();
        String message = resolveMessage(e, errorCode);
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), message);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * ExternalServiceException 처리 (외부 서비스 연동 실패)
     * <p>
     * 외부 API, 메일 서버, 결제 서비스 등 외부 시스템 연동 실패 시 발생합니다.
     * 원인 파악을 위해 ERROR 레벨로 스택 트레이스를 포함하여 로깅합니다.
     *
     * <h4>발생 예시</h4>
     * <pre>{@code
     * public void sendEmail(String to, String content) {
     *     try {
     *         mailSender.send(createMessage(to, content));
     *     } catch (MailException e) {
     *         throw new ExternalServiceException(
     *             ErrorCode.INTERNAL_SERVER_ERROR,
     *             "메일 발송에 실패했습니다.",
     *             e
     *         );
     *     }
     * }
     * }</pre>
     *
     * @param e ExternalServiceException 예외
     * @return 502/503 에러 응답
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(ExternalServiceException e) {
        log.error("ExternalServiceException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage(), e);

        ErrorCodeProvider errorCode = e.getErrorCode();
        String message = resolveMessage(e, errorCode);
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), message);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * UnauthorizedException 처리 (인증 실패)
     * <p>
     * 토큰 만료, 잘못된 인증 정보 등 인증에 실패한 경우 발생합니다.
     * HTTP 401 (Unauthorized) 상태 코드를 반환합니다.
     *
     * <h4>발생 예시</h4>
     * <pre>{@code
     * public void validateToken(String token) {
     *     if (isExpired(token)) {
     *         throw new UnauthorizedException(ErrorCode.UNAUTHORIZED, "토큰이 만료되었습니다.");
     *     }
     * }
     * }</pre>
     *
     * @param e UnauthorizedException 예외
     * @return 401 에러 응답
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("UnauthorizedException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());

        ErrorCodeProvider errorCode = e.getErrorCode();
        String message = resolveMessage(e, errorCode);
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), message);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * BusinessException 처리 (비즈니스 예외)
     * <p>
     * 비즈니스 로직 상의 예외를 처리합니다.
     * 하위 예외들(EntityNotFoundException, InvalidValueException 등)이 먼저 처리되고,
     * 해당하지 않는 비즈니스 예외는 이 핸들러에서 처리됩니다.
     *
     * <h4>처리되는 예외 종류</h4>
     * <ul>
     *   <li>InvalidValueException - 입력값 검증 실패 (400)</li>
     *   <li>DuplicateException - 중복 데이터 (409)</li>
     *   <li>BusinessRuleViolationException - 비즈니스 규칙 위반 (400)</li>
     *   <li>ForbiddenException - 권한 없음 (403)</li>
     *   <li>기타 BusinessException 하위 클래스</li>
     * </ul>
     *
     * @param e BusinessException 예외
     * @return 에러 응답 (상태 코드는 ErrorCode에 따라 결정)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());

        ErrorCodeProvider errorCode = e.getErrorCode();
        String message = resolveMessage(e, errorCode);
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), message);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * AccessDeniedException 처리 (권한 없음)
     * <p>
     * Spring Security에서 권한이 없는 리소스에 접근할 때 발생합니다.
     * HTTP 403 (Forbidden) 상태 코드를 반환합니다.
     *
     * <h4>발생 상황</h4>
     * <ul>
     *   <li>@PreAuthorize 조건 불충족</li>
     *   <li>@Secured 역할 미보유</li>
     *   <li>Method Security 접근 제어</li>
     * </ul>
     *
     * @param e AccessDeniedException 예외
     * @return 403 에러 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());

        String message = messageResolver.getMessage(ErrorCode.FORBIDDEN);
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), message);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * NoResourceFoundException 처리 (존재하지 않는 리소스)
     * <p>
     * 존재하지 않는 정적 리소스나 API 엔드포인트에 접근할 때 발생합니다.
     * (Spring 6.0+ / Spring Boot 3.0+)
     * <p>
     * 보안상 스택 트레이스를 로깅하지 않고, 500 에러 대신 404를 반환합니다.
     *
     * <h4>보안 고려사항</h4>
     * <ul>
     *   <li>500 에러 노출 방지 → 404로 응답</li>
     *   <li>스택 트레이스 비노출 → 공격자 정보 수집 방지</li>
     *   <li>간단한 WARN 로그만 기록 → 디스크 용량 절약</li>
     * </ul>
     *
     * @param e NoResourceFoundException 예외
     * @return 404 에러 응답
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Invalid resource access: {}", e.getResourcePath());

        String message = messageResolver.getMessage(MessageKey.COMMON_404);
        ApiResponse<Void> response = ApiResponse.error("COMMON_404", message);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * IllegalArgumentException 처리 (잘못된 인자)
     * <p>
     * 메서드에 잘못된 인자가 전달된 경우 발생합니다.
     * HTTP 400 (Bad Request) 상태 코드를 반환합니다.
     *
     * @param e IllegalArgumentException 예외
     * @return 400 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());

        String message = e.getMessage() != null && !e.getMessage().isEmpty()
                ? e.getMessage()
                : messageResolver.getMessage(ErrorCode.INVALID_INPUT);

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * MethodArgumentNotValidException 처리 (@Valid 검증 실패)
     * <p>
     * Controller의 @Valid, @Validated 검증이 실패한 경우 발생합니다.
     * 모든 필드 에러 메시지를 쉼표로 연결하여 반환합니다.
     *
     * <h4>발생 예시</h4>
     * <pre>{@code
     * @PostMapping
     * public ApiResponse<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
     *     // @Valid 검증 실패 시 MethodArgumentNotValidException 발생
     * }
     *
     * public class CreateUserRequest {
     *     @NotBlank(message = "이름은 필수입니다.")
     *     private String name;
     *
     *     @Email(message = "이메일 형식이 올바르지 않습니다.")
     *     private String email;
     * }
     * }</pre>
     *
     * <h4>응답 예시</h4>
     * <pre>{@code
     * {
     *   "success": false,
     *   "code": "COMMON_400",
     *   "message": "이름은 필수입니다., 이메일 형식이 올바르지 않습니다.",
     *   "data": null
     * }
     * }</pre>
     *
     * @param e MethodArgumentNotValidException 예외
     * @return 400 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("MethodArgumentNotValidException: {}", errorMessage);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_INPUT.getCode(),
                errorMessage
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * BindException 처리 (바인딩 실패)
     * <p>
     * 요청 파라미터를 객체에 바인딩할 때 실패한 경우 발생합니다.
     * 주로 @ModelAttribute 사용 시 발생합니다.
     *
     * <h4>발생 예시</h4>
     * <pre>{@code
     * @GetMapping
     * public ApiResponse<List<UserDto>> searchUsers(@ModelAttribute SearchRequest request) {
     *     // 바인딩 실패 시 BindException 발생
     * }
     * }</pre>
     *
     * @param e BindException 예외
     * @return 400 에러 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("BindException: {}", errorMessage);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_INPUT.getCode(),
                errorMessage
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * MethodArgumentTypeMismatchException 처리 (타입 불일치)
     * <p>
     * URL 파라미터나 쿼리 파라미터의 타입이 맞지 않는 경우 발생합니다.
     *
     * <h4>발생 예시</h4>
     * <pre>{@code
     * @GetMapping("/{id}")
     * public ApiResponse<UserDto> getUser(@PathVariable Long id) {
     *     // GET /users/abc 요청 시 타입 불일치 발생
     * }
     * }</pre>
     *
     * @param e MethodArgumentTypeMismatchException 예외
     * @return 400 에러 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String errorMessage = String.format("'%s' 파라미터의 값이 유효하지 않습니다.", e.getName());
        log.warn("MethodArgumentTypeMismatchException: {}", errorMessage);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_INPUT.getCode(),
                errorMessage
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * MissingServletRequestParameterException 처리 (필수 요청 파라미터 누락)
     * <p>
     * 필수 {@code @RequestParam} 이 요청에 누락된 경우 발생합니다.
     * 클라이언트 요청 오류이므로 HTTP 400 (Bad Request) 상태 코드를 반환합니다.
     * <p>
     * 이 핸들러가 없으면 최종 fallback {@code Exception} 핸들러에서 500으로 잘못 처리되므로
     * 명시적으로 400 으로 응답합니다.
     *
     * <h4>발생 예시</h4>
     * <pre>{@code
     * @GetMapping("/search")
     * public ApiResponse<List<UserDto>> search(@RequestParam String keyword) {
     *     // GET /search (keyword 누락) 요청 시 발생
     * }
     * }</pre>
     *
     * @param e MissingServletRequestParameterException 예외
     * @return 400 에러 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: parameter={}", e.getParameterName());

        String message = messageResolver.getMessage(MessageKey.COMMON_MISSING_PARAMETER, e.getParameterName());
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Exception 처리 (예상치 못한 예외, 최종 fallback)
     * <p>
     * 위의 핸들러에서 처리되지 않은 모든 예외를 처리합니다.
     * 예상치 못한 에러이므로 ERROR 레벨로 스택 트레이스를 포함하여 로깅합니다.
     * <p>
     * HTTP 500 (Internal Server Error) 상태 코드를 반환하며,
     * 보안상 상세 에러 내용은 클라이언트에게 노출하지 않습니다.
     *
     * @param e Exception 예외
     * @return 500 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("Unexpected Exception", e);

        String message = messageResolver.getMessage(ErrorCode.INTERNAL_SERVER_ERROR);
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * BusinessException 메시지 처리 공통 메서드
     * <p>
     * 예외에 커스텀 메시지가 있으면 해당 메시지를 사용하고,
     * 없으면 MessageResolver를 통해 다국어 메시지를 조회합니다.
     *
     * <h4>메시지 결정 로직</h4>
     * <ol>
     *   <li>예외 생성 시 커스텀 메시지가 있으면 사용</li>
     *   <li>없으면 ErrorCode의 messageKey로 MessageResolver에서 조회</li>
     *   <li>Accept-Language 헤더에 따라 다국어 메시지 반환</li>
     * </ol>
     *
     * @param e         BusinessException 예외
     * @param errorCode 에러 코드
     * @return 에러 메시지
     */
    private String resolveMessage(BusinessException e, ErrorCodeProvider errorCode) {
        return e.getMessage() != null && !e.getMessage().equals(errorCode.getMessageKey())
                ? e.getMessage()
                : messageResolver.getMessage(errorCode);
    }
}
