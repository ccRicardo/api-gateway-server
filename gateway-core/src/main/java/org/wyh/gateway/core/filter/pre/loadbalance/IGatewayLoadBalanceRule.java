package org.wyh.gateway.core.filter.pre.loadbalance;

import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.core.context.GatewayContext;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.loadbalance
 * @Author: wyh
 * @Date: 2024-02-21 9:50
 * @Description: 负载均衡策略（规则）的顶级接口
 */
public interface IGatewayLoadBalanceRule {
    /**
     * @date: 2024-02-21 9:51
     * @description: 根据网关上下文选择相应的服务实例
     * @Param ctx:
     * @return: org.wyh.common.config.ServiceInstance
     */
    ServiceInstance choose(GatewayContext ctx);
    /**
     * @date: 2024-02-21 9:52
     * @description: 根据服务id选择相应的服务实例
     * @Param uniqueId:
     * @Param gray: 标识请求的是否为灰度服务实例
     * @return: org.wyh.common.config.ServiceInstance
     */
    ServiceInstance choose(String uniqueId, boolean gray);
}
