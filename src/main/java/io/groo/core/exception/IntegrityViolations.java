package io.groo.core.exception;

import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;

/**
 * 무결성 제약 위반이 <b>어떤 제약</b>이었는지 한 곳에서 판정한다.
 *
 * <p>판정하는 자리가 열 곳이 넘는다 — 공통 핸들러({@code GlobalExceptionHandler}) 하나와,
 * 자기 도메인의 "이름 중복" 으로 번역하는 서비스 열 곳이다. 각자 문자열을 뒤지게 두면 규칙이
 * 갈라지고, 갈라진 것은 조용히 갈라진다 — 컴파일도 테스트도 통과한다.
 *
 * <h4>문자열 매칭을 쓰지 않는 이유, 그리고 무엇으로 판정하는가</h4>
 * 원인 메시지({@code "Column 'event_type' cannot be null"})는 <b>DB·드라이버·로케일마다 다르고</b>
 * 버전이 오르면 말없이 바뀐다. 대신 <b>하이버네이트가 이미 매긴 종류</b>를 읽는다 —
 * {@link ConstraintViolationException#getKind()} 는 방언(dialect)이 벤더 에러코드를 보고 채운
 * 값이다. 실측(2026-09-07, 이 레포 H2 슬라이스)으로 확인한 값:
 * <pre>
 *   NOT NULL  → kind=NOT_NULL     SQLState 23502   (event_label.color 를 null 로 저장)
 *   UNIQUE    → kind=UNIQUE       SQLState 23505   (UK_todo_tag 중복)
 *   FK        → kind=FOREIGN_KEY  SQLState 23506   (event_comment.event_row_id 가 없는 행)
 * </pre>
 * 그리고 스프링이 {@code DataIntegrityViolationException} 으로 감싸도 <b>원인 사슬에
 * 하이버네이트 예외가 그대로 남는다</b>는 것도 같은 실측에서 확인했다.
 *
 * <h4>H2(테스트)와 MariaDB(운영)가 다르다 — 그래서 둘 다 본다</h4>
 * 벤더 코드는 서로 완전히 다르다. H2 는 SQLState 가 곧 에러코드(23502·23505·23506)이고,
 * MariaDB 는 SQLState 가 전부 {@code 23000} 이라 <b>벤더 에러코드</b>(1048·1062·1452…)로만
 * 갈린다. 다행히 <b>양쪽 방언이 같은 {@code ConstraintKind} 로 정규화</b>한다 —
 * {@code H2Dialect}(23502→NOT_NULL, 23505→UNIQUE, 23503·23506→FOREIGN_KEY) 와
 * {@code MySQLDialect}({@code MariaDBDialect} 의 부모: 1048·1364→NOT_NULL, 1062→UNIQUE,
 * 1451·1452→FOREIGN_KEY). 그래서 <b>1차 판정은 두 환경에서 같은 코드로 돈다</b>.
 * (하이버네이트 7.2 바이트코드에서 직접 확인.)
 *
 * <p>2차로 SQLState·벤더 에러코드를 직접 본다. 하이버네이트를 안 거친 순수 JDBC 경로나
 * 방언이 종류를 못 매긴 경우({@code OTHER})를 위한 그물이다 — 여기서만 벤더가 갈리므로
 * 두 벌을 나란히 둔다.
 *
 * <p>3차는 {@link PropertyValueException} 이다. 하이버네이트가 DB 에 보내기 <b>전에</b>
 * 널 검사를 하도록 켜면({@code hibernate.check_nullability}) SQL 조차 나가지 않아
 * SQLException 이 없다. 지금은 꺼져 있지만(부트가 빈 검증기를 붙이면 기본으로 끈다)
 * 그 설정이 바뀌는 날 판정이 통째로 {@code OTHER} 로 무너지지 않게 받아 둔다.
 */
public final class IntegrityViolations {

    private IntegrityViolations() {
    }

    /** 원인 사슬을 몇 단계까지 볼지 — 자기참조·순환 방어. */
    private static final int MAX_DEPTH = 20;

    // ── MariaDB / MySQL 벤더 에러코드 (SQLState 는 전부 23000 이라 이 코드로만 갈린다) ──
    private static final int MARIA_DUP_ENTRY = 1062;            // ER_DUP_ENTRY
    private static final int MARIA_DUP_ENTRY_WITH_KEY = 1586;   // ER_DUP_ENTRY_WITH_KEY_NAME
    private static final int MARIA_DUP_UNIQUE = 1169;           // ER_DUP_UNIQUE
    private static final int MARIA_BAD_NULL = 1048;             // ER_BAD_NULL_ERROR
    private static final int MARIA_NO_DEFAULT_FOR_FIELD = 1364; // ER_NO_DEFAULT_FOR_FIELD
    private static final int MARIA_ROW_IS_REFERENCED = 1451;    // ER_ROW_IS_REFERENCED_2
    private static final int MARIA_NO_REFERENCED_ROW = 1452;    // ER_NO_REFERENCED_ROW_2

    // ── H2 (SQLState == 에러코드) ──
    private static final String H2_NULL_NOT_ALLOWED = "23502";
    private static final String H2_DUPLICATE_KEY = "23505";
    private static final String H2_REFERENTIAL_CHILD = "23503";
    private static final String H2_REFERENTIAL_PARENT = "23506";

    /** 벤더가 SQLState 를 안 쪼개는 계열(MySQL·MariaDB)의 무결성 위반 공통 SQLState. */
    private static final String SQLSTATE_INTEGRITY_GENERIC = "23000";

    /**
     * 이 위반이 어떤 제약이었나. 판정이 안 되면 {@link IntegrityViolationKind#OTHER}.
     *
     * @param t 잡은 예외(스프링 래퍼여도 되고 하이버네이트·JDBC 원본이어도 된다)
     */
    public static IntegrityViolationKind classify(Throwable t) {
        if (t instanceof DuplicateKeyException) {
            // 스프링이 이미 "중복 키" 라고 못 박은 경우 — 더 볼 것이 없다.
            return IntegrityViolationKind.UNIQUE;
        }
        IntegrityViolationKind fromVendor = null;
        int depth = 0;
        for (Throwable c = t; c != null && depth++ < MAX_DEPTH; c = c.getCause()) {
            if (c instanceof ConstraintViolationException hibernate) {
                IntegrityViolationKind kind = fromHibernateKind(hibernate);
                // OTHER 면 방언이 종류를 못 매긴 것이다 — 벤더 코드로 한 번 더 본다.
                if (kind != IntegrityViolationKind.OTHER) return kind;
            }
            if (c instanceof PropertyValueException) {
                return IntegrityViolationKind.NOT_NULL;
            }
            if (fromVendor == null && c instanceof SQLException sql) {
                fromVendor = fromVendorCodes(sql);
            }
            if (c == c.getCause()) break;
        }
        return fromVendor != null ? fromVendor : IntegrityViolationKind.OTHER;
    }

    /** UNIQUE 위반인가 — 도메인 서비스가 "이름 중복" 으로 번역해도 되는 경우인지 묻는 자리. */
    public static boolean isUnique(Throwable t) {
        return classify(t) == IntegrityViolationKind.UNIQUE;
    }

    /**
     * 위반한 제약의 이름. 못 읽으면 {@code null}.
     *
     * <p><b>사용자에게 보이는 자리에 쓰지 마라</b> — 제약 이름은 컬럼 이름 그 자체일 때가 있다
     * (H2 실측: NOT NULL 위반의 제약 이름이 {@code "COLOR"}). QA #75 가 금지한 것이 그 노출이다.
     *
     * <p>쓰는 곳은 <b>한 테이블에 UNIQUE 가 둘 이상</b>이라 어느 쪽이 걸렸는지에 따라 답이
     * 달라지는 자리다 — {@code dutch_pay_participant} 는 활성 이름과 활성 결제자 둘을 건다.
     *
     * <p><b>이름 전체를 같다고 비교하지 마라.</b> 돌아오는 값은 우리가 지은 키 이름 <b>그대로가
     * 아니다</b>. 실측(2026-09-07, 이 레포 H2 슬라이스)에서 {@code UK_todo_tag} 위반이 낸 값은
     * <pre>PUBLIC.UK_TODO_TAG INDEX PUBLIC.UK_TODO_TAG_INDEX_1</pre>
     * 이었다 — 스키마가 앞에 붙고, 대문자로 접히고, 인덱스 이름이 뒤에 따라온다. MariaDB 는
     * 드라이버 메시지({@code "Duplicate entry ... for key '...'"})에서 뽑으므로 또 다른 모양이다.
     * 그러니 우리가 정한 <b>키 이름의 특징적인 조각</b>을 {@code toLowerCase} 후 포함 검사해라.
     * groo-damda-back 의 {@code IntegrityViolationKindOnH2Test} 가 이 모양을 붙들고 있다.
     *
     * <p>드라이버 <b>메시지</b>를 뒤지는 것과는 다르다 — 제약 이름은 우리가 짓고, 바뀌면
     * 마이그레이션에서 바뀐다. 메시지는 벤더가 말없이 바꾼다.
     */
    public static String constraintName(Throwable t) {
        int depth = 0;
        for (Throwable c = t; c != null && depth++ < MAX_DEPTH; c = c.getCause()) {
            if (c instanceof ConstraintViolationException hibernate) {
                String name = hibernate.getConstraintName();
                if (name != null) return name;
            }
            if (c == c.getCause()) break;
        }
        return null;
    }

    /**
     * <b>로그 전용</b> 한 줄 요약. 제약 이름·SQLState·벤더 코드가 들어간다.
     *
     * <p>절대 응답에 싣지 마라 — 이유는 {@link #constraintName(Throwable)} 과 같다.
     */
    public static String describe(Throwable t) {
        String sqlState = null;
        Integer errorCode = null;
        int depth = 0;
        for (Throwable c = t; c != null && depth++ < MAX_DEPTH; c = c.getCause()) {
            if (sqlState == null && c instanceof SQLException sql) {
                sqlState = sql.getSQLState();
                errorCode = sql.getErrorCode();
            }
            if (c == c.getCause()) break;
        }
        return "kind=" + classify(t)
                + ", constraint=" + constraintName(t)
                + ", sqlState=" + sqlState
                + ", errorCode=" + errorCode;
    }

    private static IntegrityViolationKind fromHibernateKind(ConstraintViolationException e) {
        return switch (e.getKind()) {
            case UNIQUE -> IntegrityViolationKind.UNIQUE;
            case NOT_NULL -> IntegrityViolationKind.NOT_NULL;
            case FOREIGN_KEY -> IntegrityViolationKind.FOREIGN_KEY;
            case CHECK, OTHER -> IntegrityViolationKind.OTHER;
        };
    }

    private static IntegrityViolationKind fromVendorCodes(SQLException e) {
        String state = e.getSQLState();
        if (state != null) {
            switch (state) {
                case H2_NULL_NOT_ALLOWED -> { return IntegrityViolationKind.NOT_NULL; }
                case H2_DUPLICATE_KEY -> { return IntegrityViolationKind.UNIQUE; }
                case H2_REFERENTIAL_CHILD, H2_REFERENTIAL_PARENT -> { return IntegrityViolationKind.FOREIGN_KEY; }
                default -> { /* 아래 벤더 코드로 넘어간다 */ }
            }
            if (!SQLSTATE_INTEGRITY_GENERIC.equals(state) && !state.startsWith("23")) {
                return IntegrityViolationKind.OTHER;
            }
        }
        return switch (e.getErrorCode()) {
            case MARIA_DUP_ENTRY, MARIA_DUP_ENTRY_WITH_KEY, MARIA_DUP_UNIQUE -> IntegrityViolationKind.UNIQUE;
            case MARIA_BAD_NULL, MARIA_NO_DEFAULT_FOR_FIELD -> IntegrityViolationKind.NOT_NULL;
            case MARIA_ROW_IS_REFERENCED, MARIA_NO_REFERENCED_ROW -> IntegrityViolationKind.FOREIGN_KEY;
            default -> IntegrityViolationKind.OTHER;
        };
    }
}
