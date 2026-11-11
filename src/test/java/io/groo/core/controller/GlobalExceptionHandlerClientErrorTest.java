package io.groo.core.controller;

import io.groo.core.exception.ErrorCode;
import io.groo.core.util.MessageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.ByteArrayInputStream;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * 클라이언트 실수인데 500 으로 새던 Spring MVC 예외가 4xx 로 바뀌었는지 검증한다(3.0.2).
 *
 * <p>메시지는 실제 {@code core-messages} 번들로 푼다 — 키를 번들에 빠뜨리면 MessageResolver 가
 * 예외 없이 키 문자열을 그대로 돌려주므로, "키가 그대로 나오지 않는다"까지 확인한다.
 */
class GlobalExceptionHandlerClientErrorTest {

    private GlobalExceptionHandler sut;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("message/core-messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        sut = new GlobalExceptionHandler(new MessageResolver(source));
        LocaleContextHolder.setLocale(Locale.KOREAN);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void 지원하지_않는_요청_방식은_405와_Allow_헤더() {
        ResponseEntity<ApiResponse<Void>> res = sut.handleHttpRequestMethodNotSupportedException(
                new HttpRequestMethodNotSupportedException("GET", List.of("POST")));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(res.getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("POST");
        assertThat(res.getBody().getCode()).isEqualTo("COMMON_405");
        assertThat(res.getBody().getMessage()).isEqualTo("이 주소에서 지원하지 않는 요청 방식입니다.");
    }

    @Test
    void 지원_메서드를_모르면_Allow_헤더를_붙이지_않는다() {
        ResponseEntity<ApiResponse<Void>> res = sut.handleHttpRequestMethodNotSupportedException(
                new HttpRequestMethodNotSupportedException("GET"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(res.getHeaders().getFirst(HttpHeaders.ALLOW)).isNull();
    }

    @Test
    void Allow_값은_정렬된_목록() {
        assertThat(GlobalExceptionHandler.allowHeader(Set.of(HttpMethod.PUT, HttpMethod.GET))).isEqualTo("GET, PUT");
        assertThat(GlobalExceptionHandler.allowHeader(Set.of())).isNull();
        assertThat(GlobalExceptionHandler.allowHeader(null)).isNull();
    }

    @Test
    void 읽을_수_없는_본문은_400() {
        ResponseEntity<ApiResponse<Void>> res = sut.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("JSON parse error", emptyInput()));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getCode()).isEqualTo("COMMON_400");
        assertThat(res.getBody().getMessage()).isEqualTo("요청 형식이 올바르지 않습니다.");
    }

    @Test
    void 지원하지_않는_Content_Type은_415() {
        ResponseEntity<ApiResponse<Void>> res = sut.handleHttpMediaTypeNotSupportedException(
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(res.getBody().getCode()).isEqualTo("COMMON_415");
        assertThat(res.getBody().getMessage()).isNotEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessageKey());
    }

    @Test
    void 끊긴_비동기_연결은_아무것도_던지지_않고_끝난다() {
        assertThatNoException().isThrownBy(() -> sut.handleAsyncRequestNotUsableException(
                new AsyncRequestNotUsableException("disconnected client", new IOException("Broken pipe")), null));
    }

    @Test
    void 새_메시지는_영어_번들에서도_풀린다() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ResponseEntity<ApiResponse<Void>> res = sut.handleHttpRequestMethodNotSupportedException(
                new HttpRequestMethodNotSupportedException("GET", List.of("POST")));

        assertThat(res.getBody().getMessage()).isEqualTo("This request method is not supported for this endpoint.");
    }


    @Test
    void 파라미터_타입_오류는_이름을_응답에_싣지_않는다() {
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "budgetId", (MethodParameter) null, new NumberFormatException());

        ResponseEntity<ApiResponse<Void>> res = sut.handleMethodArgumentTypeMismatchException(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getCode()).isEqualTo("COMMON_400");
        assertThat(res.getBody().getMessage()).isEqualTo("요청 값이 올바르지 않습니다.").doesNotContain("budgetId");
    }

    @Test
    void 필수_파라미터_누락도_이름을_싣지_않고_번들_문구로_답한다() {
        ResponseEntity<ApiResponse<Void>> res = sut.handleMissingServletRequestParameterException(
                new MissingServletRequestParameterException("date", "LocalDate"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getMessage()).isEqualTo("요청에 필요한 값이 빠졌습니다.").doesNotContain("date");
    }

    @Test
    void 본문_날짜_파싱_실패는_날짜_문구로() {
        DateTimeParseException cause = new DateTimeParseException("bad", "2026-13-01", 5);
        ResponseEntity<ApiResponse<Void>> res = sut.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("JSON parse error", cause, emptyInput()));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getMessage()).isEqualTo("올바른 날짜가 아닙니다.");
    }

    @Test
    void 컨트롤러_날짜_파싱_실패는_400() {
        ResponseEntity<ApiResponse<Void>> res = sut.handleDateTimeParseException(
                new DateTimeParseException("bad", "x", 0));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getMessage()).isEqualTo("올바른 날짜가 아닙니다.");
    }

    @Test
    void 매핑_없는_경로는_404() throws Exception {
        ResponseEntity<ApiResponse<Void>> res = sut.handleNoHandlerFoundException(
                new NoHandlerFoundException("POST", "/api/v1/none", new HttpHeaders()));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().getCode()).isEqualTo("COMMON_404");
    }

    @Test
    void 동시_수정_충돌은_409() {
        ResponseEntity<ApiResponse<Void>> res = sut.handleObjectOptimisticLockingFailureException(
                new ObjectOptimisticLockingFailureException(LocalDate.class, 1L));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().getCode()).isEqualTo("COMMON_409");
    }

    @Test
    void DB_제약_위반은_벤더_코드로_종류를_가른다() {
        assertThat(integrity(1062).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);          // 중복
        assertThat(integrity(1048).getBody().getMessage()).isEqualTo("필수 값이 빠졌습니다."); // NOT NULL
        assertThat(integrity(1452).getBody().getMessage()).isEqualTo("참조하는 대상이 존재하지 않습니다."); // FK
        assertThat(integrity(9999).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);       // 그 밖
    }

    private ResponseEntity<ApiResponse<Void>> integrity(int mariaErrorCode) {
        return sut.handleDataIntegrityViolationException(new DataIntegrityViolationException("x",
                new SQLIntegrityConstraintViolationException("constraint", "23000", mariaErrorCode)));
    }

    private static HttpInputMessage emptyInput() {
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
    }
}
