package io.groo.core.audit;

import java.time.LocalDateTime;

/**
 * 접속기록 한 건
 * <p>
 * 「개인정보의 안전성 확보조치 기준」 제8조 제1항이 요구하는 다섯 항목을 그대로 담습니다.
 *
 * <table border="1">
 *   <caption>고시 요구 항목과의 대응</caption>
 *   <tr><th>고시 항목</th><th>필드</th></tr>
 *   <tr><td>계정</td><td>{@link #actorId()}</td></tr>
 *   <tr><td>접속일시</td><td>{@link #occurredAt()}</td></tr>
 *   <tr><td>접속지 정보</td><td>{@link #ipAddress()}</td></tr>
 *   <tr><td>처리한 정보주체 정보</td><td>{@link #targetType()} + {@link #targetId()}</td></tr>
 *   <tr><td>수행업무</td><td>{@link #action()}</td></tr>
 * </table>
 *
 * @param actorId    수행자(개인정보취급자) 계정. 인증 정보를 못 얻으면 null
 * @param action     수행업무
 * @param targetType 대상 유형 (USER, DUES, …)
 * @param targetId   처리한 정보주체 식별자. 목록 조회처럼 특정되지 않으면 null
 * @param detail     맥락 메모 (개인정보 금지)
 * @param ipAddress  접속지 IP
 * @param occurredAt 발생 시각 [UTC]
 *
 * @author groo
 * @see AuditAccessPort
 */
public record AccessLogEntry(
        String actorId,
        AccessAction action,
        String targetType,
        String targetId,
        String detail,
        String ipAddress,
        LocalDateTime occurredAt
) {
}
