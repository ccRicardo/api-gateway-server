package org.wyh.gateway.core.filter.old_common;

import java.lang.annotation.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter
 * @Author: wyh
 * @Date: 2024-02-19 14:09
 * @Description: 过滤器注解，用于记录过滤器的一些基本信息。。
 */

@Retention(RetentionPolicy.RUNTIME)     //定义注解的保留策略（此处为运行时仍然保留注解信息）
@Target({ElementType.TYPE})             //定义注解的作用范围（此处为类/接口/枚举/注解）
@Documented
public @interface FilterAspect {
    //过滤器id
    String id();
    //过滤器名称
    String name() default "";
    //该过滤器的执行优先级
    int order() default 0;
}
