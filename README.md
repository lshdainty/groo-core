<p align="center">
  <img src="https://img.shields.io/badge/groo--core-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="그루 Core" />
</p>

<h1 align="center">그루 Core</h1>

<p align="center">
  <strong>그루 백엔드 공통 라이브러리</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-3.1.0-blue" alt="Version" />
  <img src="https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/GitHub_Packages-181717?logo=github&logoColor=white" alt="GitHub Packages" />
</p>

---

## 소개

**groo-core**(전 이름 `groo-core`)는 [그루](https://github.com/lshdainty/groo) 백엔드 프로젝트에서 공통으로 사용되는 라이브러리입니다.

예외 처리, 국제화(i18n), API 응답 포맷, JPA Auditing, 페이지네이션, 검증 어노테이션, 로깅 AOP 등 프로젝트 전반에서 사용되는 컴포넌트를 제공합니다.

현재 **groo-damda-back**, **groo-ieum-back**, **groo-sso-back**에서 사용 중입니다. (porest-skc-back 미사용)

---

## 주요 기능

### Domain

| 클래스 | 설명 |
|--------|------|
| `AuditingFields` | JPA Auditing 기본 필드 (createAt, createBy, modifyAt, modifyBy) |

### Configuration

| 클래스 | 설명 |
|--------|------|
| `LocaleConfig` | Locale 설정 (`?lang=` 파라미터 > Accept-Language 헤더 > 기본 ko, ko/en 지원) |
| `SecurityProperties` | 보안 설정 프로퍼티 (IP 블랙리스트 등) |
| `PasswordEncoderConfig` | BCrypt 비밀번호 인코더 |

### Constant

| 클래스 | 설명 |
|--------|------|
| `CoreConstants` | 공통 상수 (날짜 포맷, 페이지네이션 기본값 등) |

### Controller

| 클래스 | 설명 |
|--------|------|
| `ApiResponse` | 통일된 API 응답 포맷 |
| `GlobalExceptionHandler` | 전역 예외 처리 |
| `PageRequest` / `PageResponse` | 오프셋 기반 페이지네이션 DTO |
| `CursorRequest` / `CursorResponse` | 커서 기반 페이지네이션 DTO |
| `SliceResponse` | Slice 기반 페이지네이션 응답 DTO (무한 스크롤용) |

### Exception

| 클래스 | HTTP Status | 설명 |
|--------|-------------|------|
| `ErrorCode` | - | Core 공통 에러 코드 enum |
| `ErrorCodeProvider` | - | 에러 코드 공통 인터페이스 (도메인별 ErrorCode가 구현) |
| `BusinessException` | - | 비즈니스 예외 기본 클래스 |
| `EntityNotFoundException` | 404 | 엔티티 조회 실패 |
| `ResourceNotFoundException` | 404 | 리소스 조회 실패 |
| `InvalidValueException` | 400 | 입력값 검증 실패 |
| `BusinessRuleViolationException` | 400 | 비즈니스 규칙 위반 |
| `DuplicateException` | 409 | 중복 데이터 |
| `UnauthorizedException` | 401 | 인증 실패 |
| `ForbiddenException` | 403 | 권한 없음 |
| `ExternalServiceException` | 5xx | 외부 서비스 연동 실패 |

### Type (Interface & Enum)

| 타입 | 설명 |
|------|------|
| `DisplayType` | 화면 표시 타입 인터페이스 |
| `CountryCode` | 국가 코드 |
| `Environment` | 실행 환경 타입 |
| `SortDirection` | 정렬 방향 타입 |
| `YNType` | Y/N 타입 |

### Utility

| 클래스 | 설명 |
|--------|------|
| `MessageResolver` | 다국어 메시지 조회 |
| `TimeUtils` | 날짜/시간 유틸리티 |
| `FileUtils` | 파일 처리 (저장, 읽기, 해시 계산 등) |
| `FileUploadValidator` | 파일 업로드 보안 검증 (확장자, 크기, MIME 타입) |
| `HttpUtils` | HTTP 요청 유틸리티 (IP, 헤더, 파라미터 등) |
| `CryptoUtils` | 암호화 유틸리티 |
| `JsonUtils` | JSON 변환 유틸리티 |
| `MaskUtils` | 마스킹 유틸리티 |
| `RegexPatterns` | 정규식 패턴 |

### Validation

| 어노테이션 | 설명 |
|------------|------|
| `@DateRange` | 날짜 범위 검증 (시작일 ≤ 종료일) |
| `@EnumValue` | Enum 값 검증 |
| `@Password` | 비밀번호 규칙 검증 (BASIC/STRONG) |
| `@Phone` | 전화번호 형식 검증 |

### Logging

| 클래스 | 설명 |
|--------|------|
| `@LogExecutionTime` | 메서드 실행 시간 로깅 어노테이션 |
| `@LogMethodCall` | 메서드 호출(진입/종료) 로깅 어노테이션 |
| `LoggingAspect` | 위 어노테이션을 처리하는 AOP Aspect |
| `SensitiveDataMasker` | 로그 문자열의 민감값 마스킹 (비밀번호·시크릿·토큰·인가코드·JWT) |
| `AbstractRequestResponseLoggingFilter` | 요청·응답 한 줄 로그 필터의 공통 뼈대(3.1.0). 서비스는 상속해 마스커·제외 경로만 정한다 |
| `UpstreamErrorLog` | 외부 API 오류를 로그에 옮길 때 응답 본문의 민감값·긴 숫자열을 가린다(3.1.0) |

> **`SensitiveDataMasker`** — 요청/응답 로깅 필터가 로그를 찍기 **직전**에 부르는 순수 문자열 함수다.
> 원본 요청·응답은 건드리지 않고 항상 새 문자열을 돌려주며, 어떤 입력에도 예외를 던지지 않는다.
>
> ```java
> // 기본 규칙만
> String safe = SensitiveDataMasker.mask(body);
>
> // 서비스 고유 키를 더할 때는 필터에서 static final 로 한 번 만들어 재사용한다
> private static final SensitiveDataMasker MASKER =
>         SensitiveDataMasker.withExtraKeys("clientId", "client_id");
> ```
>
> 키 비교는 정규화(소문자 + `_ - .` 제거) 후 완전일치라 `apiKey` · `api_key` · `API-KEY` 가 같은 키다.
> `code` 처럼 같은 이름이 상태코드와 OAuth 인가코드를 동시에 뜻하는 키는 **값이 43자 이상 base64url 일 때만**
> 가린다. `client_id` · `state` · `n` · `kid` · `codeChallenge` 는 공개 정보라 **일부러 안 가린다**
> (자세한 이유는 클래스 Javadoc 에 있다).

> **상속해서 쓰는 뼈대(3.1.0)** — 세 서비스가 복사해 쓰던 코드를 core 로 올리고, 서비스마다 다른 부분만 열어 뒀다.
> core 는 빈을 등록하지 않는다(서비스가 core 패키지를 통째로 스캔하므로 충돌한다). 서비스가 하위 클래스를 `@Component` 로 둔다.
>
> ```java
> @Component
> public class RequestResponseLoggingFilter extends AbstractRequestResponseLoggingFilter {
>     private static final SensitiveDataMasker MASKER = SensitiveDataMasker.withExtraKeys("clientId");
>     public RequestResponseLoggingFilter() { super(MASKER); }
>
>     @Override protected Collection<String> excludedPathPrefixes() {   // 앞부분 일치로 뺄 경로
>         return Stream.concat(DEFAULT_EXCLUDED_PATH_PREFIXES.stream(), Stream.of("/api/v1/notifications/stream")).toList();
>     }
>     @Override protected Collection<String> excludedExactPaths() {     // 스트리밍 다운로드(정확 일치)
>         return List.of("/api/v1/export");
>     }
> }
> ```

### Security

| 클래스 | 설명 |
|--------|------|
| `AuditorPrincipal` | JPA Auditing용 Principal 인터페이스 |
| `AbstractAuditorAware` | JPA 작성자·수정자 결정의 공통 뼈대(3.1.0). 서비스는 상속해 익명·낯선 주체일 때 값만 정한다 |

### Message

| 클래스 | 설명 |
|--------|------|
| `MessageKey` | Core 공통 다국어 메시지 키 enum |

> **다국어 정책**: core에는 공통 메시지 키(`MessageKey`)와 core 공통 메시지(`core-messages*.properties`)만 포함합니다.
> 도메인별 메시지 키와 메시지 본문은 각 프로젝트의 MessageKey enum과 message properties에서 관리합니다.

---

## 프로젝트 구조

```
src/main/
├── java/io/groo/core/
│   ├── config/
│   │   ├── web/                 # LocaleConfig
│   │   ├── properties/          # SecurityProperties
│   │   └── PasswordEncoderConfig.java
│   ├── constant/
│   │   └── CoreConstants.java
│   ├── controller/
│   │   ├── dto/                 # PageRequest/Response, CursorRequest/Response, SliceResponse
│   │   ├── ApiResponse.java
│   │   └── GlobalExceptionHandler.java
│   ├── domain/
│   │   └── AuditingFields.java  # JPA Auditing 기본 필드
│   ├── exception/
│   │   ├── ErrorCode.java
│   │   ├── ErrorCodeProvider.java
│   │   ├── BusinessException.java
│   │   └── ...
│   ├── logging/                 # LogExecutionTime, LogMethodCall, LoggingAspect, SensitiveDataMasker
│   ├── message/
│   │   └── MessageKey.java
│   ├── security/
│   │   └── AuditorPrincipal.java
│   ├── type/
│   │   ├── DisplayType.java
│   │   ├── CountryCode.java
│   │   ├── Environment.java
│   │   ├── SortDirection.java
│   │   └── YNType.java
│   ├── util/
│   │   ├── MessageResolver.java
│   │   ├── TimeUtils.java
│   │   ├── FileUtils.java
│   │   ├── FileUploadValidator.java
│   │   ├── HttpUtils.java
│   │   ├── CryptoUtils.java
│   │   ├── JsonUtils.java
│   │   ├── MaskUtils.java
│   │   └── RegexPatterns.java
│   └── validation/
│       ├── annotation/          # DateRange, EnumValue, Password, Phone
│       └── validator/           # 각 어노테이션의 Validator
└── resources/
    └── message/                 # core-messages.properties (+_ko, _en)
```

---

## 사용 예시

### AuditingFields

```java
// 기본 사용
@Entity
@Table(name = "users")
public class User extends AuditingFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
}

// IP 필드 확장 (각 서비스에서)
@MappedSuperclass
public abstract class AuditingFieldsWithIp extends AuditingFields {
    @Column(name = "create_ip", length = 45)
    private String createIp;

    @Column(name = "modify_ip", length = 45)
    private String modifyIp;

    @PrePersist
    public void prePersist() {
        this.createIp = HttpUtils.getClientIp();
        this.modifyIp = this.createIp;
    }

    @PreUpdate
    public void preUpdate() {
        this.modifyIp = HttpUtils.getClientIp();
    }
}
```

### Exception

```java
// 엔티티 조회 실패
User user = userRepository.findById(id)
    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NOT_FOUND));

// 비즈니스 규칙 위반 (도메인별 ErrorCode는 ErrorCodeProvider 구현)
if (vacation.getBalance() < requestedDays) {
    throw new BusinessRuleViolationException(HrErrorCode.VACATION_INSUFFICIENT);
}
```

### Utility

```java
// 파일 해시 계산
String hash = FileUtils.calculateSha256("/files/document.pdf");

// 파일 업로드 검증
FileUploadValidator.validate(file, allowedExtensions, maxSizeBytes);

// HTTP 요청 정보
String clientIp = HttpUtils.getClientIp();
Map<String, String> params = HttpUtils.getParameterMapSingleValue();

// 다국어 메시지
String message = messageResolver.getMessage(MessageKey.COMMON_SUCCESS);
```

---

## 설치

### Gradle (build.gradle)

```gradle
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/lshdainty/groo-core")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user")
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key")
        }
    }
}

dependencies {
    implementation 'io.groo:groo-core:3.1.0'
}
```

---

## 배포

GitHub Packages로 배포합니다. CI 배포는 없으며 **수동으로 `gradle publish`를 실행**합니다.

```bash
# ~/.gradle/gradle.properties 에 인증 정보 필요
# gpr.user=<GitHub 사용자명>
# gpr.key=<write:packages 권한 토큰>

./gradlew publish
```

버전업 절차:

1. `build.gradle`의 `version` 수정
2. `./gradlew publish` 실행
3. 사용 프로젝트(desk-back, hr-back, sso-back)의 `build.gradle` 의존성 버전 갱신

---

## 관련 저장소

| Repository | Description |
|------------|-------------|
| [groo](https://github.com/lshdainty/groo) | 통합 레포지토리 (서비스 소개) |
| [groo-damda-back](https://github.com/lshdainty/groo-damda-back) | Desk 백엔드 |
| [groo-ieum-back](https://github.com/lshdainty/groo-ieum-back) | HR 백엔드 |
| [groo-ieum-front](https://github.com/lshdainty/groo-ieum-front) | HR 프론트엔드 |
| [groo-sso-back](https://github.com/lshdainty/groo-sso-back) | SSO 백엔드 |
| [groo-sso-front](https://github.com/lshdainty/groo-sso-front) | SSO 프론트엔드 |

---

<p align="center">
  Made with by <a href="https://github.com/lshdainty">lshdainty</a>
</p>
