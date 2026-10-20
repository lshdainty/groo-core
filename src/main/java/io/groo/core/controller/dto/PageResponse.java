package io.groo.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 오프셋 기반 페이지네이션 응답 DTO
 * <p>
 * Spring Data의 {@link Page} 객체를 API 응답에 적합한 형태로 변환합니다.
 * 전체 항목 수(totalElements)와 전체 페이지 수(totalPages)를 포함하므로,
 * 페이지 번호 네비게이션이 필요한 UI에 적합합니다.
 *
 * <h3>응답 형식 (카카오/네이버 API 스타일 참고)</h3>
 * <pre>{@code
 * {
 *   "content": [
 *     {"id": 1, "name": "홍길동"},
 *     {"id": 2, "name": "김철수"}
 *   ],
 *   "meta": {
 *     "page": 0,
 *     "size": 20,
 *     "totalElements": 100,
 *     "totalPages": 5,
 *     "first": true,
 *     "last": false,
 *     "hasNext": true,
 *     "hasPrevious": false
 *   }
 * }
 * }</pre>
 *
 * <h3>기본 사용법</h3>
 * <pre>{@code
 * // Repository에서 Page 조회 후 변환
 * Page<User> page = userRepository.findAll(pageable);
 * PageResponse<User> response = PageResponse.of(page);
 *
 * // DTO 변환과 함께 사용
 * Page<User> page = userRepository.findAll(pageable);
 * PageResponse<UserDto> response = PageResponse.of(page, UserDto::from);
 * }</pre>
 *
 * <h3>Controller 사용 예시</h3>
 * <pre>{@code
 * @GetMapping("/users")
 * public ApiResponse<PageResponse<UserDto>> getUsers(PageRequest request) {
 *     Page<User> page = userRepository.findByStatus(Status.ACTIVE, request.toPageable());
 *     return ApiResponse.success(PageResponse.of(page, UserDto::from));
 * }
 * }</pre>
 *
 * @param <T> 데이터 타입
 * @author groo
 * @see PageRequest
 * @see SliceResponse
 */
@Schema(description = "오프셋 기반 페이지네이션 응답")
@Getter
public class PageResponse<T> {

    /**
     * 현재 페이지의 데이터 목록
     */
    @Schema(description = "데이터 목록")
    private final List<T> content;

    /**
     * 페이지 메타 정보 (페이지 번호, 전체 개수 등)
     */
    @Schema(description = "페이지 메타 정보")
    private final PageMeta meta;

    @Builder
    private PageResponse(List<T> content, PageMeta meta) {
        this.content = content;
        this.meta = meta;
    }

    /**
     * Spring Data Page를 PageResponse로 변환
     * <p>
     * 엔티티를 그대로 응답에 사용하는 경우 사용합니다.
     * 보안상 DTO 변환을 권장하므로 {@link #of(Page, Function)} 사용을 권장합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * Page<User> page = userRepository.findAll(pageable);
     * PageResponse<User> response = PageResponse.of(page);
     * }</pre>
     *
     * @param page Spring Data Page 객체
     * @param <T>  데이터 타입
     * @return PageResponse 객체
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .meta(PageMeta.of(page))
                .build();
    }

    /**
     * Spring Data Page를 DTO로 변환하여 PageResponse로 반환
     * <p>
     * 엔티티를 DTO로 변환하여 응답하는 경우 사용합니다.
     * API 응답에는 이 메서드 사용을 권장합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // 메서드 레퍼런스 사용
     * Page<User> page = userRepository.findAll(pageable);
     * PageResponse<UserDto> response = PageResponse.of(page, UserDto::from);
     *
     * // 람다 사용
     * PageResponse<UserDto> response = PageResponse.of(page,
     *     user -> new UserDto(user.getId(), user.getName()));
     * }</pre>
     *
     * @param page   Spring Data Page 객체
     * @param mapper 엔티티를 DTO로 변환하는 함수
     * @param <T>    원본 타입 (엔티티)
     * @param <R>    변환 후 타입 (DTO)
     * @return PageResponse 객체
     */
    public static <T, R> PageResponse<R> of(Page<T> page, Function<T, R> mapper) {
        List<R> content = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<R>builder()
                .content(content)
                .meta(PageMeta.of(page))
                .build();
    }

    /**
     * 빈 PageResponse 생성
     * <p>
     * 조회 결과가 없는 경우 사용합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * if (condition) {
     *     return PageResponse.empty(0, 20);
     * }
     * }</pre>
     *
     * @param page 요청한 페이지 번호
     * @param size 요청한 페이지 크기
     * @param <T>  데이터 타입
     * @return 빈 PageResponse 객체
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(List.of())
                .meta(PageMeta.empty(page, size))
                .build();
    }

    /**
     * 페이지 메타 정보
     * <p>
     * 페이지 네비게이션에 필요한 모든 정보를 포함합니다.
     */
    @Schema(description = "페이지 메타 정보")
    @Getter
    @Builder
    public static class PageMeta {

        /**
         * 현재 페이지 번호 (0부터 시작)
         */
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private final int page;

        /**
         * 페이지 당 항목 수
         */
        @Schema(description = "페이지 당 항목 수", example = "20")
        private final int size;

        /**
         * 전체 항목 수
         * <p>
         * 전체 데이터의 총 개수입니다.
         * 이 값을 구하기 위해 COUNT 쿼리가 실행됩니다.
         */
        @Schema(description = "전체 항목 수", example = "100")
        private final long totalElements;

        /**
         * 전체 페이지 수
         * <p>
         * totalElements / size (올림) 으로 계산됩니다.
         */
        @Schema(description = "전체 페이지 수", example = "5")
        private final int totalPages;

        /**
         * 첫 페이지 여부
         */
        @Schema(description = "첫 페이지 여부", example = "true")
        private final boolean first;

        /**
         * 마지막 페이지 여부
         */
        @Schema(description = "마지막 페이지 여부", example = "false")
        private final boolean last;

        /**
         * 다음 페이지 존재 여부
         * <p>
         * true이면 page + 1로 다음 페이지를 요청할 수 있습니다.
         */
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private final boolean hasNext;

        /**
         * 이전 페이지 존재 여부
         * <p>
         * true이면 page - 1로 이전 페이지를 요청할 수 있습니다.
         */
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private final boolean hasPrevious;

        /**
         * Spring Data Page에서 PageMeta 생성
         *
         * @param page Spring Data Page 객체
         * @return PageMeta 객체
         */
        public static PageMeta of(Page<?> page) {
            return PageMeta.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .first(page.isFirst())
                    .last(page.isLast())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }

        /**
         * 빈 PageMeta 생성
         *
         * @param page 페이지 번호
         * @param size 페이지 크기
         * @return 빈 PageMeta 객체
         */
        public static PageMeta empty(int page, int size) {
            return PageMeta.builder()
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
    }
}
