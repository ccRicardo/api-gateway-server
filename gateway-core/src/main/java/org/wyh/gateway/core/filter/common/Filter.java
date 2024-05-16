package org.wyh.gateway.core.filter.common;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 15:38
 * @Description: 过滤器（组件）的顶级接口。
                 其中，泛型T指的是过滤器处理的内容的类型（其实就是网关上下文类，但顶级接口的定义最好抽象一点）
 */
public interface Filter<T> {
    /**
     * @date: 2024-05-14 15:43
     * @description: 校验/检查是否应该执行该过滤器组件
     * @Param t: 
     * @return: boolean
     */
    boolean check(T t);
    /**
     * @date: 2024-05-14 15:49
     * @description: 执行过滤器，对指定内容进行过滤处理。（核心过滤逻辑其实是调用doFilter方法完成的）
     * @Param t:
     * @Param args:
     * @return: void
     */
    void filter(T t, Object... args);
    /**
     * @date: 2024-05-14 15:51
     * @description: 真正执行过滤处理逻辑的核心方法
     * @Param t:
     * @Param args:
     * @return: void
     */
    void doFilter(T t, Object... args);
    /**
     * @date: 2024-05-14 15:53
     * @description: 触发/激发下一个过滤器组件
     * @Param t: 
     * @Param args: 
     * @return: void
     */
    void fireNext(T t, Object... args);
    /**
     * @date: 2024-05-14 15:55
     * @description: 默认方法，用于过滤器初始化。父类方法只是占位，若子类有需求，可以重写覆盖。
     * @return: void
     */
    default void init(){}
    /**
     * @date: 2024-05-14 15:57
     * @description: 默认方法，用于过滤器销毁。父类方法只是占位，若子类有需求，可以重写覆盖。
     * @return: void
     */
    default void destroy(){}
}
