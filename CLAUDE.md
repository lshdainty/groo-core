# porest-core — 작업 규칙

> **워크스페이스 공통 규칙**(Git 작업 격리 · 스테이징 범위 · 태그·릴리스)은
> 상위 `/home/lshdainty/study/CLAUDE.md` 에 있다. Claude Code 가 디렉토리 워크업으로
> 자동 로드하므로 여기에 복사하지 않는다 — 복사본은 원문이 바뀌어도 따라오지 않는다.

## 이 레포는

`porest-desk-back` · `porest-hr-back` · `porest-sso-back` 이 공유하는 Spring Boot 공통 라이브러리
(예외 체계 · ApiResponse · 다국어 메시지 · 페이지네이션 · JPA Auditing · 로깅/접속기록 AOP · 시간 유틸).
실행되는 앱이 아니라 GitHub Packages 로 배포하는 `java-library` 모듈이다 — 여기서 잘못 건드리면
서비스 3개의 **기동**이 깨진다. Java 25 toolchain / Gradle 9.2.1 / Spring Boot 4.0.2 / Jackson 3.

## 검증

`java` 는 PATH 에 이미 있고(temurin-17) 실제 컴파일은 temurin-25 toolchain 으로 fork 되므로 env 준비는 없다.

```bash
./gradlew compileJava   # 사실상 유일한 검증. src/test 가 없어 test 태스크는 no-op 이다
```

**"테스트를 돌려 확인했다"는 이 레포에서 성립하지 않는다.** 자바 파일 51개가 전부 `src/main` 아래다.
`./gradlew build` 도 javadoc 이 `Xdoclint:none` 이라 컴파일 이상은 못 잡는다.

메시지 키를 건드렸으면 3파일 정합성을 따로 본다 (출력이 비어야 정상):

```bash
for f in src/main/resources/message/core-messages*.properties; do \
  grep -oE '^[a-z0-9.]+=' "$f" | tr -d '=' | sort -u; done | sort | uniq -c | grep -v '^ *3 '
```

## 이 레포에서만 통하는 것

- **core 빈에 필수 생성자 의존을 새로 추가하지 마라.** core 는 auto-configuration 이 아니고
  (`META-INF/spring/*.imports` 없음) 서비스 3개가 `scanBasePackages = {"...", "io.groo.core"}` 로
  통째로 스캔한다. 그 의존을 구현하지 않은 서비스는 컴파일이 아니라 **기동**에서 죽는다.
  선택적 협력자는 `ObjectProvider<T>` 로 받고 없으면 조용히 비활성화하라
  (`audit/AuditAccessAspect.java` 가 본보기 — 1회 warn 후 return).
  같은 이유로 core 는 `MessageSource` 빈도 `WebMvcConfigurer` 도 제공하지 않는다. 새 리소스·인터셉터를
  넣으면 basename 등록·`addInterceptors` 배선은 소비 레포 3곳의 몫으로 넘어간다.

- **새 의존성은 `compileOnly`.** Spring·Jackson·JPA 는 전부 소비 서비스가 제공하는 전제다.
  `implementation` 으로 넣으면 core 는 컴파일되지만 서비스 런타임에서 `NoClassDefFoundError` 로 터진다.

- **Jackson 3 이다.** databind/core 는 `tools.jackson.*`, **애노테이션만** `com.fasterxml.jackson.annotation.*`.
  두 패키지가 한 레포에 섞여 있으니 옆 파일 흉내내다 틀린다. `com.fasterxml.jackson.databind.ObjectMapper` 는 없다.

- **`MessageKey`/`ErrorCode` 에 항목을 추가하면 `core-messages{,_ko,_en}.properties` 세 파일 모두에 키를 넣어라.**
  `MessageResolver` 가 defaultMessage 로 키 자체를 넘기므로 빠뜨려도 예외도 로그도 없고 API 응답 message 에
  키 문자열이 그대로 실려 나간다. (지금 `error.common.missing.parameter` 하나가 실제로 새고 있다.)

- **`TimeUtils.today()/now()/yesterday()/tomorrow()` 를 새 코드에 쓰지 마라.** JVM 기본 타임존을 타서
  컨테이너(UTC)에서 사용자 기준과 어긋난다. deprecated 표시뿐이라 컴파일은 통과하고, 증상은
  "가끔 하루 어긋남"이라 늦게 발견된다. `todayIn/nowIn(String, ZoneId)` 또는 `core.time` 의
  `ServiceClock`/`UserClock` 을 써라.

- **`.gitignore` 의 `!gradle/wrapper/gradle-wrapper.jar` 는 `*.jar` 뒤에 있어야 한다.** git 은 마지막에
  매치된 규칙을 따른다. 정리한다고 위로 올리면 wrapper jar 이 커밋에서 빠지고 `./gradlew` 가 죽는다
  (이 레포에 실제로 있었던 일이다). `gradlew` · `gradle-wrapper.jar` · `gradle-wrapper.properties` 는
  9.2.1 한 세트다 — 셋 중 하나만 손대지 말고, 갱신은 다른 레포에서 `(desk-back)/gradlew -p <core> wrapper` 로 한다.

## 릴리스 체인

버전을 올릴 땐 네 곳이 함께 움직인다. 하나만 하면 아무 서비스도 새 코드를 안 쓴다.

1. `build.gradle:7` `version` — 기능 커밋과 **같은 커밋**에서 올리고 제목에 `(2.3.0 → 2.3.1)` 을 붙이는 게 관례다
2. `README.md` 의 버전 배지와 설치 예시 — 지금 2.0.3 에 멈춰 있어 실제 2.3.1 과 어긋난다
3. `./gradlew publish` — CI 배포가 없어 수동이며 `~/.gradle/gradle.properties` 의 `gpr.user`/`gpr.key` 가 필요하다
   (현재 머신엔 없어 그냥 돌리면 401)
4. 소비 레포 3곳의 `build.gradle` 의존 버전

태그 `v<version>` 를 밀면 `.github/workflows/release.yml` 이 GitHub Release 를 만든다 —
그래서 태그는 상위 CLAUDE.md 규칙대로 사용자 몫이다. core 의 공개 API 시그니처를 바꿨다면
소비 레포 3곳이 동시에 컴파일 실패하므로 같이 고쳐질 작업으로 잡아라.

## 패키지 · 좌표 (2026-10-20 변경, 3.0.0)

- 패키지 `com.porest.core` → **`io.groo.core`**. 서비스 쪽 패키지는 `io.groo.ieum`(HR) · `io.groo.damda`(Desk) · `io.groo.sso` 다.
- Maven 좌표 `com.porest:porest-core` → **`io.groo:groo-core`**. 패키지가 바뀌었으니 소비 서비스 3곳은 import 와
  `scanBasePackages` 를 **한꺼번에** 따라가야 한다 — 하나라도 옛 패키지로 남으면 컴파일이나 기동에서 죽는다.
- 레포 이름은 아직 `porest-core` 다. GitHub Packages 주소(`maven.pkg.github.com/lshdainty/porest-core`)가 레포 이름을 따르므로,
  레포를 바꿀 때는 배포 주소와 소비 서비스의 저장소 설정을 같이 본다.
- 패키지 이름이 직렬화 문자열(Jackson `@class`, Redis 캐시 값 등)로 남는 서비스는 배포 때 옛 캐시를 비워야 한다 — 서비스별 확인은 각 레포에서 한다.
