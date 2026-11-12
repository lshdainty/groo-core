package io.groo.core.controller;

import io.groo.core.exception.BusinessException;
import io.groo.core.exception.EntityNotFoundException;
import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.ErrorCodeProvider;
import io.groo.core.exception.ExternalServiceException;
import io.groo.core.exception.IntegrityViolationKind;
import io.groo.core.exception.IntegrityViolations;
import io.groo.core.exception.UnauthorizedException;
import io.groo.core.message.MessageKey;
import io.groo.core.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;
import java.util.Set;
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
 *   <tr><td>요청 방식·형식·값 오류(400·404·405·415)</td><td>WARN</td><td>X</td></tr>
 *   <tr><td>동시 수정·DB 제약 위반(409·400)</td><td>WARN</td><td>X</td></tr>
 *   <tr><td>클라이언트 연결 끊김(AsyncRequestNotUsable)</td><td>DEBUG</td><td>X</td></tr>
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

        String message = message(ErrorCode.FORBIDDEN);
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

        // 예외 문구는 응답에 싣지 않는다(core 3.3.0, 보안 점검 SSO-11). IllegalArgumentException 은 JDK·라이브러리가
        // 내부 사정(enum 클래스 경로, 파서 위치, 필드 이름)을 적어 던지는 예외라 그대로 내보내면 구현이 샌다.
        // 사용자에게 보여 줄 문구가 있으면 서비스가 ErrorCode 를 가진 예외(InvalidValueException 등)로 던진다.
        String message = message(ErrorCode.INVALID_INPUT);

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
        // 문구는 번들에서 꺼내고 파라미터 이름은 응답에 싣지 않는다(3.1.0). 예전엔 "'id' 파라미터의 값이
        // 유효하지 않습니다." 를 코드에 박아 둬서 번역도 말투 통일도 안 됐고, 코드 이름(id·year)이 API 구조를
        // 드러냈다(groo-damda-back QA 2026-09-03 #74 · 09-04 #75). 이름은 이 로그에만 남는다 — 지우지 말 것.
        log.warn("MethodArgumentTypeMismatchException: name={}, required={}", e.getName(), e.getRequiredType());

        ErrorCode code = ErrorCode.INVALID_PARAMETER_VALUE;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), message(code)));
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
        // 파라미터 이름은 응답 문구에 넣지 않고 로그에만 남긴다(3.1.0, 위 TypeMismatch 와 같은 이유).
        // 인자를 넘기지 않는 이유가 하나 더 있다 — 인자가 있으면 Spring 이 문구를 MessageFormat 에 태워,
        // 번역에 작은따옴표가 섞이는 순간 문구 일부가 인용부호로 먹혀 사라진다.
        log.warn("MissingServletRequestParameterException: name={}, type={}", e.getParameterName(), e.getParameterType());

        ErrorCode code = ErrorCode.MISSING_PARAMETER;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), message(code)));
    }

    /**
     * 지원하지 않는 요청 방식 — 405 + {@code Allow} 헤더.
     * <p>
     * 경로는 있는데 메서드가 다를 때(POST 전용 API 에 GET 등) Spring 이 던진다. 매핑이 없던 동안은
     * 마지막 {@code Exception} 핸들러로 떨어져 500 과 ERROR 스택이 남았다 — 2026-11-10 외부 봇이 sso 의
     * POST 전용 API 를 GET 으로 훑자 ERROR 16줄이 쌓여 "오류 로그 급증" 경보가 울렸다. 클라이언트 실수이므로
     * 4xx 로 답하고 WARN 한 줄만 남긴다. RFC 9110 은 405 에 {@code Allow} 를 요구한다.
     *
     * @param e HttpRequestMethodNotSupportedException 예외
     * @return 405 에러 응답
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: method={}, supported={}", e.getMethod(), e.getSupportedHttpMethods());

        ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(code.getHttpStatus());
        String allow = allowHeader(e.getSupportedHttpMethods());
        if (allow != null) {
            builder.header(HttpHeaders.ALLOW, allow);
        }
        return builder.body(ApiResponse.error(code.getCode(), message(code)));
    }

    /**
     * 읽을 수 없는 요청 본문 — 400.
     * <p>
     * JSON 문법 오류, 필드 타입 불일치(문자열 자리에 객체 등), 빈 본문이 여기로 온다. 역시 클라이언트 실수다.
     * 원인 메시지는 내부 클래스 이름이 섞여 있어 응답에 싣지 않고 WARN 로그에만 남긴다.
     *
     * @param e HttpMessageNotReadableException 예외
     * @return 400 에러 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        // 원인 사슬에 날짜 파싱 실패가 있으면 "날짜가 아니다" 로 더 정확히 답한다.
        ErrorCode code = isDateFailure(e) ? ErrorCode.INVALID_DATE_VALUE : ErrorCode.MALFORMED_REQUEST_BODY;
        log.warn("HttpMessageNotReadableException: code={}, cause={}", code.name(), e.getMostSpecificCause().getMessage());

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), message(code)));
    }

    /**
     * 지원하지 않는 Content-Type — 415.
     *
     * @param e HttpMediaTypeNotSupportedException 예외
     * @return 415 에러 응답
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("HttpMediaTypeNotSupportedException: contentType={}", e.getContentType());

        ErrorCode code = ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), message(code)));
    }

    /**
     * 클라이언트가 먼저 끊은 비동기 응답(SSE 등) — 아무것도 쓰지 않고 끝낸다.
     * <p>
     * 앱이 백그라운드로 가거나 탭을 닫으면 정상적으로 생긴다. 예전엔 마지막 핸들러가 ERROR 스택을 찍고
     * JSON 을 쓰려다 {@code text/event-stream} 응답이라 실패해 WARN 을 한 번 더 남겼다(끊김 1번에 2건).
     * 받을 사람이 이미 떠났으므로 DEBUG 한 줄로 끝낸다.
     * <p>
     * <b>{@link HttpServletResponse} 인자는 쓰지 않지만 지우면 안 된다.</b> 반환이 {@code void} 인 예외
     * 핸들러는 응답 인자를 받아야 "응답을 직접 처리했다" 로 표시되고, 아니면 Spring 이 뷰 이름을 찾으러 간다.
     * 응답 객체를 건드리면 끊긴 소켓에 쓰다가 같은 예외가 다시 난다.
     *
     * @param e        AsyncRequestNotUsableException 예외
     * @param response 처리 완료 표시용 (사용하지 않음)
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e, HttpServletResponse response) {
        log.debug("Client disconnected from async response: {}", e.getMessage());
    }

    /**
     * 컨트롤러가 직접 날짜를 파싱하다 실패 — 400.
     *
     * @param e DateTimeParseException 예외
     * @return 400 에러 응답
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponse<Void>> handleDateTimeParseException(DateTimeParseException e) {
        log.warn("DateTimeParseException: parsed={}", e.getParsedString());

        ErrorCode code = ErrorCode.INVALID_DATE_VALUE;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), message(code)));
    }

    /**
     * 매핑이 아예 없는 경로 — 404. <b>지금 구성에선 던져지지 않는 안전망이다.</b>
     * <p>
     * 정적 리소스 핸들러(`/**`)가 먼저 받아 {@link NoResourceFoundException} 을 던지므로 평소엔 위 핸들러가
     * 답한다. {@code spring.web.resources.add-mappings=false} 로 정적 매핑을 끄는 순간 이 예외가 살아나는데,
     * 그때 이 핸들러가 없으면 없는 경로가 500 으로 나간다. 응답은 NoResourceFound 와 글자까지 같다.
     *
     * @param e NoHandlerFoundException 예외
     * @return 404 에러 응답
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("NoHandlerFoundException: method={}, path={}", e.getHttpMethod(), e.getRequestURL());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("COMMON_404", messageResolver.getMessage(MessageKey.COMMON_404)));
    }

    /**
     * 낙관적 락 충돌(동시 수정) — 409.
     * <p>
     * {@code @Version} 엔티티를 두 요청이 거의 동시에 고치면 늦은 쪽이 받는다. 서버 고장이 아니라 경합이므로
     * "다시 시도하라" 로 답한다.
     *
     * @param e ObjectOptimisticLockingFailureException 예외
     * @return 409 에러 응답
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleObjectOptimisticLockingFailureException(ObjectOptimisticLockingFailureException e) {
        log.warn("ObjectOptimisticLockingFailureException: {}", e.getMessage());

        ErrorCode code = ErrorCode.CONCURRENT_MODIFICATION;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), message(code)));
    }

    /**
     * DB 무결성 제약 위반 — 원인별로 409 / 400.
     * <p>
     * 서비스가 미리 막지 못한 위반이 DB 에서 터진 자리다. {@link IntegrityViolations} 가 Hibernate·벤더 코드
     * (MariaDB·H2)로 종류를 가른다: 중복 → 409(동시 생성 경합), NOT NULL → 400, 외래키 → 400, 그 밖 → 400.
     * 제약 이름·SQLState 는 로그에만 남고 응답에는 싣지 않는다(스키마가 드러난다).
     *
     * @param e DataIntegrityViolationException 예외
     * @return 409 또는 400 에러 응답
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        IntegrityViolationKind kind = IntegrityViolations.classify(e);
        ErrorCode code = switch (kind) {
            case UNIQUE -> ErrorCode.CONCURRENT_MODIFICATION;
            case NOT_NULL -> ErrorCode.REQUIRED_VALUE_MISSING;
            case FOREIGN_KEY -> ErrorCode.INVALID_REFERENCE;
            case OTHER -> ErrorCode.INVALID_INPUT;
        };
        log.warn("DataIntegrityViolationException: {} / {} -> {} {}", IntegrityViolations.describe(e),
                e.getMostSpecificCause().getMessage(), code.getHttpStatus().value(), code.getCode());

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), message(code)));
    }

    /** 원인 사슬에 DateTimeParseException 이 있으면 날짜 문제로 본다. */
    static boolean isDateFailure(Throwable t) {
        int depth = 0;
        for (Throwable c = t; c != null && depth++ < 20; c = c.getCause()) {
            if (c instanceof DateTimeParseException) {
                return true;
            }
            if (c == c.getCause()) {
                break;
            }
        }
        return false;
    }

    /**
     * 오류 코드의 메시지. {@code MessageResolver#getMessage(ErrorCode)} 는 deprecated 인데, {@code ErrorCode}
     * 타입 그대로 넘기면 자바가 그 오버로드를 고른다 — 그래서 {@link ErrorCodeProvider} 로 받아 넘긴다.
     */
    private String message(ErrorCodeProvider code) {
        return messageResolver.getMessage(code);
    }

    /** {@code Allow} 헤더 값. 지원 메서드를 모르면 null(헤더를 붙이지 않는다). */
    static String allowHeader(Set<HttpMethod> supported) {
        if (supported == null || supported.isEmpty()) {
            return null;
        }
        return supported.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(", "));
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

        String message = message(ErrorCode.INTERNAL_SERVER_ERROR);
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
