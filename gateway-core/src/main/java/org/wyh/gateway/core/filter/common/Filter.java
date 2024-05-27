package org.wyh.gateway.core.filter.common;

import org.wyh.gateway.core.context.GatewayContext;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 15:38
 * @Description: 过滤器（组件）的顶级接口。
                 注意，方法中的args参数实际上存放的是过滤器组件的配置类实例
 */
public interface Filter{
    /**
     * @date: 2024-05-14 15:43
     * @description: 校验/检查是否应该执行该过滤器组件
     * @Param ctx:
     * @return: boolean
     */
    boolean check(GatewayContext ctx) throws Throwable;
    /**
     * @date: 2024-05-14 15:49
     * @description: 执行过滤器，对指定内容进行过滤处理。（核心过滤逻辑其实是调用doFilter方法完成的）
     * @Param ctx:
     * @Param args: args[0]实际上就是过滤器组件的配置类实例
     * @return: void
     */
    void filter(GatewayContext ctx, Object... args) throws Throwable;
    /**
     * @date: 2024-05-14 15:51
     * @description: 真正执行过滤处理逻辑的核心方法
     * @Param ctx:
     * @Param args: args[0]实际上就是过滤器组件的配置类实例
     * @return: void
     */
    void doFilter(GatewayContext ctx, Object... args) throws Throwable;
    /**
     * @date: 2024-05-14 15:53
     * @description: 根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
     * @Param ctx:
     * @Param args: args[0]实际上就是过滤器组件的配置类实例
     * @return: void
     */
    void fireNext(GatewayContext ctx, Object... args) throws Throwable;
    /**
     * @date: 2024-05-14 15:55
     * @description: 默认方法，用于过滤器初始化。父类方法只是占位，若子类有需求，可以重写覆盖。
     * @return: void
     */
    default void init() throws Throwable{}
    /**
     * @date: 2024-05-14 15:57
     * @description: 默认方法，用于过滤器销毁。父类方法只是占位，若子类有需求，可以重写覆盖。
     * @return: void
     */
    default void destroy() throws Throwable{}
}
