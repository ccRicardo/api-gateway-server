package org.wyh.gateway.core.filter.common.base;

import java.lang.annotation.*;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 15:59
 * @Description: 过滤器注解类，用于记录过滤器的一些基本信息。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)     //定义注解的保留策略（此处为运行时仍然保留注解信息）
@Target({ElementType.TYPE})             //定义注解的作用范围（此处为类/接口/枚举/注解）
public @interface FilterAspect {
    //过滤器id
    String id();
    //过滤器名称
    String name() default "";
    //过滤器类型
    FilterType type();
    //过滤器执行优先级。值越小，执行顺序越靠前。
    int order() default 0;
}
