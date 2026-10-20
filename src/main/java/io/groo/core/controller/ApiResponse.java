package io.groo.core.controller;

import io.groo.core.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API 공통 응답 포맷
 * <p>
 * 모든 API 응답을 표준화하여 일관성 있는 응답 형식을 제공합니다.
 * 성공/실패 여부와 관계없이 동일한 구조로 응답하여 클라이언트의 파싱을 단순화합니다.
 *
 * <h3>응답 구조</h3>
 * <pre>{@code
 * {
 *   "success": true/false,
 *   "code": "응답 코드",
 *   "message": "응답 메시지",
 *   "data": { ... } 또는 null
 * }
 * }</pre>
 *
 * <h3>성공 응답 예시</h3>
 * <pre>{@code
 * // 데이터 조회 성공
 * {
 *   "success": true,
 *   "code": "COMMON_200",
 *   "message": "OK",
 *   "data": {
 *     "id": 1,
 *     "name": "홍길동",
 *     "email": "hong@example.com"
 *   }
 * }
 *
 * // 목록 조회 성공
 * {
 *   "success": true,
 *   "code": "COMMON_200",
 *   "message": "OK",
 *   "data": [
 *     {"id": 1, "name": "홍길동"},
 *     {"id": 2, "name": "김철수"}
 *   ]
 * }
 *
 * // 삭제 성공 (데이터 없음)
 * {
 *   "success": true,
 *   "code": "COMMON_200",
 *   "message": "OK",
 *   "data": null
 * }
 * }</pre>
 *
 * <h3>실패 응답 예시</h3>
 * <pre>{@code
 * // 사용자 조회 실패
 * {
 *   "success": false,
 *   "code": "USER_001",
 *   "message": "사용자를 찾을 수 없습니다.",
 *   "data": null
 * }
 *
 * // 입력값 검증 실패
 * {
 *   "success": false,
 *   "code": "COMMON_400",
 *   "message": "이메일 형식이 올바르지 않습니다.",
 *   "data": null
 * }
 *
 * // 권한 없음
 * {
 *   "success": false,
 *   "code": "COMMON_412",
 *   "message": "접근 권한이 없습니다.",
 *   "data": null
 * }
 * }</pre>
 *
 * <h3>Controller 사용 예시</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/users")
 * public class UserController {
 *
 *     // 단일 데이터 조회
 *     @GetMapping("/{id}")
 *     public ApiResponse<UserDto> getUser(@PathVariable Long id) {
 *         UserDto user = userService.findById(id);
 *         return ApiResponse.success(user);
 *     }
 *
 *     // 목록 조회
 *     @GetMapping
 *     public ApiResponse<List<UserDto>> getUsers() {
 *         List<UserDto> users = userService.findAll();
 *         return ApiResponse.success(users);
 *     }
 *
 *     // 페이지네이션 조회
 *     @GetMapping("/page")
 *     public ApiResponse<PageResponse<UserDto>> getUsersWithPage(PageRequest request) {
 *         PageResponse<UserDto> page = userService.findAll(request.toPageable());
 *         return ApiResponse.success(page);
 *     }
 *
 *     // 생성 (커스텀 메시지)
 *     @PostMapping
 *     public ApiResponse<UserDto> createUser(@RequestBody CreateUserRequest request) {
 *         UserDto user = userService.create(request);
 *         return ApiResponse.success("사용자가 생성되었습니다.", user);
 *     }
 *
 *     // 삭제 (데이터 없음)
 *     @DeleteMapping("/{id}")
 *     public ApiResponse<Void> deleteUser(@PathVariable Long id) {
 *         userService.delete(id);
 *         return ApiResponse.success();
 *     }
 * }
 * }</pre>
 *
 * @param <T> 응답 데이터 타입
 * @author groo
 * @see GlobalExceptionHandler
 * @see ErrorCode
 */
@Schema(description = "API 공통 응답")
@Getter
public class ApiResponse<T> {

    /**
     * 요청 성공 여부
     * <p>
     * true: 요청이 정상적으로 처리됨<br>
     * false: 요청 처리 중 오류 발생
     */
    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean success;

    /**
     * 응답 코드
     * <p>
     * 성공 시: "COMMON_200"<br>
     * 실패 시: 에러 코드 (예: "USER_001", "COMMON_400")
     * <p>
     * 에러 코드 형식: {도메인}_{번호}
     * <ul>
     *   <li>COMMON_*: 공통 에러</li>
     *   <li>USER_*: 사용자 관련 에러</li>
     *   <li>VACATION_*: 휴가 관련 에러</li>
     *   <li>등...</li>
     * </ul>
     */
    @Schema(description = "응답 코드", example = "COMMON_200")
    private final String code;

    /**
     * 응답 메시지
     * <p>
     * 성공 시: "OK" 또는 커스텀 메시지<br>
     * 실패 시: 에러 메시지 (다국어 지원)
     */
    @Schema(description = "응답 메시지", example = "OK")
    private final String message;

    /**
     * 응답 데이터
     * <p>
     * 성공 시: 요청한 데이터 (단일 객체, 목록, 페이지 등)<br>
     * 실패 시: null
     */
    @Schema(description = "응답 데이터")
    private final T data;

    /**
     * ApiResponse 생성자 (private)
     * <p>
     * 직접 생성하지 않고 static 팩토리 메서드를 사용합니다.
     *
     * @param success 성공 여부
     * @param code    응답 코드
     * @param message 응답 메시지
     * @param data    응답 데이터
     */
    @JsonCreator
    private ApiResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("data") T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ========================================
    // 성공 응답
    // ========================================

    /**
     * 성공 응답 생성 (데이터 포함)
     * <p>
     * 가장 일반적인 성공 응답입니다. 메시지는 "OK"로 고정됩니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 단일 객체 반환
     * @GetMapping("/{id}")
     * public ApiResponse<UserDto> getUser(@PathVariable Long id) {
     *     UserDto user = userService.findById(id);
     *     return ApiResponse.success(user);
     * }
     *
     * // 목록 반환
     * @GetMapping
     * public ApiResponse<List<UserDto>> getUsers() {
     *     return ApiResponse.success(userService.findAll());
     * }
     *
     * // 페이지네이션 반환
     * @GetMapping("/page")
     * public ApiResponse<PageResponse<UserDto>> getUsersPage(PageRequest request) {
     *     return ApiResponse.success(userService.findAll(request));
     * }
     * }</pre>
     *
     * @param data 응답 데이터
     * @param <T>  데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                ErrorCode.SUCCESS.getCode(),
                "OK",
                data
        );
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     * <p>
     * 삭제, 업데이트 등 반환할 데이터가 없는 경우 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 삭제
     * @DeleteMapping("/{id}")
     * public ApiResponse<Void> deleteUser(@PathVariable Long id) {
     *     userService.delete(id);
     *     return ApiResponse.success();
     * }
     *
     * // 상태 변경
     * @PatchMapping("/{id}/activate")
     * public ApiResponse<Void> activateUser(@PathVariable Long id) {
     *     userService.activate(id);
     *     return ApiResponse.success();
     * }
     * }</pre>
     *
     * @param <T> 데이터 타입 (Void 권장)
     * @return 성공 ApiResponse (data: null)
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(
                true,
                ErrorCode.SUCCESS.getCode(),
                "OK",
                null
        );
    }

    /**
     * 성공 응답 생성 (커스텀 메시지 + 데이터)
     * <p>
     * 특별한 성공 메시지를 전달해야 하는 경우 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 생성 완료 메시지
     * @PostMapping
     * public ApiResponse<UserDto> createUser(@RequestBody CreateUserRequest request) {
     *     UserDto user = userService.create(request);
     *     return ApiResponse.success("사용자가 성공적으로 생성되었습니다.", user);
     * }
     *
     * // 처리 완료 메시지
     * @PostMapping("/batch")
     * public ApiResponse<BatchResult> processBatch(@RequestBody BatchRequest request) {
     *     BatchResult result = batchService.process(request);
     *     return ApiResponse.success("100건의 데이터가 처리되었습니다.", result);
     * }
     * }</pre>
     *
     * @param message 커스텀 메시지
     * @param data    응답 데이터
     * @param <T>     데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                ErrorCode.SUCCESS.getCode(),
                message,
                data
        );
    }

    // ========================================
    // 실패 응답
    // ========================================

    /**
     * 실패 응답 생성
     * <p>
     * {@link GlobalExceptionHandler}에서 예외를 처리할 때 사용합니다.
     * 일반적으로 Controller에서 직접 호출하지 않고, 예외를 throw하면
     * GlobalExceptionHandler가 자동으로 이 메서드를 호출합니다.
     *
     * <h4>GlobalExceptionHandler 내부 사용 예시</h4>
     * <pre>{@code
     * @ExceptionHandler(EntityNotFoundException.class)
     * public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
     *     ErrorCodeProvider errorCode = e.getErrorCode();
     *     String message = messageResolver.getMessage(errorCode);
     *
     *     ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), message);
     *
     *     return ResponseEntity
     *         .status(errorCode.getHttpStatus())
     *         .body(response);
     * }
     * }</pre>
     *
     * <h4>Service에서 예외 발생 예시 (권장)</h4>
     * <pre>{@code
     * // Service에서 예외 throw - GlobalExceptionHandler가 자동 처리
     * public UserDto findById(Long id) {
     *     return userRepository.findById(id)
     *         .map(UserDto::from)
     *         .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
     * }
     * }</pre>
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @param <T>     데이터 타입 (항상 null)
     * @return 실패 ApiResponse (data: null)
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null
        );
    }
}
