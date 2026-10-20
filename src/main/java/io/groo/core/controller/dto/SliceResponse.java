package io.groo.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

/**
 * Slice 기반 페이지네이션 응답 DTO (무한 스크롤용)
 * <p>
 * Spring Data의 {@link Slice} 객체를 API 응답에 적합한 형태로 변환합니다.
 * {@link PageResponse}와 달리 전체 개수(totalCount)를 조회하지 않아 성능상 유리합니다.
 *
 * <h3>PageResponse vs SliceResponse</h3>
 * <table border="1">
 *   <tr><th>항목</th><th>PageResponse</th><th>SliceResponse</th></tr>
 *   <tr><td>전체 개수</td><td>O (COUNT 쿼리 실행)</td><td>X</td></tr>
 *   <tr><td>전체 페이지 수</td><td>O</td><td>X</td></tr>
 *   <tr><td>성능</td><td>COUNT 쿼리 비용</td><td>더 빠름</td></tr>
 *   <tr><td>적합한 UI</td><td>페이지 네비게이션</td><td>무한 스크롤, 더보기</td></tr>
 * </table>
 *
 * <h3>응답 형식</h3>
 * <pre>{@code
 * {
 *   "content": [
 *     {"id": 1, "name": "홍길동"},
 *     {"id": 2, "name": "김철수"}
 *   ],
 *   "meta": {
 *     "page": 0,
 *     "size": 20,
 *     "numberOfElements": 20,
 *     "first": true,
 *     "last": false,
 *     "hasNext": true,
 *     "hasPrevious": false
 *   }
 * }
 * }</pre>
 *
 * <h3>Repository 정의</h3>
 * <pre>{@code
 * // JpaRepository에서 Slice 반환 메서드 정의
 * public interface UserRepository extends JpaRepository<User, Long> {
 *     Slice<User> findByStatus(Status status, Pageable pageable);
 * }
 * }</pre>
 *
 * <h3>Controller 사용 예시</h3>
 * <pre>{@code
 * @GetMapping("/feeds")
 * public ApiResponse<SliceResponse<FeedDto>> getFeeds(PageRequest request) {
 *     Slice<Feed> slice = feedRepository.findByUserId(userId, request.toPageable());
 *     return ApiResponse.success(SliceResponse.of(slice, FeedDto::from));
 * }
 * }</pre>
 *
 * @param <T> 데이터 타입
 * @author groo
 * @see PageResponse
 * @see CursorResponse
 */
@Schema(description = "Slice 기반 페이지네이션 응답 (무한 스크롤용)")
@Getter
public class SliceResponse<T> {

    /**
     * 현재 슬라이스의 데이터 목록
     */
    @Schema(description = "데이터 목록")
    private final List<T> content;

    /**
     * 슬라이스 메타 정보
     */
    @Schema(description = "슬라이스 메타 정보")
    private final SliceMeta meta;

    @Builder
    private SliceResponse(List<T> content, SliceMeta meta) {
        this.content = content;
        this.meta = meta;
    }

    /**
     * Spring Data Slice를 SliceResponse로 변환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * Slice<Feed> slice = feedRepository.findByUserId(userId, pageable);
     * SliceResponse<Feed> response = SliceResponse.of(slice);
     * }</pre>
     *
     * @param slice Spring Data Slice 객체
     * @param <T>   데이터 타입
     * @return SliceResponse 객체
     */
    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return SliceResponse.<T>builder()
                .content(slice.getContent())
                .meta(SliceMeta.of(slice))
                .build();
    }

    /**
     * Spring Data Slice를 DTO로 변환하여 SliceResponse로 반환
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * Slice<Feed> slice = feedRepository.findByUserId(userId, pageable);
     * SliceResponse<FeedDto> response = SliceResponse.of(slice, FeedDto::from);
     * }</pre>
     *
     * @param slice  Spring Data Slice 객체
     * @param mapper 엔티티를 DTO로 변환하는 함수
     * @param <T>    원본 타입 (엔티티)
     * @param <R>    변환 후 타입 (DTO)
     * @return SliceResponse 객체
     */
    public static <T, R> SliceResponse<R> of(Slice<T> slice, Function<T, R> mapper) {
        List<R> content = slice.getContent().stream()
                .map(mapper)
                .toList();

        return SliceResponse.<R>builder()
                .content(content)
                .meta(SliceMeta.of(slice))
                .build();
    }

    /**
     * 빈 SliceResponse 생성
     *
     * @param page 요청한 페이지 번호
     * @param size 요청한 페이지 크기
     * @param <T>  데이터 타입
     * @return 빈 SliceResponse 객체
     */
    public static <T> SliceResponse<T> empty(int page, int size) {
        return SliceResponse.<T>builder()
                .content(List.of())
                .meta(SliceMeta.empty(page, size))
                .build();
    }

    /**
     * 슬라이스 메타 정보
     * <p>
     * PageMeta와 달리 totalElements, totalPages가 없습니다.
     */
    @Schema(description = "슬라이스 메타 정보")
    @Getter
    @Builder
    public static class SliceMeta {

        /**
         * 현재 페이지 번호 (0부터 시작)
         */
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private final int page;

        /**
         * 요청한 페이지 크기
         */
        @Schema(description = "요청한 페이지 크기", example = "20")
        private final int size;

        /**
         * 현재 슬라이스의 실제 항목 수
         * <p>
         * 마지막 페이지에서는 size보다 작을 수 있습니다.
         */
        @Schema(description = "현재 슬라이스의 실제 항목 수", example = "20")
        private final int numberOfElements;

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
         * 무한 스크롤에서 "더보기" 버튼 표시 여부를 결정하는 데 사용됩니다.
         */
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private final boolean hasNext;

        /**
         * 이전 페이지 존재 여부
         */
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private final boolean hasPrevious;

        /**
         * Spring Data Slice에서 SliceMeta 생성
         *
         * @param slice Spring Data Slice 객체
         * @return SliceMeta 객체
         */
        public static SliceMeta of(Slice<?> slice) {
            return SliceMeta.builder()
                    .page(slice.getNumber())
                    .size(slice.getSize())
                    .numberOfElements(slice.getNumberOfElements())
                    .first(slice.isFirst())
                    .last(slice.isLast())
                    .hasNext(slice.hasNext())
                    .hasPrevious(slice.hasPrevious())
                    .build();
        }

        /**
         * 빈 SliceMeta 생성
         *
         * @param page 페이지 번호
         * @param size 페이지 크기
         * @return 빈 SliceMeta 객체
         */
        public static SliceMeta empty(int page, int size) {
            return SliceMeta.builder()
                    .page(page)
                    .size(size)
                    .numberOfElements(0)
                    .first(true)
                    .last(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
    }
}
