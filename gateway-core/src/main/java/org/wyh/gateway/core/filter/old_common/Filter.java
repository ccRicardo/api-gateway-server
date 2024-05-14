package org.wyh.gateway.core.filter.old_common;

import org.wyh.gateway.core.context.GatewayContext;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter
 * @Author: wyh
 * @Date: 2024-02-19 14:08
 * @Description: 过滤器接口
 */
public interface Filter {
    /**
     * @date: 2024-02-19 14:17
     * @description: 执行过滤操作
     * @Param ctx:
     * @return: void
     */
    void doFilter(GatewayContext ctx) throws Exception;
    /**
     * @date: 2024-02-19 14:17
     * @description: 默认方法，用于获取该过滤器的执行优先级
     * @return: int
     */
    default int getOrder(){
        //获取过滤器实现类对象上的过滤器注解对象
        FilterAspect annotation = this.getClass().getAnnotation(FilterAspect.class);
        if(annotation != null){
            return annotation.order();
        }
        //若未设置优先级，则默认返回int类型的最大值
        return Integer.MAX_VALUE;
    }
}
