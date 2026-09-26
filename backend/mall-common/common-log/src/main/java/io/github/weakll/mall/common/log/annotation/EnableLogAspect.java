package io.github.weakll.mall.common.log.annotation;

import io.github.weakll.mall.common.log.aspect.LogAspect;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(value = LogAspect.class) // 通过Import注解导⼊⽇志切⾯类到Spring容器中
public @interface EnableLogAspect {
}
