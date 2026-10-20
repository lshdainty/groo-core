package io.groo.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 오프셋 기반 페이지네이션 요청 DTO
 * <p>
 * 관리자 페이지, 테이블 형태 UI 등 페이지 번호로 직접 이동이 필요한 경우에 사용합니다.
 * 데이터가 많아질수록 뒤쪽 페이지 조회 성능이 저하될 수 있으므로,
 * 대용량 데이터에서는 {@link CursorRequest}를 사용하는 것을 권장합니다.
 *
 * <h3>기본 사용법</h3>
 * <pre>{@code
 * // Controller에서 파라미터로 직접 바인딩
 * @GetMapping("/users")
 * public ApiResponse<PageResponse<UserDto>> getUsers(PageRequest request) {
 *     Page<User> page = userRepository.findAll(request.toPageable());
 *     return ApiResponse.success(PageResponse.of(page, UserDto::from));
 * }
 * }</pre>
 *
 * <h3>정렬 조건 지정</h3>
 * <pre>{@code
 * // 단일 정렬
 * GET /users?page=0&size=20&sort=createdAt&direction=DESC
 *
 * // 다중 정렬 (toPageable 메서드에 직접 전달)
 * Pageable pageable = request.toPageable("createdAt,DESC", "name,ASC");
 * }</pre>
 *
 * <h3>요청 예시</h3>
 * <pre>
 * GET /users?page=0&size=20
 * GET /users?page=1&size=10&sort=name&direction=ASC
 * </pre>
 *
 * @author groo
 * @see PageResponse
 * @see CursorRequest
 */
@Schema(description = "오프셋 기반 페이지네이션 요청")
@Getter
@Setter
@NoArgsConstructor
public class PageRequest {

    /**
     * 기본 페이지 크기
     */
    private static final int DEFAULT_SIZE = 20;

    /**
     * 최대 허용 페이지 크기 (성능 보호)
     */
    private static final int MAX_SIZE = 100;

    /**
     * 페이지 번호 (0부터 시작)
     * <p>
     * 첫 번째 페이지는 0, 두 번째 페이지는 1입니다.
     */
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    private int page = 0;

    /**
     * 페이지 당 조회할 항목 수
     * <p>
     * 최소 1, 최대 100까지 허용됩니다.
     * 범위를 벗어나면 자동으로 보정됩니다.
     */
    @Schema(description = "페이지 당 항목 수 (최대 100)", example = "20", defaultValue = "20")
    private int size = DEFAULT_SIZE;

    /**
     * 정렬 기준 필드명
     * <p>
     * 엔티티의 필드명을 지정합니다. (예: createdAt, name, id)
     * null이면 정렬을 적용하지 않습니다.
     */
    @Schema(description = "정렬 필드명", example = "createdAt")
    private String sort;

    /**
     * 정렬 방향
     * <p>
     * ASC(오름차순) 또는 DESC(내림차순)를 지정합니다.
     * 잘못된 값이 들어오면 DESC로 기본 처리됩니다.
     */
    @Schema(description = "정렬 방향 (ASC: 오름차순, DESC: 내림차순)", example = "DESC", defaultValue = "DESC", allowableValues = {"ASC", "DESC"})
    private String direction = "DESC";

    /**
     * 페이지 번호와 크기를 지정하여 생성
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 당 항목 수
     */
    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    /**
     * 모든 필드를 지정하여 생성
     *
     * @param page      페이지 번호 (0부터 시작)
     * @param size      페이지 당 항목 수
     * @param sort      정렬 필드명
     * @param direction 정렬 방향 (ASC/DESC)
     */
    public PageRequest(int page, int size, String sort, String direction) {
        this.page = page;
        this.size = size;
        this.sort = sort;
        this.direction = direction;
    }

    /**
     * Spring Data의 Pageable 객체로 변환
     * <p>
     * 이 메서드는 page와 size를 유효한 범위로 자동 보정합니다:
     * <ul>
     *   <li>page: 음수인 경우 0으로 보정</li>
     *   <li>size: 1 ~ 100 범위로 보정</li>
     * </ul>
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // Repository 조회 시 사용
     * Page<User> users = userRepository.findAll(request.toPageable());
     *
     * // QueryDSL과 함께 사용
     * JPAQuery<User> query = queryFactory.selectFrom(user);
     * query.offset(request.toPageable().getOffset());
     * query.limit(request.toPageable().getPageSize());
     * }</pre>
     *
     * @return Spring Data Pageable 객체
     */
    public Pageable toPageable() {
        int validSize = Math.min(Math.max(size, 1), MAX_SIZE);
        int validPage = Math.max(page, 0);

        if (sort != null && !sort.isBlank()) {
            Sort.Direction dir = parseDirection(direction);
            return org.springframework.data.domain.PageRequest.of(validPage, validSize, Sort.by(dir, sort));
        }
        return org.springframework.data.domain.PageRequest.of(validPage, validSize);
    }

    /**
     * 여러 정렬 조건을 적용한 Pageable 생성
     * <p>
     * 복합 정렬이 필요한 경우 사용합니다.
     * 각 정렬 조건은 "필드명,방향" 형식으로 지정합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 생성일 내림차순 후 이름 오름차순
     * Pageable pageable = request.toPageable("createdAt,DESC", "name,ASC");
     *
     * // 방향 생략 시 DESC가 기본값
     * Pageable pageable = request.toPageable("createdAt", "name,ASC");
     * }</pre>
     *
     * @param sorts 정렬 조건 배열 (형식: "필드명,방향")
     * @return Spring Data Pageable 객체
     */
    public Pageable toPageable(String... sorts) {
        int validSize = Math.min(Math.max(size, 1), MAX_SIZE);
        int validPage = Math.max(page, 0);

        if (sorts != null && sorts.length > 0) {
            Sort combinedSort = parseMultipleSorts(sorts);
            return org.springframework.data.domain.PageRequest.of(validPage, validSize, combinedSort);
        }
        return org.springframework.data.domain.PageRequest.of(validPage, validSize);
    }

    /**
     * 정렬 방향 문자열을 Sort.Direction으로 변환
     *
     * @param direction 정렬 방향 문자열
     * @return Sort.Direction (잘못된 값인 경우 DESC 반환)
     */
    private Sort.Direction parseDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            return Sort.Direction.DESC;
        }
    }

    /**
     * 여러 정렬 조건 문자열을 파싱하여 Sort 객체 생성
     *
     * @param sorts 정렬 조건 배열
     * @return 복합 Sort 객체
     */
    private Sort parseMultipleSorts(String[] sorts) {
        Sort result = Sort.unsorted();
        for (String sortStr : sorts) {
            String[] parts = sortStr.split(",");
            if (parts.length >= 1) {
                String field = parts[0].trim();
                Sort.Direction dir = parts.length >= 2 ? parseDirection(parts[1].trim()) : Sort.Direction.DESC;
                result = result.and(Sort.by(dir, field));
            }
        }
        return result;
    }
}
