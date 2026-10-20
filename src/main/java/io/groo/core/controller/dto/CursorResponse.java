package io.groo.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

/**
 * 커서 기반 페이지네이션 응답 DTO
 * <p>
 * 대용량 데이터에서 일정한 성능을 보장하는 페이지네이션 응답입니다.
 * Slack API 스타일을 참고하여 설계되었습니다.
 *
 * <h3>응답 형식</h3>
 * <pre>{@code
 * {
 *   "content": [
 *     {"id": 100, "text": "메시지 100"},
 *     {"id": 99, "text": "메시지 99"},
 *     ...
 *   ],
 *   "meta": {
 *     "size": 20,
 *     "hasNext": true,
 *     "nextCursor": "80"
 *   }
 * }
 * }</pre>
 *
 * <h3>Repository에서 size + 1 조회 패턴</h3>
 * <pre>{@code
 * // Repository: limit을 size + 1로 조회
 * List<Message> messages = queryFactory
 *     .selectFrom(message)
 *     .where(cursor != null ? message.id.lt(cursor) : null)
 *     .orderBy(message.id.desc())
 *     .limit(request.getLimit())  // size + 1
 *     .fetch();
 *
 * // Service: CursorResponse.of()가 자동으로 hasNext 판단
 * return CursorResponse.of(messages, request.getValidSize(),
 *     m -> String.valueOf(m.getId()));
 * }</pre>
 *
 * @param <T> 데이터 타입
 * @author groo
 * @see CursorRequest
 * @see SliceResponse
 */
@Schema(description = "커서 기반 페이지네이션 응답")
@Getter
public class CursorResponse<T> {

    /**
     * 데이터 목록
     */
    @Schema(description = "데이터 목록")
    private final List<T> content;

    /**
     * 커서 메타 정보
     */
    @Schema(description = "커서 메타 정보")
    private final CursorMeta meta;

    @Builder
    private CursorResponse(List<T> content, CursorMeta meta) {
        this.content = content;
        this.meta = meta;
    }

    /**
     * CursorResponse 생성 (문자열 커서)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * List<Message> content = messages.subList(0, Math.min(messages.size(), size));
     * boolean hasNext = messages.size() > size;
     * String nextCursor = hasNext ? String.valueOf(content.get(content.size() - 1).getId()) : null;
     *
     * return CursorResponse.of(content, size, hasNext, nextCursor);
     * }</pre>
     *
     * @param content    데이터 목록
     * @param size       요청한 size
     * @param hasNext    다음 페이지 존재 여부
     * @param nextCursor 다음 커서 값
     * @param <T>        데이터 타입
     * @return CursorResponse 객체
     */
    public static <T> CursorResponse<T> of(List<T> content, int size, boolean hasNext, String nextCursor) {
        return CursorResponse.<T>builder()
                .content(content)
                .meta(CursorMeta.builder()
                        .size(size)
                        .hasNext(hasNext)
                        .nextCursor(hasNext ? nextCursor : null)
                        .build())
                .build();
    }

    /**
     * CursorResponse 생성 (Long ID 커서)
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * return CursorResponse.of(content, size, hasNext, lastMessage.getId());
     * }</pre>
     *
     * @param content 데이터 목록
     * @param size    요청한 size
     * @param hasNext 다음 페이지 존재 여부
     * @param lastId  마지막 항목의 ID
     * @param <T>     데이터 타입
     * @return CursorResponse 객체
     */
    public static <T> CursorResponse<T> of(List<T> content, int size, boolean hasNext, Long lastId) {
        return of(content, size, hasNext, lastId != null ? String.valueOf(lastId) : null);
    }

    /**
     * size + 1 조회 결과로 CursorResponse 생성 (권장)
     * <p>
     * Repository에서 size + 1개를 조회한 후 이 메서드를 사용하면
     * hasNext 판단과 content 트리밍을 자동으로 처리합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // Repository에서 size + 1개 조회
     * List<Message> fetched = messageRepository.findByCursor(cursor, request.getLimit());
     *
     * // 자동으로 hasNext 판단 및 트리밍
     * return CursorResponse.of(fetched, request.getValidSize(),
     *     message -> String.valueOf(message.getId()));
     * }</pre>
     *
     * @param fetchedContent  size + 1개로 조회한 데이터 목록
     * @param size            요청한 size
     * @param cursorExtractor 커서 값 추출 함수 (마지막 항목에서 다음 커서 추출)
     * @param <T>             데이터 타입
     * @return CursorResponse 객체
     */
    public static <T> CursorResponse<T> of(List<T> fetchedContent, int size, Function<T, String> cursorExtractor) {
        boolean hasNext = fetchedContent.size() > size;
        List<T> content = hasNext ? fetchedContent.subList(0, size) : fetchedContent;
        String nextCursor = hasNext && !content.isEmpty()
                ? cursorExtractor.apply(content.get(content.size() - 1))
                : null;

        return CursorResponse.<T>builder()
                .content(content)
                .meta(CursorMeta.builder()
                        .size(size)
                        .hasNext(hasNext)
                        .nextCursor(nextCursor)
                        .build())
                .build();
    }

    /**
     * size + 1 조회 결과를 DTO로 변환하여 CursorResponse 생성 (권장)
     * <p>
     * 엔티티를 DTO로 변환하면서 동시에 페이지네이션 처리를 합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * List<Message> fetched = messageRepository.findByCursor(cursor, request.getLimit());
     *
     * return CursorResponse.of(
     *     fetched,
     *     request.getValidSize(),
     *     MessageDto::from,                          // DTO 변환
     *     message -> String.valueOf(message.getId()) // 커서 추출 (엔티티 기준)
     * );
     * }</pre>
     *
     * @param fetchedContent  size + 1개로 조회한 데이터 목록
     * @param size            요청한 size
     * @param mapper          엔티티를 DTO로 변환하는 함수
     * @param cursorExtractor 커서 값 추출 함수 (변환 전 엔티티 기준)
     * @param <T>             원본 타입 (엔티티)
     * @param <R>             변환 후 타입 (DTO)
     * @return CursorResponse 객체
     */
    public static <T, R> CursorResponse<R> of(List<T> fetchedContent, int size,
                                               Function<T, R> mapper, Function<T, String> cursorExtractor) {
        boolean hasNext = fetchedContent.size() > size;
        List<T> trimmedContent = hasNext ? fetchedContent.subList(0, size) : fetchedContent;
        String nextCursor = hasNext && !trimmedContent.isEmpty()
                ? cursorExtractor.apply(trimmedContent.get(trimmedContent.size() - 1))
                : null;

        List<R> content = trimmedContent.stream().map(mapper).toList();

        return CursorResponse.<R>builder()
                .content(content)
                .meta(CursorMeta.builder()
                        .size(size)
                        .hasNext(hasNext)
                        .nextCursor(nextCursor)
                        .build())
                .build();
    }

    /**
     * 빈 CursorResponse 생성
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * if (userId == null) {
     *     return CursorResponse.empty(20);
     * }
     * }</pre>
     *
     * @param size 요청한 size
     * @param <T>  데이터 타입
     * @return 빈 CursorResponse 객체
     */
    public static <T> CursorResponse<T> empty(int size) {
        return CursorResponse.<T>builder()
                .content(List.of())
                .meta(CursorMeta.builder()
                        .size(size)
                        .hasNext(false)
                        .nextCursor(null)
                        .build())
                .build();
    }

    /**
     * 커서 메타 정보
     * <p>
     * 다음 페이지 조회에 필요한 커서 정보를 포함합니다.
     */
    @Schema(description = "커서 메타 정보")
    @Getter
    @Builder
    public static class CursorMeta {

        /**
         * 요청한 size
         */
        @Schema(description = "요청한 조회 개수", example = "20")
        private final int size;

        /**
         * 다음 페이지 존재 여부
         * <p>
         * true이면 nextCursor를 사용하여 다음 페이지를 요청할 수 있습니다.
         */
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private final boolean hasNext;

        /**
         * 다음 페이지 조회를 위한 커서 값
         * <p>
         * hasNext가 false이면 null입니다.
         * 다음 요청 시 이 값을 cursor 파라미터로 전달합니다.
         */
        @Schema(description = "다음 커서 값 (hasNext가 false면 null)", example = "80")
        private final String nextCursor;
    }
}
