package org.wyh.gateway.core.filter.common.chainfactory;

import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.filter.common.AbstractLinkedFilter;

import java.util.List;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-15 15:27
 * @Description: 过滤器链工厂的接口定义
 */
public interface FilterChainFactory {
    /**
     * @date: 2024-05-15 15:35
     * @description: 将指定类型的过滤器列表加入到相应的过滤器链中
     * @Param filterType: 过滤器的类型（过滤器列表中的类型必须一致）
     * @Param filters: 要加入过滤器链的过滤器列表集合
     * @return: void
     */
    void buildFilterChain(FilterType filterType, List<AbstractLinkedFilter<GatewayContext>> filters);
    /**
     * @date: 2024-05-15 15:43
     * @description: 执行正常情况下的过滤器链
     * @Param ctx:
     * @return: void
     */
    void doFilterChain(GatewayContext ctx);
    /**
     * @date: 2024-05-15 15:43
     * @description: 执行异常情况下的过滤器链
     * @Param ctx:
     * @return: void
     */
    void doErrorFilterChain(GatewayContext ctx);
    /**
     * @date: 2024-05-15 15:44
     * @description: 获取指定类型的过滤器
     * @Param filterClass: 过滤器类型的Class对象
     * @return: T
     */
    <T> T getFilter(Class<T> filterClass);
    /**
     * @date: 2024-05-15 15:45
     * @description: 获取指定id的过滤器
     * @Param filterId:
     * @return: T
     */
    <T> T getFilter(String filterId);
}
