package io.groo.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 인자에 로그인 사용자를 넣는다(core 3.2.0). 어떤 타입으로 넣을지는 서비스의
 * {@link io.groo.core.security.web.AbstractLoginUserArgumentResolver} 하위 클래스가 정한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
