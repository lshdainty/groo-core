package io.groo.core.exception;

/**
 * DB 무결성 제약 위반의 <b>종류</b>. 종류가 갈리면 답도 갈린다.
 *
 * <p>QA #81 이 드러낸 것이 정확히 이 구분의 부재다 — {@code DataIntegrityViolationException} 하나를
 * 통째로 {@code COMMON_409 "다른 곳에서 먼저 수정됐어요"} 로 포장하면, 값을 빼먹고 보낸 요청까지
 * "누가 먼저 저장했다" 는 거짓말을 듣는다. 다시 보내도 될 것 같아 재시도하지만 영원히 같은 답이 온다.
 *
 * @see IntegrityViolations 판정 방법과 그 근거
 */
public enum IntegrityViolationKind {
    /** 유일성 위반. 이것만 409 다 — 조회와 저장 사이에서 남에게 진 것이 맞다. */
    UNIQUE,
    /** NOT NULL 위반. 요청이 값을 안 보낸 것이므로 400. */
    NOT_NULL,
    /** 외래키 위반. 없는 것을 가리켰거나 참조 중인 것을 지운 것이므로 400. */
    FOREIGN_KEY,
    /** CHECK 위반·길이 초과·판정 불가. 위반이 난 이상 요청 데이터 문제로 보고 400. */
    OTHER
}
