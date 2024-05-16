package org.wyh.gateway.core.filter.common;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 16:02
 * @Description: 过滤器类型枚举类。过滤器类型可以分为前置pre，路由route，后置post和异常error。
                 各类型过滤器的位置/顺序，以及功能如下：
                 前置：位于路由过滤器之前，负责对网关上下文进行处理，是数量最多，最主要的过滤器类型。
                 路由：有且只有一个，负责路由转发和接收响应。
                 异常：当前置或路由过滤器中出现异常时才执行，位于后置过滤器之前，负责异常处理。
                 后置：位于路由过滤器之后，负责一些统计分析方面的工作（通常不涉及响应内容的修改）。

                 注意：
                 * 路由过滤器接收到响应后，会先直接写回响应信息，然后再执行后置过滤器。
                 * 后置过滤器中出现异常时，不会执行异常过滤器。
 */
public enum FilterType {
    PRE("pre", "前置过滤器"),
    ROUTE("route", "路由过滤器"),
    ERROR("error", "异常过滤器"),
    POST("post", "后置过滤器");
    //过滤器类型描述代码
    private final String code;
    //过滤器类型描述信息
    private final String message;
    /**
     * @date: 2024-05-14 16:19
     * @description: 枚举类的构造器
     * @Param code:
     * @Param message:
     * @return: null
     */
    FilterType(String code, String message){
        this.code = code;
        this.message = message;
    }
    /**
     * @date: 2024-05-14 16:20
     * @description: 获取过滤器类型的描述代码
     * @return: java.lang.String
     */
    public String getCode() {
        return code;
    }
    /**
     * @date: 2024-05-14 16:20
     * @description: 获取过滤器类型的描述信息
     * @return: java.lang.String
     */
    public String getMessage() {
        return message;
    }
}
