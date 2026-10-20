package io.groo.core.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 개인정보 접속기록 대상 표시
 * <p>
 * 이 어노테이션이 붙은 메서드가 <strong>정상 종료</strong>하면 접속기록 한 건이 남습니다.
 * 예외로 끝난 요청은 개인정보를 처리하지 못한 것이므로 기록하지 않습니다.
 *
 * <h3>전 API 인터셉터가 아니라 어노테이션인 이유</h3>
 * <p>
 * 모든 요청을 자동으로 기록하면 본인 데이터 조회까지 전부 쌓여 노이즈에 묻힙니다.
 * 접속기록은 개인정보취급자가 <strong>타인의</strong> 개인정보를 처리한 기록이므로,
 * 어디를 남길지는 개발자가 명시적으로 고릅니다.
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 전 직원 목록 조회 — 여러 정보주체의 개인정보를 한 번에 본다
 * @AuditAccess(action = AccessAction.LIST, targetType = "USER")
 * @GetMapping("/api/v1/users")
 * public ApiResponse<List<UserResponse>> getUsers() { ... }
 *
 * // 특정 사용자 상세 — targetId 로 "처리한 정보주체" 를 남긴다
 * @AuditAccess(action = AccessAction.READ, targetType = "USER", targetId = "#userId")
 * @GetMapping("/api/v1/users/{userId}")
 * public ApiResponse<UserResponse> getUser(@PathVariable String userId) { ... }
 *
 * // 파라미터 이름이 유지되지 않는 환경이면 위치로도 지정할 수 있다
 * @AuditAccess(action = AccessAction.DELETE, targetType = "USER", targetId = "#p0")
 * public void deleteUser(String userId) { ... }
 * }</pre>
 *
 * <h3>주의</h3>
 * <ul>
 *   <li>Spring AOP 프록시를 타므로 <strong>같은 클래스 내부 호출에는 적용되지 않습니다</strong>.</li>
 *   <li>기록 실패가 본 작업을 되돌리지 않습니다 — 감사 기록 때문에 사용자 요청이 깨지면 안 되기 때문입니다.</li>
 *   <li>{@code targetId} 에 개인정보(이름·이메일)를 넣지 마세요. 식별자만 남깁니다.</li>
 * </ul>
 *
 * @author groo
 * @see AccessAction 수행업무 구분
 * @see AuditAccessPort 실제 저장을 담당하는 포트
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAccess {

    /**
     * 수행업무 (조회 · 수정 · 다운로드 등)
     */
    AccessAction action();

    /**
     * 대상 유형 (예: {@code "USER"}, {@code "DUES"}, {@code "VACATION"})
     * <p>
     * 어떤 종류의 개인정보를 다뤘는지 구분합니다.
     */
    String targetType();

    /**
     * 처리한 정보주체 식별자를 뽑는 SpEL 표현식 (예: {@code "#userId"}, {@code "#p0"})
     * <p>
     * 목록 조회처럼 대상이 특정되지 않으면 비워둡니다. 표현식 평가에 실패해도
     * 기록 자체는 남습니다(식별자만 비어 있음).
     */
    String targetId() default "";

    /**
     * 기록에 함께 남길 메모
     * <p>
     * 개인정보가 아닌 맥락만 적습니다 (예: {@code "관리자 화면"}).
     */
    String detail() default "";
}
