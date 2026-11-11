package io.groo.core.security.web;

import io.groo.core.exception.ErrorCode;
import io.groo.core.exception.UnauthorizedException;
import io.groo.core.security.LoginUser;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link LoginUser} 인자를 채우는 공통 뼈대(core 3.2.0). 서비스는 어떤 타입을 줄지({@code type})와
 * 인증 정보에서 그 객체를 만드는 법({@link #toLoginUser})만 구현한다.
 *
 * <p>인증이 없거나 모르는 주체면 기본은 401 을 던진다 — 보호된 API 에서 {@code null} 사용자가 서비스로 흘러가면
 * NPE 나 "남의 데이터 조회" 로 이어진다. 공개 API 에서 비로그인을 허용해야 하면 {@link #onMissing()} 을 덮는다.
 *
 * @param <T> 컨트롤러가 받는 로그인 사용자 타입
 */
public abstract class AbstractLoginUserArgumentResolver<T> implements HandlerMethodArgumentResolver {

    private final Class<T> type;

    protected AbstractLoginUserArgumentResolver(Class<T> type) {
        this.type = type;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class) && type.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return onMissing();
        }
        T user = toLoginUser(authentication);
        return user != null ? user : onMissing();
    }

    /** 인증 정보에서 로그인 사용자를 만든다. 주체 타입이 맞지 않으면 null. */
    protected abstract T toLoginUser(Authentication authentication);

    /** 로그인 사용자가 없을 때. 기본은 401. */
    protected T onMissing() {
        throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
}
