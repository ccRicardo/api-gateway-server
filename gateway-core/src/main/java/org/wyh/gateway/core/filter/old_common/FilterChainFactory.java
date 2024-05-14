package org.wyh.gateway.core.filter.old_common;

import org.wyh.gateway.core.context.GatewayContext;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter
 * @Author: wyh
 * @Date: 2024-02-19 14:39
 * @Description: 过滤器链工厂的接口
 */
public interface FilterChainFactory {
    /**
     * @date: 2024-02-19 14:42
     * @description: 构建过滤器链
     * @Param ctx:
     * @return: org.wyh.core.filter.GatewayFilterChain
     */
    GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception;
    /**
     * @date: 2024-02-19 14:45
     * @description: 根据过滤器id获取过滤器对象
     * @Param filterId:
     * @return: T
     */
    Filter getFilter(String filterId) throws Exception;

}
