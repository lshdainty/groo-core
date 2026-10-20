package io.groo.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 커서 기반 페이지네이션 요청 DTO
 * <p>
 * 대용량 데이터에서 일정한 성능을 보장하는 페이지네이션 방식입니다.
 * 오프셋 기반과 달리 OFFSET을 사용하지 않아 뒤쪽 페이지에서도 빠른 성능을 유지합니다.
 *
 * <h3>오프셋 vs 커서 기반 비교</h3>
 * <table border="1">
 *   <tr><th>항목</th><th>오프셋 기반</th><th>커서 기반</th></tr>
 *   <tr><td>성능</td><td>뒤로 갈수록 느림</td><td>일정</td></tr>
 *   <tr><td>페이지 이동</td><td>임의 페이지 이동 가능</td><td>순차 이동만 가능</td></tr>
 *   <tr><td>데이터 변경</td><td>중복/누락 발생 가능</td><td>안정적</td></tr>
 *   <tr><td>적합한 경우</td><td>관리자 페이지</td><td>SNS 피드, 채팅</td></tr>
 * </table>
 *
 * <h3>요청 예시</h3>
 * <pre>
 * GET /messages?size=20                    // 첫 페이지
 * GET /messages?cursor=1234&size=20        // ID 기반 커서
 * GET /messages?cursor=eyJpZCI6MTIzNH0=&size=20  // 인코딩된 커서
 * </pre>
 *
 * <h3>Repository 구현 예시 (ID 기반 커서)</h3>
 * <pre>{@code
 * public List<Message> findByCursor(Long cursor, int limit) {
 *     BooleanBuilder builder = new BooleanBuilder();
 *
 *     // 커서가 있으면 해당 ID보다 작은 것만 조회 (내림차순)
 *     if (cursor != null) {
 *         builder.and(message.id.lt(cursor));
 *     }
 *
 *     return queryFactory
 *         .selectFrom(message)
 *         .where(builder)
 *         .orderBy(message.id.desc())
 *         .limit(limit)  // size + 1로 조회하여 hasNext 판단
 *         .fetch();
 * }
 * }</pre>
 *
 * <h3>Service/Controller 사용 예시</h3>
 * <pre>{@code
 * @GetMapping("/messages")
 * public ApiResponse<CursorResponse<MessageDto>> getMessages(CursorRequest request) {
 *     List<Message> messages = messageRepository.findByCursor(
 *         request.getCursorAsLong(),
 *         request.getLimit()  // size + 1
 *     );
 *
 *     return ApiResponse.success(
 *         CursorResponse.of(messages, request.getValidSize(),
 *             m -> String.valueOf(m.getId()))
 *     );
 * }
 * }</pre>
 *
 * @author groo
 * @see CursorResponse
 * @see PageRequest
 */
@Schema(description = "커서 기반 페이지네이션 요청")
@Getter
@Setter
@NoArgsConstructor
public class CursorRequest {

    /**
     * 기본 조회 개수
     */
    private static final int DEFAULT_SIZE = 20;

    /**
     * 최대 허용 조회 개수 (성능 보호)
     */
    private static final int MAX_SIZE = 100;

    /**
     * 커서 값
     * <p>
     * 마지막으로 조회한 항목의 식별자입니다.
     * null이면 처음부터 조회합니다.
     * <p>
     * 커서 값은 다음과 같은 형태로 사용할 수 있습니다:
     * <ul>
     *   <li>ID 기반: "1234" (단순 숫자 ID)</li>
     *   <li>복합 키: "eyJpZCI6MTIzNCwidGltZSI6MTY5OTk5OTk5OX0=" (Base64 인코딩된 JSON)</li>
     *   <li>타임스탬프: "2024-01-01T00:00:00" (시간 기반)</li>
     * </ul>
     */
    @Schema(description = "커서 값 (마지막 조회 항목의 ID 또는 인코딩된 값, null이면 첫 페이지)", example = "1234")
    private String cursor;

    /**
     * 조회할 항목 수
     * <p>
     * 최소 1, 최대 100까지 허용됩니다.
     */
    @Schema(description = "조회할 항목 수 (최대 100)", example = "20", defaultValue = "20")
    private int size = DEFAULT_SIZE;

    /**
     * 커서와 조회 개수를 지정하여 생성
     *
     * @param cursor 커서 값
     * @param size   조회할 항목 수
     */
    public CursorRequest(String cursor, int size) {
        this.cursor = cursor;
        this.size = size;
    }

    /**
     * 커서를 Long 타입으로 변환
     * <p>
     * ID 기반 커서에서 사용합니다.
     * 커서가 null이거나 숫자가 아닌 경우 null을 반환합니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * Long cursor = request.getCursorAsLong();
     * if (cursor != null) {
     *     builder.and(entity.id.lt(cursor));  // 커서 이전 데이터 조회
     * }
     * }</pre>
     *
     * @return 커서 값 (Long), 변환 불가능하면 null
     */
    public Long getCursorAsLong() {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 유효한 size 반환
     * <p>
     * size가 1 ~ 100 범위를 벗어나면 자동으로 보정합니다.
     *
     * @return 유효한 size 값 (1 ~ 100)
     */
    public int getValidSize() {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    /**
     * 조회 시 사용할 limit 값 반환
     * <p>
     * hasNext 판단을 위해 size + 1을 반환합니다.
     * Repository에서 이 값으로 조회하면 다음 페이지 존재 여부를 알 수 있습니다.
     *
     * <h4>사용 예시</h4>
     * <pre>{@code
     * // Repository
     * List<Message> messages = queryFactory
     *     .selectFrom(message)
     *     .limit(request.getLimit())  // size + 1
     *     .fetch();
     *
     * // size + 1개가 조회되면 다음 페이지가 있음
     * boolean hasNext = messages.size() > request.getValidSize();
     * }</pre>
     *
     * @return limit 값 (size + 1)
     */
    public int getLimit() {
        return getValidSize() + 1;
    }

    /**
     * 첫 페이지 요청인지 확인
     * <p>
     * 커서가 없으면 첫 페이지 요청입니다.
     *
     * @return 첫 페이지 요청이면 true
     */
    public boolean isFirstPage() {
        return cursor == null || cursor.isBlank();
    }
}
